package com.jichi.salesAgent.repository;

import com.jichi.salesAgent.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    List<SalesOrder> findByRepIdAndOrderDateBetween(Long repId, LocalDate start, LocalDate end);

    List<SalesOrder> findByRegionIdAndOrderDateBetween(Long regionId, LocalDate start, LocalDate end);

    List<SalesOrder> findByProductIdAndOrderDateBetween(Long productId, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM SalesOrder o " +
           "WHERE o.regionId = :regionId AND o.status = 'COMPLETED' " +
           "AND o.orderDate BETWEEN :start AND :end")
    BigDecimal sumAmountByRegion(@Param("regionId") Long regionId,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM SalesOrder o " +
           "WHERE o.repId = :repId AND o.status = 'COMPLETED' " +
           "AND o.orderDate BETWEEN :start AND :end")
    BigDecimal sumAmountByRep(@Param("repId") Long repId,
                               @Param("start") LocalDate start,
                               @Param("end") LocalDate end);

    @Query("SELECT o.repId, SUM(o.amount) AS total FROM SalesOrder o " +
           "WHERE o.status = 'COMPLETED' AND o.orderDate BETWEEN :start AND :end " +
           "GROUP BY o.repId ORDER BY total DESC")
    List<Object[]> findRepRanking(@Param("start") LocalDate start,
                                   @Param("end") LocalDate end);

    @Query("SELECT o.regionId, SUM(o.amount) AS total FROM SalesOrder o " +
           "WHERE o.status = 'COMPLETED' AND o.orderDate BETWEEN :start AND :end " +
           "GROUP BY o.regionId ORDER BY total DESC")
    List<Object[]> findRegionRanking(@Param("start") LocalDate start,
                                      @Param("end") LocalDate end);

    @Query("SELECT o.productId, SUM(o.amount) AS total, SUM(o.quantity) AS qty " +
           "FROM SalesOrder o WHERE o.status = 'COMPLETED' " +
           "AND o.orderDate BETWEEN :start AND :end " +
           "GROUP BY o.productId ORDER BY total DESC")
    List<Object[]> findProductRanking(@Param("start") LocalDate start,
                                       @Param("end") LocalDate end);

    @Query(value = "SELECT DATE_FORMAT(order_date, '%Y-%m') AS month, " +
                   "SUM(amount) AS total, COUNT(*) AS order_count " +
                   "FROM sa_sales_order WHERE status = 'COMPLETED' " +
                   "AND (:regionId IS NULL OR region_id = :regionId) " +
                   "AND order_date BETWEEN :start AND :end " +
                   "GROUP BY month ORDER BY month",
           nativeQuery = true)
    List<Object[]> findMonthlyTrend(@Param("regionId") Long regionId,
                                     @Param("start") LocalDate start,
                                     @Param("end") LocalDate end);

    @Query("SELECT MAX(o.orderDate) FROM SalesOrder o " +
           "WHERE o.productId = :productId AND o.status = 'COMPLETED'")
    LocalDate findLastOrderDateByProduct(@Param("productId") Long productId);

    @Query("SELECT o.repId, " +
           "SUM(CASE WHEN o.status = 'REFUNDED' THEN 1 ELSE 0 END) AS refunded, " +
           "COUNT(o) AS total " +
           "FROM SalesOrder o WHERE o.orderDate BETWEEN :start AND :end " +
           "GROUP BY o.repId")
    List<Object[]> findRefundRateByRep(@Param("start") LocalDate start,
                                        @Param("end") LocalDate end);

    @Query("SELECT COUNT(o) FROM SalesOrder o " +
           "WHERE o.regionId = :regionId AND o.status = 'COMPLETED' " +
           "AND o.orderDate BETWEEN :start AND :end")
    Long countCompletedByRegion(@Param("regionId") Long regionId,
                                 @Param("start") LocalDate start,
                                 @Param("end") LocalDate end);


    // 按大区过滤的退单率统计
    @Query("SELECT o.repId, " +
            "SUM(CASE WHEN o.status = 'REFUNDED' THEN 1 ELSE 0 END) AS refunded, " +
            "COUNT(*) AS total " +
            "FROM SalesOrder o " +
            "WHERE o.orderDate BETWEEN :start AND :end AND o.regionId = :regionId " +
            "GROUP BY o.repId")
    List<Object[]> findRefundRateByRepAndRegion(@Param("start") LocalDate start,
                                                @Param("end") LocalDate end,
                                                @Param("regionId") Long regionId);

    // 按大区过滤的产品最后出单日期
    @Query("SELECT MAX(o.orderDate) FROM SalesOrder o " +
            "WHERE o.productId = :productId AND o.status = 'COMPLETED' " +
            "AND o.regionId = :regionId")
    LocalDate findLastOrderDateByProductAndRegion(@Param("productId") Long productId,
                                                  @Param("regionId") Long regionId);

    // 单个销售员的退单数
    @Query("SELECT COUNT(o) FROM SalesOrder o " +
            "WHERE o.repId = :repId AND o.status = 'REFUNDED' " +
            "AND o.orderDate BETWEEN :start AND :end")
    long countRefundedByRep(@Param("repId") Long repId,
                            @Param("start") LocalDate start,
                            @Param("end") LocalDate end);

    // 单个销售员的订单总数
    @Query("SELECT COUNT(o) FROM SalesOrder o " +
            "WHERE o.repId = :repId " +
            "AND o.orderDate BETWEEN :start AND :end")
    long countByRepId(@Param("repId") Long repId,
                      @Param("start") LocalDate start,
                      @Param("end") LocalDate end);
}
