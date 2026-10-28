package com.solar.Quotation.service.serviceImpl;

import com.solar.Quotation.Enum.QuotationStatus;
import com.solar.Quotation.dto.requestdto.ReportFilterRequestDto;
import com.solar.Quotation.dto.responsedto.PageResponse;
import com.solar.Quotation.dto.responsedto.ReportRowResponseDto;
import com.solar.Quotation.dto.responsedto.ReportSummaryResponseDto;
import com.solar.Quotation.exception.BusinessException;
import com.solar.Quotation.repository.QuotationRepository;
import com.solar.Quotation.security.TenantResolver;
import com.solar.Quotation.service.ReportService;
import org.springframework.data.domain.*;
import com.solar.Quotation.util.ExcelExportUtil;
import com.solar.Quotation.util.PdfExportUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final QuotationRepository quotationRepo;
    private final TenantResolver tenantResolver;

    public ReportServiceImpl(QuotationRepository quotationRepo, TenantResolver tenantResolver) {
        this.quotationRepo = quotationRepo;
        this.tenantResolver = tenantResolver;
    }

    private Long partner(ReportFilterRequestDto f) { return tenantResolver.effectivePartnerId(f.getPartnerId()); }
    private Long owner() { return tenantResolver.ownerFilter(); }

    private QuotationStatus status(String s) {
        if (s == null || s.isBlank()) return null;
        try { return QuotationStatus.valueOf(s.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) {
            throw new BusinessException("VALIDATION_ERROR", "Unknown status " + s);
        }
    }

    private Pageable pageable(ReportFilterRequestDto f) {
        String[] p = f.getSort() == null || f.getSort().isBlank()
                ? new String[]{"createdAt", "desc"} : f.getSort().split(",");
        Sort.Direction dir = p.length > 1 && "asc".equalsIgnoreCase(p[1]) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(f.getPage(), f.getSize(), Sort.by(dir, p[0]));
    }

    @Override
    public ReportSummaryResponseDto getSummary(ReportFilterRequestDto f) {
        Object[] r = quotationRepo.reportSummary(
                partner(f), owner(), status(f.getStatus()), f.getSalesUserId(), f.getFrom(), f.getTo());
        ReportSummaryResponseDto d = new ReportSummaryResponseDto();
        if (r != null) {
            d.setTotalQuotations(r[0] == null ? 0L : ((Number) r[0]).longValue());
            d.setAccepted(r[1] == null ? 0L : ((Number) r[1]).longValue());
            d.setPending(r[2] == null ? 0L : ((Number) r[2]).longValue());
            d.setRejected(r[3] == null ? 0L : ((Number) r[3]).longValue());
            d.setTotalValue(r[4] == null ? BigDecimal.ZERO : (BigDecimal) r[4]);
            d.setTotalGst(r[5] == null ? BigDecimal.ZERO : (BigDecimal) r[5]);
            d.setTotalReferralPayable(r[6] == null ? BigDecimal.ZERO : (BigDecimal) r[6]);
        }
        return d;
    }

    @Override
    public PageResponse<ReportRowResponseDto.Quotation> quotationReport(ReportFilterRequestDto f, Pageable ignored) {
        Page<Object[]> page = quotationRepo.quotationReport(
                partner(f), owner(), status(f.getStatus()), f.getSalesUserId(),
                f.getFrom(), f.getTo(), pageable(f));

        return PageResponse.of(page.map(r -> {
            ReportRowResponseDto.Quotation d = new ReportRowResponseDto.Quotation();
            d.setQuoteNo((String) r[0]);
            d.setCustomer((String) r[1]);
            d.setSystemSizeKw((BigDecimal) r[2]);
            d.setAmount((BigDecimal) r[3]);
            d.setSalesPerson((String) r[4]);
            d.setStatus(r[5] == null ? null : r[5].toString());
            d.setDate(toLocalDate(r[6]));
            d.setPartnerName((String) r[7]);
            return d;
        }));
    }

    @Override
    public PageResponse<ReportRowResponseDto.Gst> gstReport(ReportFilterRequestDto f, Pageable ignored) {
        Page<Object[]> page = quotationRepo.gstReport(
                partner(f), owner(), status(f.getStatus()), f.getSalesUserId(),
                f.getFrom(), f.getTo(), pageable(f));

        return PageResponse.of(page.map(r -> {
            ReportRowResponseDto.Gst d = new ReportRowResponseDto.Gst();
            d.setQuoteNo((String) r[0]);
            d.setCustomer((String) r[1]);
            d.setTaxableAmount((BigDecimal) r[2]);
            d.setGstPercent((BigDecimal) r[3]);
            d.setCgstAmount((BigDecimal) r[4]);
            d.setSgstAmount((BigDecimal) r[5]);
            d.setGstAmount((BigDecimal) r[6]);
            d.setTotal((BigDecimal) r[7]);
            d.setDate(toLocalDate(r[8]));
            return d;
        }));
    }

    @Override
    public PageResponse<ReportRowResponseDto.ReferralIncome> referralIncomeReport(ReportFilterRequestDto f, Pageable ignored) {
        Page<Object[]> page = quotationRepo.referralIncomeReport(
                partner(f), owner(), status(f.getStatus()), f.getSalesUserId(),
                f.getFrom(), f.getTo(), pageable(f));

        return PageResponse.of(page.map(r -> {
            ReportRowResponseDto.ReferralIncome d = new ReportRowResponseDto.ReferralIncome();
            d.setReferrerName((String) r[0]);
            d.setReferredCustomer((String) r[1]);
            d.setQuotationAmount((BigDecimal) r[2]);
            d.setReferralPercent((BigDecimal) r[3]);
            d.setReferralAmount((BigDecimal) r[4]);
            d.setStatus(r[5] == null ? null : r[5].toString());
            return d;
        }));
    }

    @Override
    public PageResponse<ReportRowResponseDto.Performer> topPerformers(ReportFilterRequestDto f, Pageable ignored) {
        Page<Object[]> page = quotationRepo.topPerformers(
                partner(f), owner(), f.getFrom(), f.getTo(), pageable(f));

        return PageResponse.of(page.map(r -> {
            ReportRowResponseDto.Performer d = new ReportRowResponseDto.Performer();
            d.setUserId(r[0] == null ? null : ((Number) r[0]).longValue());
            d.setName((String) r[1]);
            d.setQuotes(r[2] == null ? 0L : ((Number) r[2]).longValue());
            d.setAccepted(r[3] == null ? 0L : ((Number) r[3]).longValue());
            d.setRatePercent(d.getQuotes() > 0
                    ? BigDecimal.valueOf(d.getAccepted() * 100.0 / d.getQuotes()).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            return d;
        }));
    }

    @Override
    public byte[] export(ReportFilterRequestDto f, String type, String format) {
        ReportFilterRequestDto full = clone(f);
        if ("GST".equalsIgnoreCase(type)) {
            List<ReportRowResponseDto.Gst> list = gstReport(full, null).getContent();
            return "EXCEL".equalsIgnoreCase(format) ? ExcelExportUtil.gst(list) : PdfExportUtil.gst(list);
        }
        if ("REFERRAL_INCOME".equalsIgnoreCase(type)) {
            List<ReportRowResponseDto.ReferralIncome> list = referralIncomeReport(full, null).getContent();
            return "EXCEL".equalsIgnoreCase(format) ? ExcelExportUtil.referral(list) : PdfExportUtil.referral(list);
        }
        List<ReportRowResponseDto.Quotation> list = quotationReport(full, null).getContent();
        return "EXCEL".equalsIgnoreCase(format) ? ExcelExportUtil.quotations(list) : PdfExportUtil.quotations(list);
    }

    private ReportFilterRequestDto clone(ReportFilterRequestDto f) {
        ReportFilterRequestDto c = new ReportFilterRequestDto();
        c.setFrom(f.getFrom());
        c.setTo(f.getTo());
        c.setStatus(f.getStatus());
        c.setSalesUserId(f.getSalesUserId());
        c.setPartnerId(f.getPartnerId());
        c.setSort(f.getSort());
        c.setPage(0);
        c.setSize(10000);
        return c;
    }

    private LocalDate toLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate ld) return ld;
        if (o instanceof LocalDateTime ldt) return ldt.toLocalDate();
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        if (o instanceof java.sql.Timestamp t) return t.toLocalDateTime().toLocalDate();
        return null;
    }
}