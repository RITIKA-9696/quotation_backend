package com.solar.Quotation.service.serviceImpl;

import com.solar.Quotation.Enum.AuditAction;
import com.solar.Quotation.Enum.CustomerType;
import com.solar.Quotation.Enum.ProjectStatus;
import com.solar.Quotation.dto.requestdto.ProjectFilterRequestDto;
import com.solar.Quotation.dto.requestdto.ProjectRequestDto;
import com.solar.Quotation.dto.requestdto.ProjectStatusRequestDto;
import com.solar.Quotation.dto.responsedto.CustomerRefResponseDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.PartnerRefResponseDto;
import com.solar.Quotation.dto.responsedto.ProjectResponseDto;
import com.solar.Quotation.dto.responsedto.ProjectSummaryResponseDto;
import com.solar.Quotation.entity.Customer;
import com.solar.Quotation.entity.Project;
import com.solar.Quotation.exception.BusinessException;
import com.solar.Quotation.exception.ResourceNotFoundException;
import com.solar.Quotation.repository.CustomerRepository;
import com.solar.Quotation.repository.ProjectRepository;
import com.solar.Quotation.security.TenantResolver;
import com.solar.Quotation.service.AuditService;
import com.solar.Quotation.service.PartnerRefProvider;
import com.solar.Quotation.service.ProjectService;
import com.solar.Quotation.util.AuditInfoMapper;
import com.solar.Quotation.util.CodeGenerator;
import com.solar.Quotation.util.DiffUtil;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ProjectServiceImpl implements ProjectService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name", "code", "capacityKw", "status");
    /**
     * Used only when the form leaves estimatedGeneration empty: capacity x units per kW per day x 30 days.
     * TODO: read the 4 from the SAVINGS_CONFIG setting once the settings module exists.
     */
    private static final BigDecimal DEFAULT_UNITS_PER_KW_PER_DAY = new BigDecimal("4");
    private static final BigDecimal DAYS_PER_MONTH = new BigDecimal("30");

    private final ProjectRepository projectRepository;
    private final CustomerRepository customerRepository;
    private final TenantResolver tenantResolver;
    private final PartnerRefProvider partnerRefProvider;
    private final AuditInfoMapper auditInfoMapper;
    private final AuditService auditService;
    private final CodeGenerator codeGenerator;

    public ProjectServiceImpl(ProjectRepository projectRepository, CustomerRepository customerRepository,
                              TenantResolver tenantResolver, PartnerRefProvider partnerRefProvider,
                              AuditInfoMapper auditInfoMapper, AuditService auditService, CodeGenerator codeGenerator) {
        this.projectRepository = projectRepository;
        this.customerRepository = customerRepository;
        this.tenantResolver = tenantResolver;
        this.partnerRefProvider = partnerRefProvider;
        this.auditInfoMapper = auditInfoMapper;
        this.auditService = auditService;
        this.codeGenerator = codeGenerator;
    }

    // ---------------------------------------------------------------- read

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponseDto> list(ProjectFilterRequestDto f) {
        Long partnerId = tenantResolver.effectivePartnerId(f.getPartnerId());
        Long ownerId = tenantResolver.ownerFilter();
        Page<Project> page = projectRepository.search(partnerId, ownerId, parseType(f.getType()),
                parseStatus(f.getStatus()), f.getCustomerId(), f.getSearchOrNull(),
                f.toPageable("createdAt,desc", SORT_FIELDS));
        Lookup lookup = lookup(page.getContent());
        return PageResponse.of(page.map(p -> toResponse(p, lookup)));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectSummaryResponseDto summary(Long requestedPartnerId) {
        Long partnerId = tenantResolver.effectivePartnerId(requestedPartnerId);
        Long ownerId = tenantResolver.ownerFilter();
        ProjectSummaryResponseDto s = new ProjectSummaryResponseDto();
        s.setTotal(projectRepository.countFiltered(partnerId, ownerId, null, null));
        s.setResidential(projectRepository.countFiltered(partnerId, ownerId, CustomerType.RESIDENTIAL, null));
        s.setCommercial(projectRepository.countFiltered(partnerId, ownerId, CustomerType.COMMERCIAL, null));
        s.setIndustrial(projectRepository.countFiltered(partnerId, ownerId, CustomerType.INDUSTRIAL, null));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ProjectStatus st : ProjectStatus.values()) {
            byStatus.put(st.name(), projectRepository.countFiltered(partnerId, ownerId, null, st));
        }
        s.setByStatus(byStatus);
        return s;
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponseDto get(Long id) {
        return toSingleResponse(findProject(id));
    }

    // ---------------------------------------------------------------- write

    @Override
    @Transactional
    public ProjectResponseDto create(ProjectRequestDto req) {
        // The customer decides the partner: a partner user can only pick a customer of their own partner,
        // Super Admin can pick any customer and the project then belongs to that customer's partner.
        Customer customer = customerRepository.findOne(req.getCustomerId(),
                        tenantResolver.effectivePartnerId(null), tenantResolver.ownerFilter())
                .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Customer not found"));
        Long partnerId = customer.getChannelPartnerId();

        Project p = new Project();
        p.setChannelPartnerId(partnerId);
        p.setCode(codeGenerator.next("PRJ", partnerId));
        p.setCustomerId(customer.getId());
        applyFields(p, req);
        Project saved = projectRepository.save(p);

        auditService.record(AuditAction.CREATE, "PROJECT", "Project", saved.getId(), saved.getCode(),
                partnerId, DiffUtil.diff(null, snapshot(saved)));
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public ProjectResponseDto update(Long id, ProjectRequestDto req) {
        Project p = findProject(id);
        if (!p.getCustomerId().equals(req.getCustomerId())) {
            // the new customer must belong to the SAME partner as the project
            Customer customer = customerRepository.findOne(req.getCustomerId(), p.getChannelPartnerId(),
                            tenantResolver.ownerFilter())
                    .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Customer not found"));
            p.setCustomerId(customer.getId());
        }
        Map<String, Object> before = snapshot(p);
        applyFields(p, req);
        Project saved = projectRepository.save(p);

        auditService.record(AuditAction.UPDATE, "PROJECT", "Project", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), DiffUtil.diff(before, snapshot(saved)));
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public ProjectResponseDto updateStatus(Long id, ProjectStatusRequestDto req) {
        Project p = findProject(id);
        Map<String, Object> before = snapshot(p);
        p.setStatus(ProjectStatus.valueOf(req.getStatus()));
        Project saved = projectRepository.save(p);

        auditService.record(AuditAction.STATUS_CHANGE, "PROJECT", "Project", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), DiffUtil.diff(before, snapshot(saved)));
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Project p = findProject(id);
        String code = p.getCode();
        Long partnerId = p.getChannelPartnerId();
        p.setDeleted(true);
        projectRepository.save(p);
        auditService.record(AuditAction.DELETE, "PROJECT", "Project", id, code, partnerId, null);
    }

    // ---------------------------------------------------------------- helpers

    private Project findProject(Long id) {
        return projectRepository.findOne(id, tenantResolver.effectivePartnerId(null), tenantResolver.ownerFilter())
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
    }

    private void applyFields(Project p, ProjectRequestDto req) {
        p.setName(req.getName().trim());
        p.setType(CustomerType.valueOf(req.getType()));
        p.setCapacityKw(req.getCapacityKw().setScale(2, RoundingMode.HALF_UP));
        p.setRoofType(blankToNull(req.getRoofType()));
        p.setMonthlyConsumption(req.getMonthlyConsumption());
        p.setLocation(blankToNull(req.getLocation()));
        p.setRoofArea(req.getRoofArea());
        p.setEstimatedGeneration(req.getEstimatedGeneration() != null
                ? req.getEstimatedGeneration()
                : p.getCapacityKw().multiply(DEFAULT_UNITS_PER_KW_PER_DAY).multiply(DAYS_PER_MONTH)
                .setScale(2, RoundingMode.HALF_UP));
        p.setInstallationType(blankToNull(req.getInstallationType()));
        ProjectStatus status = parseStatus(req.getStatus());
        p.setStatus(status != null ? status : (p.getStatus() != null ? p.getStatus() : ProjectStatus.PLANNING));
        p.setRemarks(blankToNull(req.getRemarks()));
    }

    private CustomerType parseType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CustomerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("VALIDATION_ERROR", "Unknown project type " + value);
        }
    }

    private ProjectStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ProjectStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("VALIDATION_ERROR", "Unknown project status " + value);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private Map<String, Object> snapshot(Project p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", p.getName());
        m.put("customerId", p.getCustomerId() == null ? null : p.getCustomerId().toString());
        m.put("type", p.getType().name());
        m.put("capacityKw", p.getCapacityKw() == null ? null : p.getCapacityKw().toPlainString());
        m.put("roofType", p.getRoofType());
        m.put("monthlyConsumption", plain(p.getMonthlyConsumption()));
        m.put("location", p.getLocation());
        m.put("roofArea", plain(p.getRoofArea()));
        m.put("estimatedGeneration", plain(p.getEstimatedGeneration()));
        m.put("installationType", p.getInstallationType());
        m.put("status", p.getStatus().name());
        m.put("remarks", p.getRemarks());
        return m;
    }

    private String plain(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    private static final class Lookup {
        private Map<Long, PartnerRefResponseDto> partners = new HashMap<>();
        private Map<Long, String> names = new HashMap<>();
        private final Map<Long, CustomerRefResponseDto> customers = new HashMap<>();
    }

    private Lookup lookup(List<Project> projects) {
        Lookup lk = new Lookup();
        if (projects.isEmpty()) {
            return lk;
        }
        Set<Long> customerIds = new HashSet<>();
        Set<Long> partnerIds = new HashSet<>();
        for (Project p : projects) {
            customerIds.add(p.getCustomerId());
            partnerIds.add(p.getChannelPartnerId());
        }
        lk.partners = partnerRefProvider.refs(partnerIds);
        lk.names = auditInfoMapper.namesFor(projects);
        for (Customer c : customerRepository.findAllById(customerIds)) {
            lk.customers.put(c.getId(), new CustomerRefResponseDto(c.getId(), c.getCode(), c.getName()));
        }
        return lk;
    }

    private ProjectResponseDto toSingleResponse(Project p) {
        return toResponse(p, lookup(List.of(p)));
    }

    private ProjectResponseDto toResponse(Project p, Lookup lk) {
        ProjectResponseDto d = new ProjectResponseDto();
        d.setId(p.getId());
        d.setCode(p.getCode());
        d.setName(p.getName());
        d.setCustomer(lk.customers.get(p.getCustomerId()));
        d.setType(p.getType().name());
        d.setCapacityKw(p.getCapacityKw());
        d.setRoofType(p.getRoofType());
        d.setMonthlyConsumption(p.getMonthlyConsumption());
        d.setLocation(p.getLocation());
        d.setRoofArea(p.getRoofArea());
        d.setEstimatedGeneration(p.getEstimatedGeneration());
        d.setInstallationType(p.getInstallationType());
        d.setStatus(p.getStatus().name());
        d.setRemarks(p.getRemarks());
        d.setPartner(lk.partners.get(p.getChannelPartnerId()));
        d.setAudit(auditInfoMapper.of(p, lk.names));
        return d;
    }
}
