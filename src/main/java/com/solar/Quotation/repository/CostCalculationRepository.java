package com.solar.Quotation.repository;

import com.solar.Quotation.Enum.ProjectType;
import com.solar.Quotation.entity.CostCalculation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CostCalculationRepository extends JpaRepository<CostCalculation, Long> {

    @Query("select c from CostCalculation c where c.id = :id and c.deleted = false "
            + "and (:partnerId is null or c.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or c.createdBy = :ownerId)")
    Optional<CostCalculation> findOne(@Param("id") Long id,
                                      @Param("partnerId") Long partnerId,
                                      @Param("ownerId") Long ownerId);

    @Query("select c from CostCalculation c where c.deleted = false "
            + "and (:partnerId is null or c.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or c.createdBy = :ownerId) "
            + "and (:projectType is null or c.projectType = :projectType) "
            + "and (:search is null "
            + "     or lower(c.customerName) like lower(concat('%', :search, '%')) "
            + "     or lower(c.projectName) like lower(concat('%', :search, '%')) "
            + "     or lower(c.code) like lower(concat('%', :search, '%')))")
    Page<CostCalculation> search(@Param("partnerId") Long partnerId,
                                 @Param("ownerId") Long ownerId,
                                 @Param("projectType") ProjectType projectType,
                                 @Param("search") String search,
                                 Pageable pageable);
}