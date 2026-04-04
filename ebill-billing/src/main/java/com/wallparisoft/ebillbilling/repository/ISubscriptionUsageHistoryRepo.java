package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.SubscriptionUsageHistory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ISubscriptionUsageHistoryRepo
    extends JpaRepository<SubscriptionUsageHistory, Long> {

  Optional<SubscriptionUsageHistory> findByIdCompanyAndMonthYear(Long idCompany, String monthYear);

  @Query(
      "SELECT suh FROM SubscriptionUsageHistory suh "
          + "WHERE suh.idCompany = :idCompany "
          + "ORDER BY suh.monthYear DESC "
          + "LIMIT :limit")
  List<SubscriptionUsageHistory> findRecentByIdCompany(
      @Param("idCompany") Long idCompany, @Param("limit") int limit);

  @Query(
      "SELECT COALESCE(SUM(suh.totalDocuments), 0) "
          + "FROM SubscriptionUsageHistory suh "
          + "WHERE suh.idCompany = :idCompany "
          + "AND suh.monthYear >= :startMonth "
          + "AND suh.monthYear <= :endMonth")
  Integer sumTotalDocumentsByCompanyAndDateRange(
      @Param("idCompany") Long idCompany,
      @Param("startMonth") String startMonth,
      @Param("endMonth") String endMonth);
}
