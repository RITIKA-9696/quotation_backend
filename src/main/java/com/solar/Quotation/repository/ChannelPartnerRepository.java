package com.solar.Quotation.repository;

import com.solar.Quotation.Enum.PartnerType;
import com.solar.Quotation.Enum.RecordStatus;
import com.solar.Quotation.entity.ChannelPartner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChannelPartnerRepository extends JpaRepository<ChannelPartner, Long> {

    Optional<ChannelPartner> findByIdAndDeletedFalse(Long id);

    /** Partner list for the Channel Partner screen (PLATFORM row is never listed). */
    @Query("select p from ChannelPartner p where p.deleted = false and p.partnerType = :type "
            + "and (:status is null or p.status = :status) "
            + "and (:search is null or lower(p.name) like lower(concat('%', :search, '%')) "
            + "     or lower(p.code) like lower(concat('%', :search, '%')) "
            + "     or lower(p.contactPerson) like lower(concat('%', :search, '%')) "
            + "     or lower(p.email) like lower(concat('%', :search, '%')))")
    Page<ChannelPartner> search(@Param("type") PartnerType type,
                                @Param("status") RecordStatus status,
                                @Param("search") String search,
                                Pageable pageable);

    @Query("select p from ChannelPartner p where p.deleted = false and p.partnerType = :type and p.status = :status "
            + "order by p.name asc")
    java.util.List<ChannelPartner> findOptions(@Param("type") PartnerType type, @Param("status") RecordStatus status);

    @Query("select count(cp) from ChannelPartner cp where cp.deleted = false")
    long countAllPartners();

    @Query("select count(cp) from ChannelPartner cp where cp.deleted = false and cp.status = com.solar.Quotation.Enum.RecordStatus.ACTIVE")
    long countActivePartners();

    @Query("select cp.id, cp.name, cp.code from ChannelPartner cp where cp.deleted = false and cp.id in :ids")
    java.util.List<Object[]> findBasicByIds(@Param("ids") java.util.List<Long> ids);
}