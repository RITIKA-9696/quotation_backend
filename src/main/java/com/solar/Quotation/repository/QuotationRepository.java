package com.solar.Quotation.repository;

import com.solar.Quotation.Enum.QuotationStatus;
import com.solar.Quotation.entity.Quotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    // ============================ EXISTING ============================
    @Query("select q from Quotation q where q.id = :id and q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId)")
    Optional<Quotation> findOne(@Param("id") Long id,
                                @Param("partnerId") Long partnerId,
                                @Param("ownerId") Long ownerId);

    @Query("select q from Quotation q where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and (:status is null or q.status = :status)")
    Page<Quotation> search(@Param("partnerId") Long partnerId,
                           @Param("ownerId") Long ownerId,
                           @Param("status") QuotationStatus status,
                           @Param("customerId") Long customerId,
                           @Param("referred") Boolean referred,
                           @Param("fromDate") LocalDate fromDate,
                           @Param("toDate") LocalDate toDate,
                           @Param("search") String search,
                           Pageable pageable);

    @Query("select count(q) from Quotation q where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and (:status is null or q.status = :status)")
    long countFiltered(@Param("partnerId") Long partnerId,
                       @Param("ownerId") Long ownerId,
                       @Param("status") QuotationStatus status);

    @Query("select coalesce(sum(q.totalAmount), 0) from Quotation q where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and (:status is null or q.status = :status)")
    BigDecimal sumTotal(@Param("partnerId") Long partnerId,
                        @Param("ownerId") Long ownerId,
                        @Param("status") QuotationStatus status);

    long countByCustomerIdAndDeletedFalse(Long customerId);

    // ============ DASHBOARD ============

    @Query("select coalesce(sum(q.totalAmount), 0) from Quotation q where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and q.quoteDate >= :start and q.quoteDate < :end")
    BigDecimal sumTotalBetween(@Param("partnerId") Long partnerId,
                               @Param("ownerId") Long ownerId,
                               @Param("start") LocalDate start,
                               @Param("end") LocalDate end);

    @Query("select count(q) from Quotation q where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and q.quoteDate >= :start and q.quoteDate <= :end")
    long countBetween(@Param("partnerId") Long partnerId,
                      @Param("ownerId") Long ownerId,
                      @Param("start") LocalDate start,
                      @Param("end") LocalDate end);

    @Query("select q.status, count(q) from Quotation q where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "group by q.status")
    List<Object[]> countByStatusGrouped(@Param("partnerId") Long partnerId,
                                        @Param("ownerId") Long ownerId);

    @Query("select case when q.systemSizeKw < 3 then '0-3 kw' "
            + "            when q.systemSizeKw < 10 then '3-10 kw' "
            + "            else '10+ kw' end, count(q) "
            + "from Quotation q where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "group by case when q.systemSizeKw < 3 then '0-3 kw' "
            + "              when q.systemSizeKw < 10 then '3-10 kw' "
            + "              else '10+ kw' end")
    List<Object[]> sizeDistribution(@Param("partnerId") Long partnerId,
                                    @Param("ownerId") Long ownerId);

    @Query("select function('date_format', q.quoteDate, '%Y-%m'), count(q), coalesce(sum(q.totalAmount), 0) "
            + "from Quotation q where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and (:fromDate is null or q.quoteDate >= :fromDate) "
            + "and (:toDate is null or q.quoteDate <= :toDate) "
            + "group by function('date_format', q.quoteDate, '%Y-%m') "
            + "order by 1")
    List<Object[]> monthlyTrend(@Param("partnerId") Long partnerId,
                                @Param("ownerId") Long ownerId,
                                @Param("fromDate") LocalDate fromDate,
                                @Param("toDate") LocalDate toDate);

    @Query("select q.channelPartnerId, count(q), coalesce(sum(q.totalAmount), 0) "
            + "from Quotation q where q.deleted = false and q.channelPartnerId is not null "
            + "group by q.channelPartnerId order by sum(q.totalAmount) desc")
    List<Object[]> topPartnersByRevenue(Pageable pageable);

    @Query("select coalesce(sum(q.referralAmount), 0) from Quotation q where q.deleted = false "
            + "and q.referralAmount is not null "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId)")
    BigDecimal sumReferralIncome(@Param("partnerId") Long partnerId,
                                 @Param("ownerId") Long ownerId);

    @Query("select coalesce(sum(q.referralAmount), 0) from Quotation q where q.deleted = false "
            + "and q.status = com.solar.Quotation.Enum.QuotationStatus.APPROVED "
            + "and q.referralAmount is not null and q.referralPaidDate is null "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId)")
    BigDecimal sumPendingReferralPayouts(@Param("partnerId") Long partnerId,
                                         @Param("ownerId") Long ownerId);

    @Query("select q.code, c.name, q.channelPartnerId, q.systemSizeKw, q.totalAmount, q.status, q.quoteDate "
            + "from Quotation q, Customer c "
            + "where q.customerId = c.id and q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "order by q.createdAt desc, q.id desc")
    List<Object[]> recent(@Param("partnerId") Long partnerId,
                          @Param("ownerId") Long ownerId,
                          Pageable pageable);

    // ============ REPORTS ============

    @Query("select count(q), "
            + "  sum(case when q.status = com.solar.Quotation.Enum.QuotationStatus.ACCEPTED then 1 else 0 end), "
            + "  sum(case when q.status = com.solar.Quotation.Enum.QuotationStatus.PENDING  then 1 else 0 end), "
            + "  sum(case when q.status = com.solar.Quotation.Enum.QuotationStatus.REJECTED then 1 else 0 end), "
            + "  coalesce(sum(q.totalAmount), 0), "
            + "  coalesce(sum(q.gstAmount), 0), "
            + "  coalesce(sum(q.referralAmount), 0) "
            + "from Quotation q where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and (:status is null or q.status = :status) "
            + "and (:salesUserId is null or q.createdBy = :salesUserId) "
            + "and (:fromDate is null or q.quoteDate >= :fromDate) "
            + "and (:toDate is null or q.quoteDate <= :toDate)")
    Object[] reportSummary(@Param("partnerId") Long partnerId,
                           @Param("ownerId") Long ownerId,
                           @Param("status") com.solar.Quotation.Enum.QuotationStatus status,
                           @Param("salesUserId") Long salesUserId,
                           @Param("fromDate") LocalDate fromDate,
                           @Param("toDate") LocalDate toDate);

    @Query("select q.code, c.name, q.systemSizeKw, q.totalAmount, "
            + "       q.referredBy, q.status, q.quoteDate, cp.name "
            + "from Quotation q "
            + "left join Customer c on c.id = q.customerId "
            + "left join ChannelPartner cp on cp.id = q.channelPartnerId "
            + "where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and (:status is null or q.status = :status) "
            + "and (:salesUserId is null or q.createdBy = :salesUserId) "
            + "and (:fromDate is null or q.quoteDate >= :fromDate) "
            + "and (:toDate is null or q.quoteDate <= :toDate)")
    Page<Object[]> quotationReport(@Param("partnerId") Long partnerId,
                                   @Param("ownerId") Long ownerId,
                                   @Param("status") com.solar.Quotation.Enum.QuotationStatus status,
                                   @Param("salesUserId") Long salesUserId,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate,
                                   Pageable pageable);

    @Query("select q.code, c.name, q.taxableAmount, q.gstPercent, "
            + "       q.cgstAmount, q.sgstAmount, q.gstAmount, q.totalAmount, q.quoteDate "
            + "from Quotation q "
            + "left join Customer c on c.id = q.customerId "
            + "where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and (:status is null or q.status = :status) "
            + "and (:salesUserId is null or q.createdBy = :salesUserId) "
            + "and (:fromDate is null or q.quoteDate >= :fromDate) "
            + "and (:toDate is null or q.quoteDate <= :toDate)")
    Page<Object[]> gstReport(@Param("partnerId") Long partnerId,
                             @Param("ownerId") Long ownerId,
                             @Param("status") com.solar.Quotation.Enum.QuotationStatus status,
                             @Param("salesUserId") Long salesUserId,
                             @Param("fromDate") LocalDate fromDate,
                             @Param("toDate") LocalDate toDate,
                             Pageable pageable);

    @Query("select q.referredBy, c.name, q.totalAmount, "
            + "       q.referralPercent, q.referralAmount, q.status, cp.name "
            + "from Quotation q "
            + "left join Customer c on c.id = q.customerId "
            + "left join ChannelPartner cp on cp.id = q.channelPartnerId "
            + "where q.deleted = false "
            + "and q.referralAmount is not null "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and (:status is null or q.status = :status) "
            + "and (:salesUserId is null or q.createdBy = :salesUserId) "
            + "and (:fromDate is null or q.quoteDate >= :fromDate) "
            + "and (:toDate is null or q.quoteDate <= :toDate)")
    Page<Object[]> referralIncomeReport(@Param("partnerId") Long partnerId,
                                        @Param("ownerId") Long ownerId,
                                        @Param("status") com.solar.Quotation.Enum.QuotationStatus status,
                                        @Param("salesUserId") Long salesUserId,
                                        @Param("fromDate") LocalDate fromDate,
                                        @Param("toDate") LocalDate toDate,
                                        Pageable pageable);

    // YAHAN USER KI JAGAH APP_USER USE KIYA HAI
    @Query("select q.createdBy, u.name, count(q), "
            + "       sum(case when q.status = com.solar.Quotation.Enum.QuotationStatus.ACCEPTED then 1 else 0 end) "
            + "from Quotation q "
            + "left join AppUser u on u.id = q.createdBy "
            + "where q.deleted = false "
            + "and (:partnerId is null or q.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or q.createdBy = :ownerId) "
            + "and (:fromDate is null or q.quoteDate >= :fromDate) "
            + "and (:toDate is null or q.quoteDate <= :toDate) "
            + "group by q.createdBy, u.name order by count(q) desc")
    Page<Object[]> topPerformers(@Param("partnerId") Long partnerId,
                                 @Param("ownerId") Long ownerId,
                                 @Param("fromDate") LocalDate fromDate,
                                 @Param("toDate") LocalDate toDate,
                                 Pageable pageable);

    // ============ REFERRAL ACTIVITY ============

    @Query("select q.id, q.code, c.name, q.totalAmount, "
            + "       q.referralPercent, q.referralAmount, q.status, q.quoteDate, q.referralPaidDate "
            + "from Quotation q "
            + "left join Customer c on c.id = q.customerId "
            + "where q.deleted = false "
            + "and q.channelPartnerId = :partnerId "
            + "and q.referralAmount is not null "
            + "and (:status is null or q.status = :status)")
    Page<Object[]> referralActivity(@Param("partnerId") Long partnerId,
                                    @Param("status") com.solar.Quotation.Enum.QuotationStatus status,
                                    Pageable pageable);
}