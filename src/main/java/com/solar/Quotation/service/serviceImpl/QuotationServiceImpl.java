package com.solar.Quotation.service.serviceImpl;

import com.solar.Quotation.Enum.AuditAction;
import com.solar.Quotation.Enum.DiscountType;
import com.solar.Quotation.Enum.QuotationStatus;
import com.solar.Quotation.Enum.ReferralStatus;
import com.solar.Quotation.dto.requestdto.QuotationFilterRequestDto;
import com.solar.Quotation.dto.requestdto.QuotationItemRequestDto;
import com.solar.Quotation.dto.requestdto.QuotationRequestDto;
import com.solar.Quotation.dto.requestdto.QuotationStatusRequestDto;
import com.solar.Quotation.dto.responsedto.AuditLogResponseDto;
import com.solar.Quotation.dto.responsedto.BankDetailsResponseDto;
import com.solar.Quotation.dto.responsedto.CustomerRefResponseDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.PartnerRefResponseDto;
import com.solar.Quotation.dto.responsedto.QuotationDefaultsResponseDto;
import com.solar.Quotation.dto.responsedto.QuotationItemResponseDto;
import com.solar.Quotation.dto.responsedto.QuotationListItemResponseDto;
import com.solar.Quotation.dto.responsedto.QuotationResponseDto;
import com.solar.Quotation.dto.responsedto.QuotationSummaryResponseDto;
import com.solar.Quotation.entity.Customer;
import com.solar.Quotation.entity.Project;
import com.solar.Quotation.entity.Quotation;
import com.solar.Quotation.entity.QuotationItem;
import com.solar.Quotation.exception.BusinessException;
import com.solar.Quotation.exception.ResourceNotFoundException;
import com.solar.Quotation.repository.CustomerRepository;
import com.solar.Quotation.repository.ProductRepository;
import com.solar.Quotation.repository.ProjectRepository;
import com.solar.Quotation.repository.QuotationItemRepository;
import com.solar.Quotation.repository.QuotationRepository;
import com.solar.Quotation.security.TenantResolver;
import com.solar.Quotation.service.AuditService;
import com.solar.Quotation.service.PartnerRefProvider;
import com.solar.Quotation.service.QuotationService;
import com.solar.Quotation.service.SettingsProvider;
import com.solar.Quotation.util.AuditInfoMapper;
import com.solar.Quotation.util.CodeGenerator;
import com.solar.Quotation.util.DiffUtil;
import com.solar.Quotation.util.MoneyUtil;
import com.solar.Quotation.util.NumberToWords;
import com.solar.Quotation.util.QuotationCalculator;
import com.solar.Quotation.util.SubsidyCalculator;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QuotationServiceImpl implements QuotationService {

    private static final Set<String> SORT_FIELDS =
            Set.of("createdAt", "code", "totalAmount", "status", "quoteDate", "systemSizeKw");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository itemRepository;
    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final ProductRepository productRepository;
    private final TenantResolver tenantResolver;
    private final PartnerRefProvider partnerRefProvider;
    private final AuditInfoMapper auditInfoMapper;
    private final AuditService auditService;
    private final CodeGenerator codeGenerator;
    private final SettingsProvider settingsProvider;

    public QuotationServiceImpl(QuotationRepository quotationRepository, QuotationItemRepository itemRepository,
                                CustomerRepository customerRepository, ProjectRepository projectRepository,
                                ProductRepository productRepository, TenantResolver tenantResolver,
                                PartnerRefProvider partnerRefProvider, AuditInfoMapper auditInfoMapper,
                                AuditService auditService, CodeGenerator codeGenerator,
                                SettingsProvider settingsProvider) {
        this.quotationRepository = quotationRepository;
        this.itemRepository = itemRepository;
        this.customerRepository = customerRepository;
        this.projectRepository = projectRepository;
        this.productRepository = productRepository;
        this.tenantResolver = tenantResolver;
        this.partnerRefProvider = partnerRefProvider;
        this.auditInfoMapper = auditInfoMapper;
        this.auditService = auditService;
        this.codeGenerator = codeGenerator;
        this.settingsProvider = settingsProvider;
    }

    // ---------------------------------------------------------------- read

    @Override
    @Transactional(readOnly = true)
    public PageResponse<QuotationListItemResponseDto> list(QuotationFilterRequestDto f) {
        Long partnerId = tenantResolver.effectivePartnerId(f.getPartnerId());
        Long ownerId = tenantResolver.ownerFilter();
        Page<Quotation> page = quotationRepository.search(partnerId, ownerId, parseStatus(f.getStatus()),
                f.getCustomerId(), f.getReferred(), f.getFromDate(), f.getToDate(), f.getSearchOrNull(),
                f.toPageable("createdAt,desc", SORT_FIELDS));
        ListLookup lookup = listLookup(page.getContent());
        return PageResponse.of(page.map(q -> toListItem(q, lookup)));
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationSummaryResponseDto summary(Long requestedPartnerId) {
        Long partnerId = tenantResolver.effectivePartnerId(requestedPartnerId);
        Long ownerId = tenantResolver.ownerFilter();
        QuotationSummaryResponseDto s = new QuotationSummaryResponseDto();
        s.setTotal(quotationRepository.countFiltered(partnerId, ownerId, null));
        s.setPending(quotationRepository.countFiltered(partnerId, ownerId, QuotationStatus.PENDING));
        s.setAccepted(quotationRepository.countFiltered(partnerId, ownerId, QuotationStatus.ACCEPTED));
        s.setRejected(quotationRepository.countFiltered(partnerId, ownerId, QuotationStatus.REJECTED));
        s.setTotalValue(quotationRepository.sumTotal(partnerId, ownerId, null));
        s.setAcceptedValue(quotationRepository.sumTotal(partnerId, ownerId, QuotationStatus.ACCEPTED));
        return s;
    }

    @Override
    @Transactional(readOnly = true)
    public QuotationResponseDto get(Long id) {
        Quotation q = findQuotation(id);
        return toResponse(q, itemRepository.findByQuotationIdOrderBySortOrderAsc(q.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponseDto> history(Long id) {
        Quotation q = findQuotation(id);                 // 404 if it is not visible to the caller
        return auditService.entityHistory("Quotation", q.getId());
    }

    // ---------------------------------------------------------------- write

    @Override
    @Transactional(readOnly = true)
    public QuotationResponseDto preview(QuotationRequestDto req) {
        Customer customer = findCustomerForCaller(req.getCustomerId());
        Quotation q = new Quotation();
        q.setChannelPartnerId(customer.getChannelPartnerId());
        q.setCustomerId(customer.getId());
        q.setQuoteDate(LocalDate.now(IST));
        q.setStatus(QuotationStatus.PENDING);
        List<QuotationItem> items = apply(q, req, customer, true);
        return toResponse(q, items);
    }

    @Override
    @Transactional
    public QuotationResponseDto create(QuotationRequestDto req) {
        // The customer decides the partner: a partner user can only pick a customer of their own partner,
        // Super Admin can pick any customer and the quotation then belongs to that customer's partner.
        Customer customer = findCustomerForCaller(req.getCustomerId());
        Long partnerId = customer.getChannelPartnerId();

        Quotation q = new Quotation();
        q.setChannelPartnerId(partnerId);
        q.setCustomerId(customer.getId());
        q.setQuoteDate(LocalDate.now(IST));
        q.setStatus(QuotationStatus.PENDING);
        List<QuotationItem> items = apply(q, req, customer, false);
        q.setCode(codeGenerator.next(codePrefix(), partnerId));
        Quotation saved = quotationRepository.save(q);

        saveItems(saved, items);

        auditService.record(AuditAction.CREATE, "QUOTATION", "Quotation", saved.getId(), saved.getCode(),
                partnerId, DiffUtil.diff(null, snapshot(saved, items)));
        return toResponse(saved, items);
    }

    @Override
    @Transactional
    public QuotationResponseDto update(Long id, QuotationRequestDto req) {
        Quotation q = findQuotation(id);
        if (q.getStatus() != QuotationStatus.PENDING) {
            throw new BusinessException("BUSINESS_RULE", "Only a pending quotation can be edited");
        }
        Customer customer;
        if (q.getCustomerId().equals(req.getCustomerId())) {
            customer = customerRepository.findById(q.getCustomerId())
                    .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Customer not found"));
        } else {
            // the new customer must belong to the SAME partner as the quotation
            customer = customerRepository.findOne(req.getCustomerId(), q.getChannelPartnerId(),
                            tenantResolver.ownerFilter())
                    .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Customer not found"));
            q.setCustomerId(customer.getId());
        }

        List<QuotationItem> oldItems = itemRepository.findByQuotationIdOrderBySortOrderAsc(q.getId());
        Map<String, Object> before = snapshot(q, oldItems);

        List<QuotationItem> items = apply(q, req, customer, false);
        Quotation saved = quotationRepository.save(q);

        itemRepository.deleteByQuotationId(saved.getId());
        saveItems(saved, items);

        auditService.record(AuditAction.UPDATE, "QUOTATION", "Quotation", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), DiffUtil.diff(before, snapshot(saved, items)));
        return toResponse(saved, items);
    }

    @Override
    @Transactional
    public QuotationResponseDto updateStatus(Long id, QuotationStatusRequestDto req) {
        Quotation q = findQuotation(id);
        List<QuotationItem> items = itemRepository.findByQuotationIdOrderBySortOrderAsc(q.getId());
        Map<String, Object> before = snapshot(q, items);

        QuotationStatus next = QuotationStatus.valueOf(req.getStatus());
        q.setStatus(next);
        // A referred quotation that gets accepted becomes a payable referral request (Channel Partner module approves it)
        if (next == QuotationStatus.ACCEPTED && q.isReferred() && q.getReferralStatus() == null) {
            q.setReferralStatus(ReferralStatus.PENDING);
        }
        Quotation saved = quotationRepository.save(q);

        auditService.record(AuditAction.STATUS_CHANGE, "QUOTATION", "Quotation", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), DiffUtil.diff(before, snapshot(saved, items)));
        return toResponse(saved, items);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Quotation q = findQuotation(id);
        if (q.getStatus() == QuotationStatus.ACCEPTED) {
            throw new BusinessException("BUSINESS_RULE", "An accepted quotation cannot be deleted");
        }
        String code = q.getCode();
        Long partnerId = q.getChannelPartnerId();
        q.setDeleted(true);
        quotationRepository.save(q);
        auditService.record(AuditAction.DELETE, "QUOTATION", "Quotation", id, code, partnerId, null);
    }

    // ---------------------------------------------------------------- helpers

    private Quotation findQuotation(Long id) {
        return quotationRepository.findOne(id, tenantResolver.effectivePartnerId(null), tenantResolver.ownerFilter())
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found"));
    }

    private Customer findCustomerForCaller(Long customerId) {
        return customerRepository.findOne(customerId, tenantResolver.effectivePartnerId(null),
                        tenantResolver.ownerFilter())
                .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Customer not found"));
    }

    private String codePrefix() {
        String p = settingsProvider.quotationDefaults().getPrefix();
        return (p == null || p.isBlank()) ? "SQ" : p.trim().toUpperCase(Locale.ROOT);
    }

    private void saveItems(Quotation saved, List<QuotationItem> items) {
        for (QuotationItem it : items) {
            it.setQuotationId(saved.getId());
            it.setChannelPartnerId(saved.getChannelPartnerId());
        }
        itemRepository.saveAll(items);
    }

    /**
     * Copies the request onto the quotation and returns the (unsaved) item lines.
     * Every money value is recalculated here; nothing the client sends for totals is trusted (there is none).
     */
    private List<QuotationItem> apply(Quotation q, QuotationRequestDto req, Customer customer, boolean preview) {
        Long partnerId = q.getChannelPartnerId();
        QuotationDefaultsResponseDto defaults = settingsProvider.quotationDefaults();

        // project (optional) must belong to the same customer
        if (req.getProjectId() != null) {
            Project p = projectRepository.findOne(req.getProjectId(), partnerId, tenantResolver.ownerFilter())
                    .orElseThrow(() -> new BusinessException("VALIDATION_ERROR", "Project not found"));
            if (!p.getCustomerId().equals(customer.getId())) {
                throw new BusinessException("VALIDATION_ERROR", "Project does not belong to this customer");
            }
        }
        q.setProjectId(req.getProjectId());

        // validity
        LocalDate validUntil = req.getValidUntil() != null
                ? req.getValidUntil()
                : q.getQuoteDate().plusDays(Math.max(defaults.getValidityDays(), 1));
        if (validUntil.isBefore(q.getQuoteDate())) {
            throw new BusinessException("VALIDATION_ERROR", "Valid-until date cannot be before the quotation date");
        }
        q.setValidUntil(validUntil);

        q.setSystemSizeKw(req.getSystemSizeKw().setScale(2, RoundingMode.HALF_UP));
        q.setMounting(blankToNull(req.getMounting()));

        // items
        List<QuotationItem> items = new ArrayList<>();
        BigDecimal itemsTotal = BigDecimal.ZERO;
        int order = 0;
        for (QuotationItemRequestDto r : req.getItems()) {
            if (r.getProductId() != null && productRepository.findOne(r.getProductId(), partnerId).isEmpty()) {
                throw new BusinessException("VALIDATION_ERROR", "Product not found: " + r.getName());
            }
            QuotationItem it = new QuotationItem();
            it.setChannelPartnerId(partnerId);
            it.setSortOrder(order++);
            it.setProductId(r.getProductId());
            it.setName(r.getName().trim());
            it.setType(blankToNull(r.getType()));
            it.setQty(r.getQty().setScale(3, RoundingMode.HALF_UP));
            it.setUnit(r.getUnit().trim());
            it.setRate(r.getRate().setScale(2, RoundingMode.HALF_UP));
            it.setAmount(it.getQty().multiply(it.getRate()).setScale(2, RoundingMode.HALF_UP));
            itemsTotal = itemsTotal.add(it.getAmount());
            items.add(it);
        }

        // extra costs, discount, GST
        q.setInstallationCost(MoneyUtil.scale(req.getInstallationCost()));
        q.setTransportCost(MoneyUtil.scale(req.getTransportCost()));
        q.setOtherLabel(blankToNull(req.getOtherLabel()));
        q.setOtherCost(MoneyUtil.scale(req.getOtherCost()));
        q.setGstPercent(req.getGstPercent().setScale(2, RoundingMode.HALF_UP));
        DiscountType discountType = DiscountType.valueOf(req.getDiscountType());
        BigDecimal discountValue = MoneyUtil.scale(req.getDiscountValue());
        if (discountType == DiscountType.PERCENT && discountValue.compareTo(HUNDRED) > 0) {
            throw new BusinessException("VALIDATION_ERROR", "Discount cannot be more than 100%");
        }
        q.setDiscountType(discountType);
        q.setDiscountValue(discountValue);

        QuotationCalculator.Totals t = QuotationCalculator.totals(itemsTotal, q.getInstallationCost(),
                q.getTransportCost(), q.getOtherCost(), discountType, discountValue, q.getGstPercent());
        if (discountType == DiscountType.FLAT && discountValue.compareTo(t.subtotal) > 0) {
            throw new BusinessException("VALIDATION_ERROR", "Discount cannot be more than the sub total");
        }
        q.setItemsTotal(MoneyUtil.scale(itemsTotal));
        q.setSubtotal(t.subtotal);
        q.setDiscountAmount(t.discountAmount);
        q.setTaxableAmount(t.taxable);
        q.setSgstAmount(t.sgst);
        q.setCgstAmount(t.cgst);
        q.setGstAmount(MoneyUtil.scale(t.sgst.add(t.cgst)));
        q.setTotalAmount(t.total);

        BigDecimal advance = MoneyUtil.scale(req.getAdvance());
        if (advance.compareTo(t.total) > 0) {
            throw new BusinessException("VALIDATION_ERROR", "Advance cannot be more than the total amount");
        }
        q.setAdvance(advance);
        q.setBalanceAmount(t.total.subtract(advance));

        // subsidy + net investment
        String category = blankToNull(customer.getConsumerCategory());
        if (category == null && customer.getType() != null) {
            category = customer.getType().name();
        }
        BigDecimal subsidy = SubsidyCalculator.calculate(category, q.getSystemSizeKw(), t.total,
                settingsProvider.activeSubsidyRules());
        q.setSubsidyAmount(subsidy);
        q.setNetInvestment(MoneyUtil.max(BigDecimal.ZERO, t.total.subtract(subsidy)).setScale(2, RoundingMode.HALF_UP));

        // savings inputs
        q.setTariff(MoneyUtil.scale(req.getTariff()));
        q.setEscalationPercent(MoneyUtil.scale(req.getEscalationPercent()));

        applyTerms(q, req, defaults);
        applyBank(q, req);
        applyReferral(q, req, defaults, t.total, preview);
        return items;
    }

    private void applyTerms(Quotation q, QuotationRequestDto req, QuotationDefaultsResponseDto defaults) {
        q.setTerms(blankToNull(req.getTerms()) != null ? req.getTerms().trim() : defaults.getTerms());

        BigDecimal adv;
        BigDecimal mat;
        BigDecimal ins;
        BigDecimal bal;
        QuotationRequestDto.PaymentTermsRequest p = req.getPaymentTerms();
        if (p != null) {
            adv = p.getAdvance();
            mat = p.getMaterial();
            ins = p.getInstallation();
            bal = p.getBalance();
        } else {
            QuotationDefaultsResponseDto.PaymentTerms d = defaults.getPaymentTerms();
            adv = d.getAdvance();
            mat = d.getMaterial();
            ins = d.getInstallation();
            bal = d.getBalance();
        }
        BigDecimal sum = adv.add(mat).add(ins).add(bal);
        if (sum.compareTo(HUNDRED) != 0) {
            throw new BusinessException("VALIDATION_ERROR",
                    "Payment terms must add up to 100% (currently " + sum.stripTrailingZeros().toPlainString() + "%)");
        }
        q.setPayAdvancePct(adv.setScale(2, RoundingMode.HALF_UP));
        q.setPayMaterialPct(mat.setScale(2, RoundingMode.HALF_UP));
        q.setPayInstallationPct(ins.setScale(2, RoundingMode.HALF_UP));
        q.setPayBalancePct(bal.setScale(2, RoundingMode.HALF_UP));
    }

    private void applyBank(Quotation q, QuotationRequestDto req) {
        BankDetailsResponseDto d = settingsProvider.bank();
        QuotationRequestDto.BankRequest b = req.getBank();
        q.setBankAccountName(pick(b == null ? null : b.getAccountName(), d.getAccountName()));
        q.setBankName(pick(b == null ? null : b.getBankName(), d.getBankName()));
        q.setBankAccountNumber(pick(b == null ? null : b.getAccountNumber(), d.getAccountNumber()));
        q.setBankIfsc(pick(b == null ? null : b.getIfsc(), d.getIfsc()));
        q.setBankBranch(pick(b == null ? null : b.getBranch(), d.getBranch()));
    }

    private void applyReferral(Quotation q, QuotationRequestDto req, QuotationDefaultsResponseDto defaults,
                               BigDecimal total, boolean preview) {
        QuotationRequestDto.ReferralRequest r = req.getReferral();
        if (r == null || !r.isReferred()) {
            q.setReferred(false);
            q.setReferredBy(null);
            q.setReferrerType(null);
            q.setReferralCode(null);
            q.setReferralPercent(null);
            q.setReferralAmount(BigDecimal.ZERO.setScale(2));
            q.setReferralStatus(null);
            return;
        }
        String by = blankToNull(r.getReferredBy());
        if (by == null) {
            throw new BusinessException("VALIDATION_ERROR", "Referred by is required when the quotation is a referral");
        }
        BigDecimal percent = r.getReferralPercent() != null ? r.getReferralPercent()
                : (defaults.getCustomerReferralPercent() != null ? defaults.getCustomerReferralPercent() : BigDecimal.ZERO);
        String code = blankToNull(r.getReferralCode());
        if (code == null) {
            code = q.getReferralCode() != null ? q.getReferralCode() : (preview ? null : generateReferralCode(by));
        }
        boolean wasReferred = q.isReferred();
        q.setReferred(true);
        q.setReferredBy(by);
        q.setReferrerType(blankToNull(r.getReferrerType()));
        q.setReferralCode(code);
        q.setReferralPercent(percent.setScale(2, RoundingMode.HALF_UP));
        q.setReferralAmount(MoneyUtil.percentOf(total, percent));
        if (!wasReferred || q.getReferralStatus() == null) {
            // starts as PENDING only once the quotation is accepted; until then it stays empty
            q.setReferralStatus(q.getStatus() == QuotationStatus.ACCEPTED ? ReferralStatus.PENDING : null);
        }
    }

    /** Initials of the name + 4 digits, for example "Priya Nair" -> PN4821. */
    private String generateReferralCode(String name) {
        StringBuilder initials = new StringBuilder();
        for (String part : name.trim().split("\\s+")) {
            if (!part.isEmpty() && Character.isLetterOrDigit(part.charAt(0)) && initials.length() < 3) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        if (initials.length() == 0) {
            initials.append("REF");
        }
        return initials + String.valueOf(ThreadLocalRandom.current().nextInt(1000, 10000));
    }

    private QuotationStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return QuotationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("VALIDATION_ERROR", "Unknown quotation status " + value);
        }
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String pick(String value, String fallback) {
        String v = blankToNull(value);
        return v != null ? v : fallback;
    }

    private Map<String, Object> snapshot(Quotation q, List<QuotationItem> items) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customerId", q.getCustomerId() == null ? null : q.getCustomerId().toString());
        m.put("status", q.getStatus().name());
        m.put("validUntil", q.getValidUntil() == null ? null : q.getValidUntil().toString());
        m.put("systemSizeKw", plain(q.getSystemSizeKw()));
        m.put("mounting", q.getMounting());
        StringBuilder sb = new StringBuilder();
        for (QuotationItem it : items) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(it.getName()).append(" x").append(plain(it.getQty())).append(" @").append(plain(it.getRate()));
        }
        m.put("items", sb.toString());
        m.put("installationCost", plain(q.getInstallationCost()));
        m.put("transportCost", plain(q.getTransportCost()));
        m.put("otherCost", plain(q.getOtherCost()));
        m.put("discountType", q.getDiscountType().name());
        m.put("discountValue", plain(q.getDiscountValue()));
        m.put("gstPercent", plain(q.getGstPercent()));
        m.put("totalAmount", plain(q.getTotalAmount()));
        m.put("advance", plain(q.getAdvance()));
        m.put("subsidyAmount", plain(q.getSubsidyAmount()));
        m.put("tariff", plain(q.getTariff()));
        m.put("escalationPercent", plain(q.getEscalationPercent()));
        m.put("referredBy", q.isReferred() ? q.getReferredBy() : null);
        m.put("referralPercent", plain(q.getReferralPercent()));
        m.put("terms", q.getTerms());
        return m;
    }

    private String plain(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    // ---------------------------------------------------------------- mapping

    private static final class ListLookup {
        private Map<Long, PartnerRefResponseDto> partners = new HashMap<>();
        private Map<Long, String> names = new HashMap<>();
        private final Map<Long, CustomerRefResponseDto> customers = new HashMap<>();
    }

    private ListLookup listLookup(List<Quotation> rows) {
        ListLookup lk = new ListLookup();
        if (rows.isEmpty()) {
            return lk;
        }
        Set<Long> customerIds = new HashSet<>();
        Set<Long> partnerIds = new HashSet<>();
        for (Quotation q : rows) {
            customerIds.add(q.getCustomerId());
            partnerIds.add(q.getChannelPartnerId());
        }
        lk.partners = partnerRefProvider.refs(partnerIds);
        lk.names = auditInfoMapper.namesFor(rows);
        for (Customer c : customerRepository.findAllById(customerIds)) {
            lk.customers.put(c.getId(), new CustomerRefResponseDto(c.getId(), c.getCode(), c.getName()));
        }
        return lk;
    }

    private QuotationListItemResponseDto toListItem(Quotation q, ListLookup lk) {
        QuotationListItemResponseDto d = new QuotationListItemResponseDto();
        d.setId(q.getId());
        d.setCode(q.getCode());
        d.setCustomer(lk.customers.get(q.getCustomerId()));
        d.setSystemSizeKw(q.getSystemSizeKw());
        d.setTotalAmount(q.getTotalAmount());
        d.setBalanceAmount(q.getBalanceAmount());
        d.setStatus(q.getStatus().name());
        d.setQuoteDate(q.getQuoteDate());
        d.setValidUntil(q.getValidUntil());
        d.setReferred(q.isReferred());
        d.setReferredBy(q.getReferredBy());
        d.setPartner(lk.partners.get(q.getChannelPartnerId()));
        d.setAudit(auditInfoMapper.of(q, lk.names));
        return d;
    }

    private QuotationResponseDto toResponse(Quotation q, List<QuotationItem> items) {
        QuotationResponseDto d = new QuotationResponseDto();
        d.setId(q.getId());
        d.setCode(q.getCode());
        d.setStatus(q.getStatus().name());
        d.setQuoteDate(q.getQuoteDate());
        d.setValidUntil(q.getValidUntil());
        d.setProjectId(q.getProjectId());
        d.setSystemSizeKw(q.getSystemSizeKw());
        d.setMounting(q.getMounting());

        Customer c = customerRepository.findById(q.getCustomerId()).orElse(null);
        if (c != null) {
            QuotationResponseDto.CustomerBlock cb = new QuotationResponseDto.CustomerBlock();
            cb.setId(c.getId());
            cb.setCode(c.getCode());
            cb.setName(c.getName());
            cb.setMobile(c.getMobile());
            cb.setEmail(c.getEmail());
            cb.setAddress(c.getAddress());
            cb.setCity(c.getCity());
            cb.setState(c.getState());
            cb.setPincode(c.getPincode());
            cb.setGstNo(c.getGstNo());
            cb.setType(c.getType() == null ? null : c.getType().name());
            cb.setConsumerNo(c.getConsumerNo());
            cb.setConsumerCategory(c.getConsumerCategory());
            d.setCustomer(cb);
        }

        List<QuotationItemResponseDto> itemDtos = new ArrayList<>();
        for (QuotationItem it : items) {
            QuotationItemResponseDto i = new QuotationItemResponseDto();
            i.setId(it.getId());
            i.setProductId(it.getProductId());
            i.setName(it.getName());
            i.setType(it.getType());
            i.setQty(it.getQty());
            i.setUnit(it.getUnit());
            i.setRate(it.getRate());
            i.setAmount(it.getAmount());
            itemDtos.add(i);
        }
        d.setItems(itemDtos);

        d.setItemsTotal(q.getItemsTotal());
        d.setInstallationCost(q.getInstallationCost());
        d.setTransportCost(q.getTransportCost());
        d.setOtherLabel(q.getOtherLabel());
        d.setOtherCost(q.getOtherCost());
        d.setSubtotal(q.getSubtotal());
        d.setDiscountType(q.getDiscountType().name());
        d.setDiscountValue(q.getDiscountValue());
        d.setDiscountAmount(q.getDiscountAmount());
        d.setGstPercent(q.getGstPercent());
        d.setTaxableAmount(q.getTaxableAmount());
        d.setSgstAmount(q.getSgstAmount());
        d.setCgstAmount(q.getCgstAmount());
        d.setTotalAmount(q.getTotalAmount());
        d.setAmountInWords(NumberToWords.rupees(q.getTotalAmount().setScale(0, RoundingMode.HALF_UP).longValue()));
        d.setAdvance(q.getAdvance());
        d.setBalanceAmount(q.getBalanceAmount());
        d.setSubsidyAmount(q.getSubsidyAmount());
        d.setNetInvestment(q.getNetInvestment());

        d.setTariff(q.getTariff());
        d.setEscalationPercent(q.getEscalationPercent());
        d.setSavings(QuotationCalculator.savings(q.getSystemSizeKw(), q.getTariff(), q.getEscalationPercent(),
                q.getNetInvestment(), settingsProvider.savingsConfig()));

        d.setTerms(q.getTerms());
        QuotationResponseDto.PaymentTerms pt = new QuotationResponseDto.PaymentTerms();
        pt.setAdvance(q.getPayAdvancePct());
        pt.setMaterial(q.getPayMaterialPct());
        pt.setInstallation(q.getPayInstallationPct());
        pt.setBalance(q.getPayBalancePct());
        d.setPaymentTerms(pt);

        QuotationResponseDto.Bank b = new QuotationResponseDto.Bank();
        b.setAccountName(q.getBankAccountName());
        b.setBankName(q.getBankName());
        b.setAccountNumber(q.getBankAccountNumber());
        b.setIfsc(q.getBankIfsc());
        b.setBranch(q.getBankBranch());
        d.setBank(b);

        QuotationResponseDto.Referral r = new QuotationResponseDto.Referral();
        r.setReferred(q.isReferred());
        r.setReferredBy(q.getReferredBy());
        r.setReferrerType(q.getReferrerType());
        r.setReferralCode(q.getReferralCode());
        r.setReferralPercent(q.getReferralPercent());
        r.setReferralAmount(q.getReferralAmount());
        r.setReferralStatus(q.getReferralStatus() == null ? null : q.getReferralStatus().name());
        d.setReferral(r);

        d.setPartner(partnerRefProvider.ref(q.getChannelPartnerId()));
        d.setAudit(auditInfoMapper.of(q, auditInfoMapper.namesFor(List.of(q))));
        return d;
    }
}
