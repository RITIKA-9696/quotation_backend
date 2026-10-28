package com.solar.Quotation.repository;

import com.solar.Quotation.Enum.CustomerType;
import com.solar.Quotation.Enum.ProjectStatus;
import com.solar.Quotation.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("select p from Project p where p.id = :id and p.deleted = false "
            + "and (:partnerId is null or p.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or p.createdBy = :ownerId)")
    Optional<Project> findOne(@Param("id") Long id,
                              @Param("partnerId") Long partnerId,
                              @Param("ownerId") Long ownerId);

    @Query("select p from Project p where p.deleted = false "
            + "and (:partnerId is null or p.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or p.createdBy = :ownerId) "
            + "and (:type is null or p.type = :type) "
            + "and (:status is null or p.status = :status) "
            + "and (:customerId is null or p.customerId = :customerId) "
            + "and (:search is null or lower(p.name) like lower(concat('%', :search, '%')) "
            + "     or lower(p.code) like lower(concat('%', :search, '%')) "
            + "     or lower(p.location) like lower(concat('%', :search, '%')) "
            + "     or p.customerId in (select c.id from Customer c "
            + "                         where lower(c.name) like lower(concat('%', :search, '%'))))")
    Page<Project> search(@Param("partnerId") Long partnerId,
                         @Param("ownerId") Long ownerId,
                         @Param("type") CustomerType type,
                         @Param("status") ProjectStatus status,
                         @Param("customerId") Long customerId,
                         @Param("search") String search,
                         Pageable pageable);

    @Query("select count(p) from Project p where p.deleted = false "
            + "and (:partnerId is null or p.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or p.createdBy = :ownerId) "
            + "and (:type is null or p.type = :type) "
            + "and (:status is null or p.status = :status)")
    long countFiltered(@Param("partnerId") Long partnerId,
                       @Param("ownerId") Long ownerId,
                       @Param("type") CustomerType type,
                       @Param("status") ProjectStatus status);

    long countByCustomerIdAndDeletedFalse(Long customerId);

    /** Rows of [customerId, count] for a page of customers. */
    @Query("select p.customerId, count(p) from Project p where p.deleted = false "
            + "and p.customerId in :customerIds group by p.customerId")
    List<Object[]> countGroupedByCustomer(@Param("customerIds") Collection<Long> customerIds);

    /** Newest first; the caller keeps the first project per customer. */
    @Query("select p from Project p where p.deleted = false and p.customerId in :customerIds "
            + "order by p.createdAt desc")
    java.util.List<Project> findNewestForCustomers(@Param("customerIds") java.util.Collection<Long> customerIds);

    @Query("select count(p) from Project p where p.deleted = false "
            + "and (:partnerId is null or p.channelPartnerId = :partnerId) "
            + "and (:ownerId is null or p.createdBy = :ownerId)")
    long countScoped(@Param("partnerId") Long partnerId,
                     @Param("ownerId") Long ownerId);
}
