package com.solar.Quotation.service.serviceImpl;

import com.solar.Quotation.Enum.AuditAction;
import com.solar.Quotation.dto.requestdto.CalculationFilterRequestDto;
import com.solar.Quotation.dto.requestdto.CalculationRequestDto;
import com.solar.Quotation.dto.responsedto.CalculationResponseDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.entity.ChannelPartner;
import com.solar.Quotation.entity.CostCalculation;
import com.solar.Quotation.exception.ApiException;
import com.solar.Quotation.repository.ChannelPartnerRepository;
import com.solar.Quotation.repository.CostCalculationRepository;
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
import java.util.Map;

@Service
public class CalculatorServiceImpl implements CalculatorService {

    private final CostCalculationRepository calcRepo;
    private final ChannelPartnerRepository partnerRepo;
    private final TenantResolver tenantResolver;
    private final AuditService auditService;
    private final CodeGenerator codeGenerator;

    public CalculatorServiceImpl(CostCalculationRepository calcRepo,
                                 ChannelPartnerRepository partnerRepo,
                                 TenantResolver tenantResolver,
                                 AuditService auditService,
                                 CodeGenerator codeGenerator) {
        this.calcRepo = calcRepo;
        this.partnerRepo = partnerRepo;
        this.tenantResolver = tenantResolver;
        this.auditService = auditService;
        this.codeGenerator = codeGenerator;
    }

    @Override
    public CalculationResponseDto preview(CalculationRequestDto req) {
        Long partnerId = tenantResolver.effectivePartnerId(req.getPartnerId());
        if (partnerId == null) throw new ApiException(403, "partnerId required");
        return buildPreview(req, partnerId);
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
    @Transactional
    public CalculationResponseDto update(Long id, CalculationRequestDto req) {
        Long partnerId = tenantResolver.effectivePartnerId(req.getPartnerId());
        if (partnerId == null) throw new ApiException(403, "partnerId required");

        CostCalculation e = calcRepo.findOne(id, partnerId, tenantResolver.ownerFilter())
                .orElseThrow(() -> new ApiException(404, "Calculation not found"));

        Map<String, Object> before = snapshot(e);
        apply(e, req);
        CostCalculation saved = calcRepo.save(e);

        auditService.record(AuditAction.UPDATE, "CALCULATOR", "CostCalculation",
                saved.getId(), saved.getCode(), saved.getChannelPartnerId(),
                DiffUtil.diff(before, snapshot(saved)));
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
        d.setPartner(partnerRef(e.getChannelPartnerId()));

        CalculationResponseDto.Costs c = new CalculationResponseDto.Costs();
        c.setMaterial(e.getMaterialCost());
        c.setInstallation(e.getInstallationCost());
        c.setTransportation(e.getTransportationCost());
        c.setCivil(e.getCivilCost());
        c.setLabour(e.getLabourCost());
        c.setElectrical(e.getElectricalCost());
        c.setOther(e.getOtherCost());
        c.setDiscount(e.getDiscount());
        d.setCosts(c);

        CalculationResponseDto.Result r = new CalculationResponseDto.Result();
        r.setSubtotal(e.getSubtotal());
        r.setGstPercent(e.getGstPercent());
        r.setGstAmount(e.getGstAmount());
        r.setGrandTotal(e.getGrandTotal());
        r.setCostPerKw(e.getCostPerKw());
        r.setEstimatedSaving(e.getEstimatedSaving());
        d.setResult(r);
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
        d.setPartner(partnerRef(partnerId));

        CalculationResponseDto.Costs c = new CalculationResponseDto.Costs();
        c.setMaterial(nz(r.getMaterialCost()));
        c.setInstallation(nz(r.getInstallationCost()));
        c.setTransportation(nz(r.getTransportationCost()));
        c.setCivil(nz(r.getCivilCost()));
        c.setLabour(nz(r.getLabourCost()));
        c.setElectrical(nz(r.getElectricalCost()));
        c.setOther(nz(r.getOtherCost()));
        c.setDiscount(nz(r.getDiscount()));
        d.setCosts(c);

        CalculationResponseDto.Result res = new CalculationResponseDto.Result();
        res.setSubtotal(subtotal);
        res.setGstPercent(nz(r.getGstPercent()));
        res.setGstAmount(gst);
        res.setGrandTotal(total);
        res.setCostPerKw(perKw);
        res.setEstimatedSaving(BigDecimal.ZERO);
        d.setResult(res);
        return d;
    }

    private CalculationResponseDto.PartnerRef partnerRef(Long partnerId) {
        if (partnerId == null) return null;
        ChannelPartner cp = partnerRepo.findById(partnerId).orElse(null);
        if (cp == null) return null;
        CalculationResponseDto.PartnerRef pr = new CalculationResponseDto.PartnerRef();
        pr.setId(cp.getId());
        pr.setName(cp.getName());
        return pr;
    }

    private Instant toInstant(Object createdAt) {
        if (createdAt instanceof Instant i) return i;
        if (createdAt instanceof java.time.LocalDateTime ldt) return ldt.atZone(java.time.ZoneId.of("Asia/Kolkata")).toInstant();
        if (createdAt instanceof java.util.Date d) return d.toInstant();
        return Instant.now();
    }
}