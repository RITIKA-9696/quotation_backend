package com.solar.Quotation.service.serviceImpl;

import com.solar.Quotation.dto.responsedto.DashboardResponseDto;
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
        d.setTotalCustomers(customerRepo.countScoped(partnerId, ownerId));
        d.setTotalProjects(projectRepo.countScoped(partnerId, ownerId));
        d.setTotalQuotations(quotationRepo.countFiltered(partnerId, ownerId, null));
        d.setTotalQuotationValue(quotationRepo.sumTotal(partnerId, ownerId, null));
        d.setQuotationValueThisMonth(
                quotationRepo.sumTotalBetween(partnerId, ownerId, monthStart, nextMonth));
        d.setPendingApprovals(quotationRepo.countFiltered(
                partnerId, ownerId, com.solar.Quotation.Enum.QuotationStatus.PENDING));
        d.setTotalReferralIncome(quotationRepo.sumReferralIncome(partnerId, ownerId));

        if (superAdmin) {
            d.setTotalPartners(partnerRepo.countAllPartners());
            d.setActivePartners(partnerRepo.countActivePartners());
            d.setPlatformPendingReferralPayouts(quotationRepo.sumPendingReferralPayouts(null, null));

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
}