package com.solar.Quotation.service.serviceImpl;

import com.solar.Quotation.dto.responsedto.DashboardResponseDto;
import com.solar.Quotation.dto.responsedto.DashboardRecentQuotationResponseDto;
import com.solar.Quotation.repository.ChannelPartnerRepository;
import com.solar.Quotation.repository.CustomerRepository;
import com.solar.Quotation.repository.ProjectRepository;
import com.solar.Quotation.repository.QuotationRepository;
import com.solar.Quotation.security.CurrentUser;
import com.solar.Quotation.security.TenantContext;
import com.solar.Quotation.security.TenantResolver;
import com.solar.Quotation.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final QuotationRepository quotationRepo;
    private final CustomerRepository customerRepo;
    private final ProjectRepository projectRepo;
    private final ChannelPartnerRepository partnerRepo;
    private final TenantResolver tenantResolver;

    public DashboardServiceImpl(QuotationRepository quotationRepo, CustomerRepository customerRepo,
                                ProjectRepository projectRepo, ChannelPartnerRepository partnerRepo,
                                TenantResolver tenantResolver) {
        this.quotationRepo = quotationRepo;
        this.customerRepo = customerRepo;
        this.projectRepo = projectRepo;
        this.partnerRepo = partnerRepo;
        this.tenantResolver = tenantResolver;
    }

    @Override
    public DashboardResponseDto getSummary(Long partnerIdFilter) {
        Long partnerId = tenantResolver.effectivePartnerId(partnerIdFilter);
        Long ownerId = tenantResolver.ownerFilter();
        CurrentUser u = TenantContext.get();
        boolean superAdmin = u != null && u.isSuperAdmin();

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate nextMonth = monthStart.plusMonths(1);

        DashboardResponseDto d = new DashboardResponseDto();

        // Core Counts
        long totalQuotes = quotationRepo.countFiltered(partnerId, ownerId, null);
        d.setTotalQuotations(totalQuotes);
        d.setTotalCustomers(customerRepo.countScoped(partnerId, ownerId));
        d.setTotalProjects(projectRepo.countScoped(partnerId, ownerId));

        // Values
        d.setTotalQuotationValue(quotationRepo.sumTotal(partnerId, ownerId, null));
        d.setMonthQuotationValue(quotationRepo.sumTotalBetween(partnerId, ownerId, monthStart, nextMonth));

        // Status Counts
        long accepted = quotationRepo.countFiltered(partnerId, ownerId, com.solar.Quotation.Enum.QuotationStatus.ACCEPTED);
        long rejected = quotationRepo.countFiltered(partnerId, ownerId, com.solar.Quotation.Enum.QuotationStatus.REJECTED);
        long pending = quotationRepo.countFiltered(partnerId, ownerId, com.solar.Quotation.Enum.QuotationStatus.PENDING);

        d.setAcceptedQuotations(accepted);
        d.setRejectedQuotations(rejected);
        d.setPendingQuotations(pending);
        d.setPendingApprovals(pending);

        // Date calculations
        d.setTodayQuotations(quotationRepo.countBetween(partnerId, ownerId, today, today));
        d.setMonthQuotations(quotationRepo.countBetween(partnerId, ownerId, monthStart, nextMonth.minusDays(1)));

        // Win Rate
        int winRate = totalQuotes > 0 ? (int) Math.round((accepted * 100.0) / totalQuotes) : 0;
        d.setWinRatePercent(winRate);

        // System Size (Chart Data)
        List<Object[]> sizes = quotationRepo.sizeDistribution(partnerId, ownerId);
        long small = 0, medium = 0, large = 0;
        for (Object[] r : sizes) {
            String label = (String) r[0];
            long count = ((Number) r[1]).longValue();
            if (label != null && (label.contains("0-3") || label.contains("3-10"))) {
                medium += count;
            } else {
                large += count;
            }
        }
        d.setSmallSystems(small);
        d.setMediumSystems(medium);
        d.setLargeSystems(large);

        // Super Admin Specifics
        if (superAdmin) {
            d.setTotalPartners(partnerRepo.countAllPartners());
            d.setActivePartners(partnerRepo.countActivePartners());
            d.setPlatformPendingReferralPayouts(quotationRepo.sumPendingReferralPayouts(null, null));
            d.setTotalReferralIncome(quotationRepo.sumReferralIncome(partnerId, ownerId));

            List<Object[]> top = quotationRepo.topPartnersByRevenue(PageRequest.of(0, 5));
            List<Long> ids = new ArrayList<>();
            for (Object[] r : top) ids.add(((Number) r[0]).longValue());
            Map<Long, Object[]> cpMap = new HashMap<>();
            if (!ids.isEmpty()) {
                for (Object[] cp : partnerRepo.findBasicByIds(ids)) {
                    cpMap.put(((Number) cp[0]).longValue(), cp);
                }
            }
            List<DashboardResponseDto.TopPartner> list = new ArrayList<>();
            for (Object[] r : top) {
                Long pid = ((Number) r[0]).longValue();
                Object[] cp = cpMap.get(pid);
                DashboardResponseDto.TopPartner t = new DashboardResponseDto.TopPartner();
                t.setPartnerId(pid);
                if (cp != null) {
                    t.setPartnerName((String) cp[1]);
                    t.setPartnerCode((String) cp[2]);
                }
                t.setTotalQuotations(((Number) r[1]).longValue());
                t.setTotalRevenue((BigDecimal) r[2]);
                list.add(t);
            }
            d.setTopPartners(list);
        }

        // Recent Quotations (Yahan queryRepo ki jagah quotationRepo use kiya hai)
        List<Object[]> recentRows = quotationRepo.recent(partnerId, ownerId, PageRequest.of(0, 10));
        List<DashboardRecentQuotationResponseDto> recentList = new ArrayList<>();
        for (Object[] r : recentRows) {
            DashboardRecentQuotationResponseDto rd = new DashboardRecentQuotationResponseDto();
            rd.setCode((String) r[0]);
            rd.setCustomerName((String) r[1]);
            rd.setSystemSizeKw((BigDecimal) r[3]);
            rd.setTotalAmount((BigDecimal) r[4]);
            rd.setStatus(r[5] != null ? r[5].toString() : "PENDING");
            rd.setQuoteDate(toLocalDate(r[6]));
            recentList.add(rd);
        }
        d.setRecentQuotations(recentList);

        return d;
    }

    @Override
    public DashboardResponseDto getCharts(Long partnerIdFilter, LocalDate from, LocalDate to) {
        Long partnerId = tenantResolver.effectivePartnerId(partnerIdFilter);
        Long ownerId = tenantResolver.ownerFilter();

        DashboardResponseDto d = new DashboardResponseDto();

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (Object[] r : quotationRepo.countByStatusGrouped(partnerId, ownerId)) {
            statusCounts.put(String.valueOf(r[0]), ((Number) r[1]).longValue());
        }
        d.setStatusCounts(statusCounts);

        List<DashboardResponseDto.SizeBucket> sizes = new ArrayList<>();
        for (Object[] r : quotationRepo.sizeDistribution(partnerId, ownerId)) {
            sizes.add(new DashboardResponseDto.SizeBucket((String) r[0], ((Number) r[1]).longValue()));
        }
        d.setSizeDistribution(sizes);

        List<DashboardResponseDto.MonthlyPoint> trend = new ArrayList<>();
        for (Object[] r : quotationRepo.monthlyTrend(partnerId, ownerId, from, to)) {
            trend.add(new DashboardResponseDto.MonthlyPoint(
                    (String) r[0], ((Number) r[1]).longValue(), (BigDecimal) r[2]));
        }
        d.setMonthlyTrend(trend);
        return d;
    }

    // Helper method to convert Date/Timestamp to LocalDate
    private LocalDate toLocalDate(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate) return (LocalDate) o;
        if (o instanceof java.sql.Date) return ((java.sql.Date) o).toLocalDate();
        if (o instanceof java.sql.Timestamp) return ((java.sql.Timestamp) o).toLocalDateTime().toLocalDate();
        if (o instanceof java.util.Date) return new java.sql.Date(((java.util.Date) o).getTime()).toLocalDate();
        return null;
    }
}