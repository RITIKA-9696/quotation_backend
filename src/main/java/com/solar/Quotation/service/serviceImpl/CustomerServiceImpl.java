package com.solar.Quotation.service.serviceImpl;

import com.solar.Quotation.Enum.AuditAction;
import com.solar.Quotation.Enum.CustomerType;
import com.solar.Quotation.Enum.FollowupStatus;
import com.solar.Quotation.Enum.RecordStatus;
import com.solar.Quotation.dto.requestdto.CustomerFilterRequestDto;
import com.solar.Quotation.dto.requestdto.CustomerRequestDto;
import com.solar.Quotation.dto.requestdto.CustomerStatusRequestDto;
import com.solar.Quotation.dto.requestdto.FollowupFilterRequestDto;
import com.solar.Quotation.dto.requestdto.FollowupRequestDto;
import com.solar.Quotation.dto.responsedto.AuditLogResponseDto;
import com.solar.Quotation.dto.responsedto.CustomerResponseDto;
import com.solar.Quotation.dto.responsedto.CustomerSummaryResponseDto;
import com.solar.Quotation.dto.responsedto.FollowupResponseDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.PartnerRefResponseDto;
import com.solar.Quotation.entity.Customer;
import com.solar.Quotation.entity.CustomerFollowup;
import com.solar.Quotation.entity.Project;
import com.solar.Quotation.exception.BusinessException;
import com.solar.Quotation.exception.ResourceNotFoundException;
import com.solar.Quotation.repository.CustomerFollowupRepository;
import com.solar.Quotation.repository.CustomerRepository;
import com.solar.Quotation.repository.ProjectRepository;
import com.solar.Quotation.security.TenantResolver;
import com.solar.Quotation.service.AuditService;
import com.solar.Quotation.service.CustomerService;
import com.solar.Quotation.service.PartnerRefProvider;

import com.solar.Quotation.util.AuditInfoMapper;
import com.solar.Quotation.util.CodeGenerator;
import com.solar.Quotation.util.DiffUtil;
import com.solar.Quotation.util.PartnerGuard;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name", "code", "city", "status");
    private static final Set<String> FOLLOWUP_SORT_FIELDS = Set.of("followupDate", "nextDate", "createdAt");
    /** "Today" for overdue follow-ups is the business day in India, not the server's zone. */
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final CustomerRepository customerRepository;
    private final CustomerFollowupRepository followupRepository;
    private final ProjectRepository projectRepository;
    private final TenantResolver tenantResolver;
    private final PartnerRefProvider partnerRefProvider;
    private final AuditInfoMapper auditInfoMapper;
    private final AuditService auditService;
    private final CodeGenerator codeGenerator;
    private final PartnerGuard partnerGuard;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerFollowupRepository followupRepository,
                               ProjectRepository projectRepository, TenantResolver tenantResolver,
                               PartnerRefProvider partnerRefProvider, AuditInfoMapper auditInfoMapper,
                               AuditService auditService, CodeGenerator codeGenerator, PartnerGuard partnerGuard) {
        this.customerRepository = customerRepository;
        this.followupRepository = followupRepository;
        this.projectRepository = projectRepository;
        this.tenantResolver = tenantResolver;
        this.partnerRefProvider = partnerRefProvider;
        this.auditInfoMapper = auditInfoMapper;
        this.auditService = auditService;
        this.codeGenerator = codeGenerator;
        this.partnerGuard = partnerGuard;
    }

    // ---------------------------------------------------------------- customers: read

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponseDto> list(CustomerFilterRequestDto f) {
        Long partnerId = tenantResolver.effectivePartnerId(f.getPartnerId());
        Long ownerId = tenantResolver.ownerFilter();
        Page<Customer> page = customerRepository.search(partnerId, ownerId, parseType(f.getType()),
                parseStatus(f.getStatus()), f.getSearchOrNull(), f.toPageable("createdAt,desc", SORT_FIELDS));
        Lookup lookup = lookup(page.getContent());
        return PageResponse.of(page.map(c -> toResponse(c, lookup)));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerSummaryResponseDto summary(Long requestedPartnerId) {
        Long partnerId = tenantResolver.effectivePartnerId(requestedPartnerId);
        Long ownerId = tenantResolver.ownerFilter();
        CustomerSummaryResponseDto s = new CustomerSummaryResponseDto();
        s.setTotal(customerRepository.countFiltered(partnerId, ownerId, null, null));
        s.setActive(customerRepository.countFiltered(partnerId, ownerId, null, RecordStatus.ACTIVE));
        s.setInactive(customerRepository.countFiltered(partnerId, ownerId, null, RecordStatus.INACTIVE));
        s.setResidential(customerRepository.countFiltered(partnerId, ownerId, CustomerType.RESIDENTIAL, null));
        s.setCommercial(customerRepository.countFiltered(partnerId, ownerId, CustomerType.COMMERCIAL, null));
        s.setIndustrial(customerRepository.countFiltered(partnerId, ownerId, CustomerType.INDUSTRIAL, null));
        return s;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponseDto get(Long id) {
        return toSingleResponse(findCustomer(id));
    }

    // ---------------------------------------------------------------- customers: write

    @Override
    @Transactional
    public CustomerResponseDto create(CustomerRequestDto req) {
        Long partnerId = tenantResolver.requirePartnerIdForCreate(req.getPartnerId());
        partnerGuard.assertUsable(partnerId);

        String mobile = req.getMobile().trim();
        if (customerRepository.existsByChannelPartnerIdAndMobileAndDeletedFalse(partnerId, mobile)) {
            throw duplicateMobile();
        }

        Customer c = new Customer();
        c.setChannelPartnerId(partnerId);
        c.setCode(codeGenerator.next("CUST", partnerId));
        c.setMobile(mobile);
        applyFields(c, req);
        Customer saved = customerRepository.save(c);

        auditService.record(AuditAction.CREATE, "CUSTOMER", "Customer", saved.getId(), saved.getCode(),
                partnerId, DiffUtil.diff(null, snapshot(saved)));
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public CustomerResponseDto update(Long id, CustomerRequestDto req) {
        Customer c = findCustomer(id);
        String mobile = req.getMobile().trim();
        if (!mobile.equals(c.getMobile())
                && customerRepository.existsByChannelPartnerIdAndMobileAndDeletedFalseAndIdNot(
                c.getChannelPartnerId(), mobile, c.getId())) {
            throw duplicateMobile();
        }

        Map<String, Object> before = snapshot(c);
        c.setMobile(mobile);
        applyFields(c, req);
        Customer saved = customerRepository.save(c);

        auditService.record(AuditAction.UPDATE, "CUSTOMER", "Customer", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), DiffUtil.diff(before, snapshot(saved)));
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public CustomerResponseDto updateStatus(Long id, CustomerStatusRequestDto req) {
        Customer c = findCustomer(id);
        Map<String, Object> before = snapshot(c);
        c.setStatus(RecordStatus.valueOf(req.getStatus()));
        Customer saved = customerRepository.save(c);

        auditService.record(AuditAction.STATUS_CHANGE, "CUSTOMER", "Customer", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), DiffUtil.diff(before, snapshot(saved)));
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Customer c = findCustomer(id);
        // Add the same check for quotations when the quotation module exists.
        if (projectRepository.countByCustomerIdAndDeletedFalse(c.getId()) > 0) {
            throw new BusinessException("BUSINESS_RULE", "This customer has projects. Delete or move them first");
        }
        String code = c.getCode();
        Long partnerId = c.getChannelPartnerId();
        c.setMobile(c.getMobile() + "~del" + c.getId());      // frees the mobile number for reuse
        c.setStatus(RecordStatus.INACTIVE);
        c.setDeleted(true);
        customerRepository.save(c);

        auditService.record(AuditAction.DELETE, "CUSTOMER", "Customer", id, code, partnerId, null);
    }

    // ---------------------------------------------------------------- follow-ups

    @Override
    @Transactional(readOnly = true)
    public List<FollowupResponseDto> followups(Long customerId) {
        Customer c = findCustomer(customerId);
        PartnerRefResponseDto partner = partnerRefProvider.refs(Set.of(c.getChannelPartnerId())).get(c.getChannelPartnerId());
        LocalDate today = today();
        return followupRepository.findByCustomerIdAndDeletedFalseOrderByFollowupDateDescIdDesc(c.getId()).stream()
                .map(f -> toFollowupResponse(f, c, partner, today))
                .toList();
    }

    @Override
    @Transactional
    public FollowupResponseDto addFollowup(Long customerId, FollowupRequestDto req) {
        Customer c = findCustomer(customerId);
        validateFollowupDates(req);

        CustomerFollowup f = new CustomerFollowup();
        f.setChannelPartnerId(c.getChannelPartnerId());          // inherited from the customer
        f.setCustomerId(c.getId());
        applyFollowup(f, req);
        CustomerFollowup saved = followupRepository.save(f);

        // shows up in the customer's history
        auditService.record(AuditAction.UPDATE, "CUSTOMER", "Customer", c.getId(), c.getCode(),
                c.getChannelPartnerId(), DiffUtil.diff(null, followupSnapshot(saved)));
        return toFollowupSingle(saved, c);
    }

    @Override
    @Transactional
    public FollowupResponseDto updateFollowup(Long customerId, Long followupId, FollowupRequestDto req) {
        Customer c = findCustomer(customerId);
        validateFollowupDates(req);
        CustomerFollowup f = followupRepository.findByIdAndCustomerIdAndDeletedFalse(followupId, c.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found"));

        Map<String, Object> before = followupSnapshot(f);
        applyFollowup(f, req);
        CustomerFollowup saved = followupRepository.save(f);

        auditService.record(AuditAction.UPDATE, "CUSTOMER", "Customer", c.getId(), c.getCode(),
                c.getChannelPartnerId(), DiffUtil.diff(before, followupSnapshot(saved)));
        return toFollowupSingle(saved, c);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<FollowupResponseDto> allFollowups(FollowupFilterRequestDto f) {
        Long partnerId = tenantResolver.effectivePartnerId(f.getPartnerId());
        Long ownerId = tenantResolver.ownerFilter();

        // PENDING, COMPLETED and OVERDUE are expressed as (stored status, date rule)
        FollowupStatus stored = null;
        int mode = 0;
        String wanted = f.getStatus() == null ? "" : f.getStatus().trim().toUpperCase(Locale.ROOT);
        switch (wanted) {
            case "" -> { }
            case "COMPLETED" -> stored = FollowupStatus.COMPLETED;
            case "OVERDUE" -> { stored = FollowupStatus.PENDING; mode = 1; }
            case "PENDING" -> { stored = FollowupStatus.PENDING; mode = 2; }
            default -> throw new BusinessException("VALIDATION_ERROR", "Unknown follow-up status " + f.getStatus());
        }

        LocalDate today = today();
        Page<CustomerFollowup> page = followupRepository.search(partnerId, ownerId, f.getCustomerId(), stored, mode,
                today, f.getSearchOrNull(), f.toPageable("followupDate,desc", FOLLOWUP_SORT_FIELDS));

        Set<Long> customerIds = new HashSet<>();
        Set<Long> partnerIds = new HashSet<>();
        for (CustomerFollowup x : page.getContent()) {
            customerIds.add(x.getCustomerId());
            partnerIds.add(x.getChannelPartnerId());
        }
        Map<Long, Customer> customers = new HashMap<>();
        if (!customerIds.isEmpty()) {
            for (Customer c : customerRepository.findAllById(customerIds)) {
                customers.put(c.getId(), c);
            }
        }
        Map<Long, PartnerRefResponseDto> partners = partnerRefProvider.refs(partnerIds);
        final Map<Long, Customer> customersFinal = customers;
        final Map<Long, PartnerRefResponseDto> partnersFinal = partners;
        final LocalDate todayFinal = today;
        return PageResponse.of(page.map(x -> toFollowupResponse(
                x,
                customersFinal.get(x.getCustomerId()),
                partnersFinal.get(x.getChannelPartnerId()),
                todayFinal)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> history(Long customerId) {
        Customer c = findCustomer(customerId);          // 404 if it is not visible to the caller
        return auditService.entityHistory("Customer", c.getId());
    }

    // ---------------------------------------------------------------- helpers

    private Customer findCustomer(Long id) {
        return customerRepository.findOne(id, tenantResolver.effectivePartnerId(null), tenantResolver.ownerFilter())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private BusinessException duplicateMobile() {
        return new BusinessException("DUPLICATE", "A customer with this mobile number already exists", HttpStatus.CONFLICT);
    }

    private void applyFields(Customer c, CustomerRequestDto req) {
        c.setName(req.getName().trim());
        c.setEmail(blankToNull(req.getEmail()));
        c.setAddress(blankToNull(req.getAddress()));
        c.setCity(blankToNull(req.getCity()));
        c.setState(blankToNull(req.getState()));
        c.setPincode(blankToNull(req.getPincode()));
        String gst = blankToNull(req.getGstNo());
        c.setGstNo(gst == null ? null : gst.toUpperCase(Locale.ROOT));
        c.setType(CustomerType.valueOf(req.getType()));
        c.setConsumerNo(blankToNull(req.getConsumerNo()));
        c.setConsumerCategory(blankToNull(req.getConsumerCategory()));
        RecordStatus status = parseStatus(req.getStatus());
        c.setStatus(status != null ? status : (c.getStatus() != null ? c.getStatus() : RecordStatus.ACTIVE));
    }

    private void validateFollowupDates(FollowupRequestDto req) {
        if (req.getNextDate() != null && req.getNextDate().isBefore(req.getFollowupDate())) {
            throw new BusinessException("VALIDATION_ERROR", "Next date cannot be before the follow-up date");
        }
    }

    private void applyFollowup(CustomerFollowup f, FollowupRequestDto req) {
        f.setFollowupDate(req.getFollowupDate());
        f.setRemark(req.getRemark().trim());
        f.setNextDate(req.getNextDate());
        f.setStatus(req.getStatus() == null ? FollowupStatus.PENDING : FollowupStatus.valueOf(req.getStatus()));
    }

    private LocalDate today() {
        return LocalDate.now(BUSINESS_ZONE);
    }

    private String displayStatus(CustomerFollowup f, LocalDate today) {
        if (f.getStatus() == FollowupStatus.COMPLETED) {
            return "COMPLETED";
        }
        return (f.getNextDate() != null && f.getNextDate().isBefore(today)) ? "OVERDUE" : "PENDING";
    }

    private CustomerType parseType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CustomerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("VALIDATION_ERROR", "Unknown customer type " + value);
        }
    }

    private RecordStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return RecordStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("VALIDATION_ERROR", "Unknown status " + value);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private Map<String, Object> snapshot(Customer c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", c.getName());
        m.put("mobile", c.getMobile());
        m.put("email", c.getEmail());
        m.put("address", c.getAddress());
        m.put("city", c.getCity());
        m.put("state", c.getState());
        m.put("pincode", c.getPincode());
        m.put("gstNo", c.getGstNo());
        m.put("type", c.getType().name());
        m.put("consumerNo", c.getConsumerNo());
        m.put("consumerCategory", c.getConsumerCategory());
        m.put("status", c.getStatus().name());
        return m;
    }

    private Map<String, Object> followupSnapshot(CustomerFollowup f) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("followupDate", f.getFollowupDate() == null ? null : f.getFollowupDate().toString());
        m.put("followupRemark", f.getRemark());
        m.put("followupNextDate", f.getNextDate() == null ? null : f.getNextDate().toString());
        m.put("followupStatus", f.getStatus().name());
        return m;
    }

    /** Batch lookups so a page of customers costs a fixed number of extra queries. */
    private static final class Lookup {
        private Map<Long, PartnerRefResponseDto> partners = new HashMap<>();
        private Map<Long, String> names = new HashMap<>();
        private final Map<Long, Long> projectCounts = new HashMap<>();
        private final Map<Long, String> latestProject = new HashMap<>();
    }

    private Lookup lookup(List<Customer> customers) {
        Lookup lk = new Lookup();
        if (customers.isEmpty()) {
            return lk;
        }
        Set<Long> ids = new HashSet<>();
        Set<Long> partnerIds = new HashSet<>();
        for (Customer c : customers) {
            ids.add(c.getId());
            partnerIds.add(c.getChannelPartnerId());
        }
        lk.partners = partnerRefProvider.refs(partnerIds);
        lk.names = auditInfoMapper.namesFor(customers);
        for (Object[] row : projectRepository.countGroupedByCustomer(ids)) {
            lk.projectCounts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        for (Project p : projectRepository.findNewestForCustomers(ids)) {
            lk.latestProject.putIfAbsent(p.getCustomerId(), p.getName());
        }
        return lk;
    }

    private CustomerResponseDto toSingleResponse(Customer c) {
        return toResponse(c, lookup(List.of(c)));
    }

    private CustomerResponseDto toResponse(Customer c, Lookup lk) {
        CustomerResponseDto d = new CustomerResponseDto();
        d.setId(c.getId());
        d.setCode(c.getCode());
        d.setName(c.getName());
        d.setMobile(c.getMobile());
        d.setEmail(c.getEmail());
        d.setAddress(c.getAddress());
        d.setCity(c.getCity());
        d.setState(c.getState());
        d.setPincode(c.getPincode());
        d.setGstNo(c.getGstNo());
        d.setType(c.getType().name());
        d.setConsumerNo(c.getConsumerNo());
        d.setConsumerCategory(c.getConsumerCategory());
        d.setStatus(c.getStatus().name());
        d.setProjectSummary(lk.latestProject.get(c.getId()));
        d.setProjectCount(lk.projectCounts.getOrDefault(c.getId(), 0L));
        d.setQuotationCount(0L);                       // filled when the quotation module exists
        d.setPartner(lk.partners.get(c.getChannelPartnerId()));
        d.setAudit(auditInfoMapper.of(c, lk.names));
        return d;
    }

    private FollowupResponseDto toFollowupSingle(CustomerFollowup f, Customer c) {
        PartnerRefResponseDto partner = partnerRefProvider.refs(Set.of(c.getChannelPartnerId())).get(c.getChannelPartnerId());
        return toFollowupResponse(f, c, partner, today());
    }

    private FollowupResponseDto toFollowupResponse(CustomerFollowup f, Customer c, PartnerRefResponseDto partner,
                                                   LocalDate today) {
        FollowupResponseDto d = new FollowupResponseDto();
        d.setId(f.getId());
        d.setCustomerId(f.getCustomerId());
        d.setCustomerCode(c == null ? null : c.getCode());
        d.setCustomerName(c == null ? null : c.getName());
        d.setFollowupDate(f.getFollowupDate());
        d.setRemark(f.getRemark());
        d.setNextDate(f.getNextDate());
        d.setStatus(displayStatus(f, today));
        d.setPartner(partner);
        return d;
    }
}
