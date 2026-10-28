package com.solar.Quotation.repository;


import com.solar.Quotation.Enum.CustomerType;
import com.solar.Quotation.Enum.RecordStatus;
import com.solar.Quotation.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("select c from Customer c where c.id = :id and c.deleted = false "
            + "and (:partnerId is null or c.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or c.createdBy = :ownerId)")
    Optional<Customer> findOne(@Param("id") Long id,
                               @Param("partnerId") Long partnerId,
                               @Param("ownerId") Long ownerId);

    @Query("select c from Customer c where c.deleted = false "
            + "and (:partnerId is null or c.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or c.createdBy = :ownerId) "
            + "and (:type is null or c.type = :type) "
            + "and (:status is null or c.status = :status) "
            + "and (:search is null or lower(c.name) like lower(concat('%', :search, '%')) "
            + "     or c.mobile like concat('%', :search, '%') "
            + "     or lower(c.email) like lower(concat('%', :search, '%')) "
            + "     or lower(c.code) like lower(concat('%', :search, '%')) "
            + "     or lower(c.city) like lower(concat('%', :search, '%')))")
    Page<Customer> search(@Param("partnerId") Long partnerId,
                          @Param("ownerId") Long ownerId,
                          @Param("type") CustomerType type,
                          @Param("status") RecordStatus status,
                          @Param("search") String search,
                          Pageable pageable);

    @Query("select count(c) from Customer c where c.deleted = false "
            + "and (:partnerId is null or c.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or c.createdBy = :ownerId) "
            + "and (:type is null or c.type = :type) "
            + "and (:status is null or c.status = :status)")
    long countFiltered(@Param("partnerId") Long partnerId,
                       @Param("ownerId") Long ownerId,
                       @Param("type") CustomerType type,
                       @Param("status") RecordStatus status);

    boolean existsByChannelPartnerIdAndMobileAndDeletedFalse(Long channelPartnerId, String mobile);

    boolean existsByChannelPartnerIdAndMobileAndDeletedFalseAndIdNot(Long channelPartnerId, String mobile, Long id);

    @Query("select count(c) from Customer c where c.deleted = false "
            + "and (:partnerId is null or c.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or c.createdBy = :ownerId)")
    long countScoped(@Param("partnerId") Long partnerId, @Param("ownerId") Long ownerId);
}
