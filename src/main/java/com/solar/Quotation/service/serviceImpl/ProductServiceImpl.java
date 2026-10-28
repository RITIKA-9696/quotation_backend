package com.solar.Quotation.service.serviceImpl;

import com.solar.Quotation.Enum.AuditAction;
import com.solar.Quotation.Enum.ProductCategory;
import com.solar.Quotation.Enum.RecordStatus;
import com.solar.Quotation.dto.requestdto.ProductFilterRequestDto;
import com.solar.Quotation.dto.requestdto.ProductRequestDto;
import com.solar.Quotation.dto.requestdto.ProductStatusRequestDto;
import com.solar.Quotation.dto.requestdto.StockAdjustRequestDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.PartnerRefResponseDto;
import com.solar.Quotation.dto.responsedto.ProductCatalogItemResponseDto;
import com.solar.Quotation.dto.responsedto.ProductResponseDto;
import com.solar.Quotation.dto.responsedto.ProductSummaryResponseDto;
import com.solar.Quotation.entity.Product;
import com.solar.Quotation.exception.BusinessException;
import com.solar.Quotation.exception.ResourceNotFoundException;
import com.solar.Quotation.repository.ProductRepository;
import com.solar.Quotation.security.TenantResolver;
import com.solar.Quotation.service.AuditService;
import com.solar.Quotation.service.PartnerRefProvider;
import com.solar.Quotation.service.ProductService;
import com.solar.Quotation.util.AuditInfoMapper;
import com.solar.Quotation.util.CodeGenerator;
import com.solar.Quotation.util.DiffUtil;
import com.solar.Quotation.util.PartnerGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Products are private to each partner. There is deliberately no owner (data scope OWN) filter here:
 * the catalog is shared by every user of the same partner.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "name", "category", "brand", "price", "stock", "status");
    private static final int CATALOG_LIMIT = 1000;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final ProductRepository productRepository;
    private final TenantResolver tenantResolver;
    private final PartnerRefProvider partnerRefProvider;
    private final AuditInfoMapper auditInfoMapper;
    private final AuditService auditService;
    private final CodeGenerator codeGenerator;
    private final PartnerGuard partnerGuard;

    public ProductServiceImpl(ProductRepository productRepository, TenantResolver tenantResolver,
                              PartnerRefProvider partnerRefProvider, AuditInfoMapper auditInfoMapper,
                              AuditService auditService, CodeGenerator codeGenerator, PartnerGuard partnerGuard) {
        this.productRepository = productRepository;
        this.tenantResolver = tenantResolver;
        this.partnerRefProvider = partnerRefProvider;
        this.auditInfoMapper = auditInfoMapper;
        this.auditService = auditService;
        this.codeGenerator = codeGenerator;
        this.partnerGuard = partnerGuard;
    }

    // ---------------------------------------------------------------- read

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponseDto> list(ProductFilterRequestDto f) {
        Long partnerId = tenantResolver.effectivePartnerId(f.getPartnerId());
        Page<Product> page = productRepository.search(partnerId, parseCategory(f.getCategory()),
                parseStatus(f.getStatus()), f.getGstPercent(), parseStockMode(f.getStockStatus()),
                f.getSearchOrNull(), f.toPageable("createdAt,desc", SORT_FIELDS));
        Lookup lookup = lookup(page.getContent());
        return PageResponse.of(page.map(p -> toResponse(p, lookup)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCatalogItemResponseDto> catalog(Long requestedPartnerId) {
        Long partnerId = tenantResolver.effectivePartnerId(requestedPartnerId);
        return productRepository.findCatalog(partnerId, RecordStatus.ACTIVE, PageRequest.of(0, CATALOG_LIMIT))
                .stream().map(this::toCatalogItem).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductSummaryResponseDto summary(Long requestedPartnerId) {
        Long partnerId = tenantResolver.effectivePartnerId(requestedPartnerId);
        ProductSummaryResponseDto s = new ProductSummaryResponseDto();
        s.setTotal(productRepository.countFiltered(partnerId, null, 0));
        s.setActive(productRepository.countFiltered(partnerId, RecordStatus.ACTIVE, 0));
        s.setLowStock(productRepository.countFiltered(partnerId, null, 2));
        s.setOutOfStock(productRepository.countFiltered(partnerId, null, 1));
        BigDecimal value = productRepository.sumInventoryValue(partnerId);
        s.setInventoryValue(value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP));
        return s;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto get(Long id) {
        return toSingleResponse(findProduct(id));
    }

    // ---------------------------------------------------------------- write

    @Override
    @Transactional
    public ProductResponseDto create(ProductRequestDto req) {
        Long partnerId = tenantResolver.requirePartnerIdForCreate(req.getPartnerId());
        partnerGuard.assertUsable(partnerId);

        String sku = blankToNull(req.getSku());
        if (sku != null && productRepository.existsByChannelPartnerIdAndSkuAndDeletedFalse(partnerId, sku)) {
            throw duplicateSku();
        }

        Product p = new Product();
        p.setChannelPartnerId(partnerId);
        p.setCode(codeGenerator.next("PRD", partnerId));
        p.setSku(sku);
        applyFields(p, req);
        Product saved = productRepository.save(p);

        auditService.record(AuditAction.CREATE, "PRODUCT", "Product", saved.getId(), saved.getCode(),
                partnerId, DiffUtil.diff(null, snapshot(saved)));
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto update(Long id, ProductRequestDto req) {
        Product p = findProduct(id);
        String sku = blankToNull(req.getSku());
        if (sku != null && !sku.equals(p.getSku())
                && productRepository.existsByChannelPartnerIdAndSkuAndDeletedFalseAndIdNot(
                p.getChannelPartnerId(), sku, p.getId())) {
            throw duplicateSku();
        }

        Map<String, Object> before = snapshot(p);
        p.setSku(sku);
        applyFields(p, req);
        Product saved = productRepository.save(p);

        auditService.record(AuditAction.UPDATE, "PRODUCT", "Product", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), DiffUtil.diff(before, snapshot(saved)));
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto updateStatus(Long id, ProductStatusRequestDto req) {
        Product p = findProduct(id);
        Map<String, Object> before = snapshot(p);
        p.setStatus(RecordStatus.valueOf(req.getStatus()));
        Product saved = productRepository.save(p);

        auditService.record(AuditAction.STATUS_CHANGE, "PRODUCT", "Product", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), DiffUtil.diff(before, snapshot(saved)));
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponseDto adjustStock(Long id, StockAdjustRequestDto req) {
        if (req.getAdjustment() == 0) {
            throw new BusinessException("VALIDATION_ERROR", "Adjustment cannot be 0");
        }
        Product p = findProduct(id);
        long newStock = (long) p.getStock() + req.getAdjustment();
        if (newStock < 0) {
            throw new BusinessException("BUSINESS_RULE", "Stock cannot go below zero");
        }
        if (newStock > Integer.MAX_VALUE) {
            throw new BusinessException("VALIDATION_ERROR", "Stock is too large");
        }

        Map<String, Object> before = new LinkedHashMap<>();
        before.put("stock", String.valueOf(p.getStock()));
        p.setStock((int) newStock);
        Product saved = productRepository.save(p);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("stock", String.valueOf(saved.getStock()));
        List<Map<String, String>> changes = new ArrayList<>(DiffUtil.diff(before, after));
        Map<String, String> reason = new LinkedHashMap<>();
        reason.put("field", "stockReason");
        reason.put("old", null);
        reason.put("new", req.getReason().trim());
        changes.add(reason);

        auditService.record(AuditAction.UPDATE, "PRODUCT", "Product", saved.getId(), saved.getCode(),
                saved.getChannelPartnerId(), changes);
        return toSingleResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product p = findProduct(id);
        String code = p.getCode();
        Long partnerId = p.getChannelPartnerId();
        if (p.getSku() != null) {
            p.setSku(p.getSku() + "~del" + p.getId());        // frees the sku for reuse
        }
        p.setStatus(RecordStatus.INACTIVE);
        p.setDeleted(true);
        productRepository.save(p);
        auditService.record(AuditAction.DELETE, "PRODUCT", "Product", id, code, partnerId, null);
    }

    // ---------------------------------------------------------------- helpers

    private Product findProduct(Long id) {
        return productRepository.findOne(id, tenantResolver.effectivePartnerId(null))
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private BusinessException duplicateSku() {
        return new BusinessException("DUPLICATE", "A product with this SKU already exists", HttpStatus.CONFLICT);
    }

    private void applyFields(Product p, ProductRequestDto req) {
        p.setName(req.getName().trim());
        p.setCategory(ProductCategory.valueOf(req.getCategory()));
        p.setBrand(blankToNull(req.getBrand()));
        p.setSpec(blankToNull(req.getSpec()));
        p.setUnit(req.getUnit().trim());
        p.setHsn(blankToNull(req.getHsn()));
        p.setGstPercent(req.getGstPercent());
        p.setWarrantyYears(req.getWarrantyYears());
        p.setMrp(req.getMrp());
        p.setDiscountPercent(req.getDiscountPercent());
        p.setPrice(computePrice(req));
        p.setStock(req.getStock());
        p.setLowStockThreshold(req.getLowStockThreshold() == null ? 10 : req.getLowStockThreshold());
        p.setQtyPerKw(req.getQtyPerKw());
        RecordStatus status = parseStatus(req.getStatus());
        p.setStatus(status != null ? status : (p.getStatus() != null ? p.getStatus() : RecordStatus.ACTIVE));
        p.setDescription(blankToNull(req.getDescription()));
    }

    /** price = mrp x (1 - discount / 100) when both are given, otherwise the price that was sent. */
    BigDecimal computePrice(ProductRequestDto req) {
        if (req.getMrp() != null && req.getDiscountPercent() != null) {
            BigDecimal factor = BigDecimal.ONE.subtract(req.getDiscountPercent().divide(HUNDRED, 6, RoundingMode.HALF_UP));
            return req.getMrp().multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }
        if (req.getPrice() == null) {
            throw new BusinessException("VALIDATION_ERROR", "Price is required when MRP and discount are not both given");
        }
        return req.getPrice().setScale(2, RoundingMode.HALF_UP);
    }

    static String stockStatus(Product p) {
        if (p.getStock() <= 0) {
            return "OUT_OF_STOCK";
        }
        return p.getStock() <= p.getLowStockThreshold() ? "LOW_STOCK" : "IN_STOCK";
    }

    private ProductCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return ProductCategory.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException("VALIDATION_ERROR", "Unknown category " + value);
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

    /** 0 = any, 1 = out of stock, 2 = low stock, 3 = in stock (same codes as ProductRepository). */
    private int parseStockMode(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "OUT_OF_STOCK" -> 1;
            case "LOW_STOCK" -> 2;
            case "IN_STOCK" -> 3;
            default -> throw new BusinessException("VALIDATION_ERROR", "Unknown stock status " + value);
        };
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private String plain(BigDecimal v) {
        return v == null ? null : v.stripTrailingZeros().toPlainString();
    }

    private Map<String, Object> snapshot(Product p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", p.getName());
        m.put("category", p.getCategory().name());
        m.put("brand", p.getBrand());
        m.put("sku", p.getSku());
        m.put("spec", p.getSpec());
        m.put("unit", p.getUnit());
        m.put("hsn", p.getHsn());
        m.put("gstPercent", plain(p.getGstPercent()));
        m.put("warrantyYears", p.getWarrantyYears() == null ? null : p.getWarrantyYears().toString());
        m.put("mrp", plain(p.getMrp()));
        m.put("discountPercent", plain(p.getDiscountPercent()));
        m.put("price", plain(p.getPrice()));
        m.put("stock", String.valueOf(p.getStock()));
        m.put("lowStockThreshold", String.valueOf(p.getLowStockThreshold()));
        m.put("qtyPerKw", plain(p.getQtyPerKw()));
        m.put("status", p.getStatus().name());
        m.put("description", p.getDescription());
        return m;
    }

    private static final class Lookup {
        private Map<Long, PartnerRefResponseDto> partners = new HashMap<>();
        private Map<Long, String> names = new HashMap<>();
    }

    private Lookup lookup(List<Product> products) {
        Lookup lk = new Lookup();
        if (products.isEmpty()) {
            return lk;
        }
        Set<Long> partnerIds = new HashSet<>();
        for (Product p : products) {
            partnerIds.add(p.getChannelPartnerId());
        }
        lk.partners = partnerRefProvider.refs(partnerIds);
        lk.names = auditInfoMapper.namesFor(products);
        return lk;
    }

    private ProductResponseDto toSingleResponse(Product p) {
        return toResponse(p, lookup(List.of(p)));
    }

    private ProductResponseDto toResponse(Product p, Lookup lk) {
        ProductResponseDto d = new ProductResponseDto();
        d.setId(p.getId());
        d.setCode(p.getCode());
        d.setName(p.getName());
        d.setCategory(p.getCategory().name());
        d.setBrand(p.getBrand());
        d.setSku(p.getSku());
        d.setSpec(p.getSpec());
        d.setUnit(p.getUnit());
        d.setHsn(p.getHsn());
        d.setGstPercent(p.getGstPercent());
        d.setWarrantyYears(p.getWarrantyYears());
        d.setMrp(p.getMrp());
        d.setDiscountPercent(p.getDiscountPercent());
        d.setPrice(p.getPrice());
        d.setStock(p.getStock());
        d.setLowStockThreshold(p.getLowStockThreshold());
        d.setQtyPerKw(p.getQtyPerKw());
        d.setStatus(p.getStatus().name());
        d.setStockStatus(stockStatus(p));
        d.setDescription(p.getDescription());
        d.setPartner(lk.partners.get(p.getChannelPartnerId()));
        d.setAudit(auditInfoMapper.of(p, lk.names));
        return d;
    }

    private ProductCatalogItemResponseDto toCatalogItem(Product p) {
        ProductCatalogItemResponseDto d = new ProductCatalogItemResponseDto();
        d.setId(p.getId());
        d.setName(p.getName());
        d.setCategory(p.getCategory().name());
        d.setBrand(p.getBrand());
        d.setSpec(p.getSpec());
        d.setUnit(p.getUnit());
        d.setHsn(p.getHsn());
        d.setGstPercent(p.getGstPercent());
        d.setPrice(p.getPrice());
        d.setQtyPerKw(p.getQtyPerKw());
        d.setWarrantyYears(p.getWarrantyYears());
        d.setStock(p.getStock());
        return d;
    }
}
