package com.solar.Quotation.service.serviceImpl;

import com.solar.Quotation.Enum.AuditAction;
import com.solar.Quotation.dto.requestdto.CalculationFilterRequestDto;
import com.solar.Quotation.dto.requestdto.CalculationRequestDto;
import com.solar.Quotation.dto.responsedto.CalculationResponseDto;
import com.solar.Quotation.dto.responsedto.CustomerResponseDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.ProjectResponseDto;
import com.solar.Quotation.entity.ChannelPartner;
import com.solar.Quotation.entity.CostCalculation;
import com.solar.Quotation.exception.ApiException;
import com.solar.Quotation.repository.ChannelPartnerRepository;
import com.solar.Quotation.repository.CostCalculationRepository;
import com.solar.Quotation.repository.CustomerRepository;
import com.solar.Quotation.repository.ProjectRepository;
import com.solar.Quotation.security.TenantResolver;
import com.solar.Quotation.service.AuditService;
import com.solar.Quotation.service.CalculatorService;
import com.solar.Quotation.util.CodeGenerator;
import com.solar.Quotation.util.DiffUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CalculatorServiceImpl implements CalculatorService {

    private final CostCalculationRepository calcRepo;
    private final ChannelPartnerRepository partnerRepo;
    private final TenantResolver tenantResolver;
    private final AuditService auditService;
    private final CodeGenerator codeGenerator;
    private final CustomerRepository customerRepo;
    private final ProjectRepository projectRepo;

    public CalculatorServiceImpl(CostCalculationRepository calcRepo,
                                 ChannelPartnerRepository partnerRepo,
                                 TenantResolver tenantResolver,
                                 AuditService auditService,
                                 CustomerRepository customerRepo,  // <-- YEH ADD KARO
                                 ProjectRepository projectRepo,
                                 CodeGenerator codeGenerator) {


        this.calcRepo = calcRepo;
        this.partnerRepo = partnerRepo;
        this.tenantResolver = tenantResolver;
        this.auditService = auditService;
        this.codeGenerator = codeGenerator;
        this.customerRepo = customerRepo; // <-- YEH ADD KARO
        this.projectRepo = projectRepo;
    }

    @Override
    public List<CustomerResponseDto> getAllCustomers() {
        // Note: CustomerRepository ke method ka naam aapke project ke hisaab se alag ho sakta hai (jaise findByDeletedFalse())
        return customerRepo.findAll().stream()
                .filter(c -> !c.isDeleted()) // soft delete handle karne ke liye
                .map(c -> {
                    CustomerResponseDto dto = new CustomerResponseDto();
                    dto.setId(c.getId());
                    dto.setName(c.getName()); // Aapke entity me jo field ho (e.g., customerName)
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponseDto> getAllProjects() {
        return projectRepo.findAll().stream()
                .filter(p -> !p.isDeleted())
                .map(p -> {
                    ProjectResponseDto dto = new ProjectResponseDto();
                    dto.setId(p.getId());
                    dto.setName(p.getName()); // Aapke entity me jo field ho (e.g., projectName)
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CalculationResponseDto preview(CalculationRequestDto req) {
        // Preview is stateless; partner is only needed for persistence.
        Long partnerId = tenantResolver.effectivePartnerId(req.getPartnerId());
        return buildPreview(req, partnerId);   // partnerId may be null here
    }

    @Override
    @Transactional
    public CalculationResponseDto update(Long id, CalculationRequestDto req) {
        // 1. Pehle current user ka partner nikaalo
        Long partnerId = tenantResolver.effectivePartnerId(req.getPartnerId());

        // 2. Agar req me partnerId nahi hai (null hai), toh purane record se partnerId nikaal lo
        if (partnerId == null) {
            CostCalculation existing = calcRepo.findById(id)
                    .orElseThrow(() -> new ApiException(404, "Calculation not found"));
            partnerId = existing.getChannelPartnerId();
        }

        if (partnerId == null) throw new ApiException(403, "partnerId required");

        CostCalculation e = calcRepo.findOne(id, partnerId, tenantResolver.ownerFilter())
                .orElseThrow(() -> new ApiException(404, "Calculation not found"));

        Map<String, Object> before = snapshot(e);

        // Enum casing fix (agar frontend se galat case aa raha ho)
        if (req.getProjectType() != null) {
            req.setProjectType(req.getProjectType());
        }

        apply(e, req);
        CostCalculation saved = calcRepo.save(e);

        auditService.record(AuditAction.UPDATE, "CALCULATOR", "CostCalculation",
                saved.getId(), saved.getCode(), saved.getChannelPartnerId(),
                DiffUtil.diff(before, snapshot(saved)));
        return toDto(saved);
    }

    @Override
    @Transactional
    public CalculationResponseDto create(CalculationRequestDto req) {
        Long partnerId = tenantResolver.effectivePartnerId(req.getPartnerId());
        if (partnerId == null) throw new ApiException(403, "partnerId required");

        CostCalculation e = new CostCalculation();
        e.setChannelPartnerId(partnerId);
        e.setCode(codeGenerator.next("CALC", partnerId));
        apply(e, req);
        CostCalculation saved = calcRepo.save(e);

        auditService.record(AuditAction.CREATE, "CALCULATOR", "CostCalculation",
                saved.getId(), saved.getCode(), saved.getChannelPartnerId(),
                DiffUtil.diff(null, snapshot(saved)));
        return toDto(saved);
    }

    @Override
    public CalculationResponseDto getById(Long id) {
        Long partnerId = tenantResolver.effectivePartnerId(null);
        CostCalculation e = calcRepo.findOne(id, partnerId, tenantResolver.ownerFilter())
                .orElseThrow(() -> new ApiException(404, "Calculation not found"));
        return toDto(e);
    }

    @Override
    public PageResponse<CalculationResponseDto> list(CalculationFilterRequestDto f, Pageable pageable) {
        Long partnerId = tenantResolver.effectivePartnerId(f.getPartnerId());
        Page<CostCalculation> page = calcRepo.search(
                partnerId, tenantResolver.ownerFilter(),
                f.getProjectType(), f.getSearch(), pageable);
        return PageResponse.of(page.map(this::toDto));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long partnerId = tenantResolver.effectivePartnerId(null);
        CostCalculation e = calcRepo.findOne(id, partnerId, tenantResolver.ownerFilter())
                .orElseThrow(() -> new ApiException(404, "Calculation not found"));
        e.setDeleted(true);
        calcRepo.save(e);
        auditService.record(AuditAction.DELETE, "CALCULATOR", "CostCalculation",
                e.getId(), e.getCode(), e.getChannelPartnerId(), null);
    }

    // ---- helpers ----

    private void apply(CostCalculation e, CalculationRequestDto r) {
        e.setCustomerName(r.getCustomerName());
        e.setProjectName(r.getProjectName());
        e.setCapacityKw(nz(r.getCapacityKw()));
        e.setProjectType(r.getProjectType());
        e.setMaterialCost(nz(r.getMaterialCost()));
        e.setInstallationCost(nz(r.getInstallationCost()));
        e.setTransportationCost(nz(r.getTransportationCost()));
        e.setCivilCost(nz(r.getCivilCost()));
        e.setLabourCost(nz(r.getLabourCost()));
        e.setElectricalCost(nz(r.getElectricalCost()));
        e.setOtherCost(nz(r.getOtherCost()));
        e.setDiscount(nz(r.getDiscount()));
        e.setGstPercent(nz(r.getGstPercent()));
        recalc(e);
    }

    private void recalc(CostCalculation e) {
        BigDecimal subtotal = e.getMaterialCost()
                .add(e.getInstallationCost())
                .add(e.getTransportationCost())
                .add(e.getCivilCost())
                .add(e.getLabourCost())
                .add(e.getElectricalCost())
                .add(e.getOtherCost())
                .subtract(e.getDiscount());
        BigDecimal gst = subtotal.multiply(e.getGstPercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(gst);
        BigDecimal perKw = (e.getCapacityKw().compareTo(BigDecimal.ZERO) > 0)
                ? total.divide(e.getCapacityKw(), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        e.setSubtotal(subtotal);
        e.setGstAmount(gst);
        e.setGrandTotal(total);
        e.setCostPerKw(perKw);
        e.setEstimatedSaving(BigDecimal.ZERO);
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private Map<String, Object> snapshot(CostCalculation e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", e.getCode());
        m.put("customerName", e.getCustomerName());
        m.put("projectName", e.getProjectName());
        m.put("capacityKw", plain(e.getCapacityKw()));
        m.put("projectType", e.getProjectType() == null ? null : e.getProjectType().name());
        m.put("materialCost", plain(e.getMaterialCost()));
        m.put("installationCost", plain(e.getInstallationCost()));
        m.put("transportationCost", plain(e.getTransportationCost()));
        m.put("civilCost", plain(e.getCivilCost()));
        m.put("labourCost", plain(e.getLabourCost()));
        m.put("electricalCost", plain(e.getElectricalCost()));
        m.put("otherCost", plain(e.getOtherCost()));
        m.put("discount", plain(e.getDiscount()));
        m.put("gstPercent", plain(e.getGstPercent()));
        m.put("subtotal", plain(e.getSubtotal()));
        m.put("gstAmount", plain(e.getGstAmount()));
        m.put("grandTotal", plain(e.getGrandTotal()));
        m.put("costPerKw", plain(e.getCostPerKw()));
        return m;
    }

    private String plain(BigDecimal v) { return v == null ? null : v.stripTrailingZeros().toPlainString(); }

    private CalculationResponseDto toDto(CostCalculation e) {
        CalculationResponseDto d = new CalculationResponseDto();
        d.setId(e.getId());
        d.setCode(e.getCode());
        d.setCustomerName(e.getCustomerName());
        d.setProjectName(e.getProjectName());
        d.setCapacityKw(e.getCapacityKw());
        d.setProjectType(e.getProjectType());
        d.setCreatedAt(e.getCreatedAt() == null ? Instant.now() : toInstant(e.getCreatedAt()));

        d.setPartnerId(e.getChannelPartnerId());
        d.setPartnerName(partnerName(e.getChannelPartnerId()));

        d.setMaterialCost(nz(e.getMaterialCost()));
        d.setInstallationCost(nz(e.getInstallationCost()));
        d.setTransportationCost(nz(e.getTransportationCost()));
        d.setCivilCost(nz(e.getCivilCost()));
        d.setLabourCost(nz(e.getLabourCost()));
        d.setElectricalCost(nz(e.getElectricalCost()));
        d.setOtherCost(nz(e.getOtherCost()));
        d.setDiscount(nz(e.getDiscount()));

        d.setSubtotal(nz(e.getSubtotal()));
        d.setGstPercent(nz(e.getGstPercent()));
        d.setGstAmount(nz(e.getGstAmount()));
        d.setFinalCost(nz(e.getGrandTotal()));      // <-- map grandTotal -> finalCost
        d.setCostPerKw(nz(e.getCostPerKw()));
        d.setEstimatedSaving(nz(e.getEstimatedSaving()));
        return d;
    }

    private CalculationResponseDto buildPreview(CalculationRequestDto r, Long partnerId) {
        BigDecimal subtotal = nz(r.getMaterialCost()).add(nz(r.getInstallationCost()))
                .add(nz(r.getTransportationCost())).add(nz(r.getCivilCost()))
                .add(nz(r.getLabourCost())).add(nz(r.getElectricalCost()))
                .add(nz(r.getOtherCost())).subtract(nz(r.getDiscount()));
        BigDecimal gst = subtotal.multiply(nz(r.getGstPercent()))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(gst);
        BigDecimal perKw = (r.getCapacityKw() != null && r.getCapacityKw().compareTo(BigDecimal.ZERO) > 0)
                ? total.divide(r.getCapacityKw(), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        CalculationResponseDto d = new CalculationResponseDto();
        d.setCustomerName(r.getCustomerName());
        d.setProjectName(r.getProjectName());
        d.setCapacityKw(r.getCapacityKw());
        d.setProjectType(r.getProjectType());
        d.setCreatedAt(Instant.now());
        d.setPartnerId(partnerId);
        d.setPartnerName(partnerName(partnerId));

        d.setMaterialCost(nz(r.getMaterialCost()));
        d.setInstallationCost(nz(r.getInstallationCost()));
        d.setTransportationCost(nz(r.getTransportationCost()));
        d.setCivilCost(nz(r.getCivilCost()));
        d.setLabourCost(nz(r.getLabourCost()));
        d.setElectricalCost(nz(r.getElectricalCost()));
        d.setOtherCost(nz(r.getOtherCost()));
        d.setDiscount(nz(r.getDiscount()));

        d.setSubtotal(subtotal);
        d.setGstPercent(nz(r.getGstPercent()));
        d.setGstAmount(gst);
        d.setFinalCost(total);                       // <-- finalCost
        d.setCostPerKw(perKw);
        d.setEstimatedSaving(BigDecimal.ZERO);
        return d;
    }

    private String partnerName(Long partnerId) {
        if (partnerId == null) return null;
        return partnerRepo.findById(partnerId).map(ChannelPartner::getName).orElse(null);
    }



    private Instant toInstant(Object createdAt) {
        if (createdAt instanceof Instant i) return i;
        if (createdAt instanceof java.time.LocalDateTime ldt) return ldt.atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant();
        if (createdAt instanceof java.util.Date d) return d.toInstant();
        return Instant.now();
    }
}
