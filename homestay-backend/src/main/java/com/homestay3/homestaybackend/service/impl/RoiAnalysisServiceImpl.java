package com.homestay3.homestaybackend.service.impl;

import com.homestay3.homestaybackend.dto.RoiCampaignDTO;
import com.homestay3.homestaybackend.dto.RoiOverviewDTO;
import com.homestay3.homestaybackend.service.RoiAnalysisService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
public class RoiAnalysisServiceImpl implements RoiAnalysisService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public RoiOverviewDTO getPlatformRoiOverview(LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("  COALESCE(SUM(ops.payable_amount), 0) as total_gmv, ");
        sql.append("  COALESCE(SUM(pu.discount_amount), 0) as total_discount_cost, ");
        sql.append("  COUNT(DISTINCT pu.order_id) as total_orders, ");
        sql.append("  COUNT(*) as total_usage_count, ");
        sql.append("  SUM(CASE WHEN pu.status = 'USED' THEN 1 ELSE 0 END) as used_count, ");
        sql.append("  COALESCE(SUM(CASE WHEN pu.bearer = 'PLATFORM' THEN pu.discount_amount ELSE 0 END), 0) as platform_cost, ");
        sql.append("  COALESCE(SUM(CASE WHEN pu.bearer = 'HOST' THEN pu.discount_amount ELSE 0 END), 0) as host_cost ");
        sql.append("FROM promotion_usage pu ");
        sql.append("LEFT JOIN order_price_snapshot ops ON pu.order_id = ops.order_id ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (startDate != null) {
            sql.append("AND pu.created_at >= ? ");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append("AND pu.created_at <= ? ");
            params.add(endDate);
        }

        // GMV 需要通过 distinct order_id 子查询来计算，避免同一订单多个优惠重复计算
        String gmvSql = "SELECT COALESCE(SUM(ops.payable_amount), 0) " +
                "FROM (SELECT DISTINCT order_id FROM promotion_usage WHERE 1=1 ";
        List<Object> gmvParams = new ArrayList<>();
        if (startDate != null) {
            gmvSql += "AND created_at >= ? ";
            gmvParams.add(startDate);
        }
        if (endDate != null) {
            gmvSql += "AND created_at <= ? ";
            gmvParams.add(endDate);
        }
        gmvSql += ") distinct_orders " +
                "JOIN order_price_snapshot ops ON distinct_orders.order_id = ops.order_id";

        Query gmvQuery = entityManager.createNativeQuery(gmvSql);
        for (int i = 0; i < gmvParams.size(); i++) {
            gmvQuery.setParameter(i + 1, gmvParams.get(i));
        }
        BigDecimal totalGmv = (BigDecimal) gmvQuery.getSingleResult();

        Query query = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        Object[] row = (Object[]) query.getSingleResult();
        BigDecimal totalDiscountCost = toBigDecimal(row[1]);
        Number totalOrders = (Number) row[2];
        Number totalUsageCount = (Number) row[3];
        Number usedCount = (Number) row[4];
        BigDecimal platformCost = toBigDecimal(row[5]);
        BigDecimal hostCost = toBigDecimal(row[6]);

        return buildOverview(totalGmv, totalDiscountCost, totalOrders, totalUsageCount, usedCount, platformCost, hostCost);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RoiCampaignDTO> getPlatformCampaignRoi(LocalDateTime startDate, LocalDateTime endDate, int limit) {
        // 构建 GMV 子查询（与主查询时间条件一致）
        StringBuilder gmvSubSql = new StringBuilder();
        gmvSubSql.append("SELECT campaign_id, COALESCE(SUM(ops.payable_amount), 0) as gmv ");
        gmvSubSql.append("FROM (SELECT DISTINCT campaign_id, order_id FROM promotion_usage WHERE campaign_id IS NOT NULL ");
        List<Object> gmvSubParams = new ArrayList<>();
        if (startDate != null) {
            gmvSubSql.append("AND created_at >= ? ");
            gmvSubParams.add(startDate);
        }
        if (endDate != null) {
            gmvSubSql.append("AND created_at <= ? ");
            gmvSubParams.add(endDate);
        }
        gmvSubSql.append(") distinct_pu ");
        gmvSubSql.append("JOIN order_price_snapshot ops ON distinct_pu.order_id = ops.order_id ");
        gmvSubSql.append("GROUP BY campaign_id");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("  pc.id as campaign_id, ");
        sql.append("  pc.name as campaign_name, ");
        sql.append("  pc.campaign_type, ");
        sql.append("  pc.subsidy_bearer, ");
        sql.append("  COALESCE(SUM(pu.discount_amount), 0) as discount_cost, ");
        sql.append("  COUNT(DISTINCT pu.order_id) as order_count, ");
        sql.append("  COUNT(*) as usage_count, ");
        sql.append("  SUM(CASE WHEN pu.status = 'USED' THEN 1 ELSE 0 END) as used_count, ");
        sql.append("  pc.budget_total, ");
        sql.append("  pc.budget_used, ");
        sql.append("  COALESCE(gmv.gmv, 0) as gmv ");
        sql.append("FROM promotion_usage pu ");
        sql.append("JOIN promotion_campaign pc ON pu.campaign_id = pc.id ");
        sql.append("LEFT JOIN (").append(gmvSubSql).append(") gmv ON gmv.campaign_id = pc.id ");
        sql.append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        params.addAll(gmvSubParams);
        if (startDate != null) {
            sql.append("AND pu.created_at >= ? ");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append("AND pu.created_at <= ? ");
            params.add(endDate);
        }
        sql.append("GROUP BY pc.id, pc.name, pc.campaign_type, pc.subsidy_bearer, pc.budget_total, pc.budget_used, gmv.gmv ");
        sql.append("ORDER BY discount_cost DESC ");
        sql.append("LIMIT ?");
        params.add(limit);

        Query query = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        List<Object[]> rows = query.getResultList();
        List<RoiCampaignDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            Long campaignId = ((Number) row[0]).longValue();
            String campaignName = (String) row[1];
            String campaignType = (String) row[2];
            String subsidyBearer = (String) row[3];
            BigDecimal discountCost = toBigDecimal(row[4]);
            Number orderCount = (Number) row[5];
            Number usageCount = (Number) row[6];
            Number usedCount = (Number) row[7];
            BigDecimal budgetTotal = toBigDecimal(row[8]);
            BigDecimal budgetUsed = toBigDecimal(row[9]);
            BigDecimal gmv = toBigDecimal(row[10]);

            BigDecimal roi = calculateRoi(gmv, discountCost);
            BigDecimal usageRate = calculateRate(usedCount, usageCount);
            BigDecimal budgetUsageRate = calculateRate(budgetUsed, budgetTotal);

            result.add(RoiCampaignDTO.builder()
                    .campaignId(campaignId)
                    .campaignName(campaignName)
                    .campaignType(campaignType)
                    .subsidyBearer(subsidyBearer)
                    .gmv(gmv)
                    .discountCost(discountCost)
                    .orderCount(orderCount != null ? orderCount.longValue() : 0L)
                    .usageCount(usageCount != null ? usageCount.longValue() : 0L)
                    .usedCount(usedCount != null ? usedCount.longValue() : 0L)
                    .roi(roi)
                    .usageRate(usageRate)
                    .budgetUsageRate(budgetUsageRate)
                    .budgetTotal(budgetTotal)
                    .budgetUsed(budgetUsed)
                    .build());
        }

        return result;
    }

    @Override
    public RoiOverviewDTO getHostRoiOverview(Long hostId, LocalDateTime startDate, LocalDateTime endDate) {
        // 查询该房东的所有活动ID
        String campaignIdsSql = "SELECT id FROM promotion_campaign WHERE host_id = ?";
        Query campaignQuery = entityManager.createNativeQuery(campaignIdsSql);
        campaignQuery.setParameter(1, hostId);
        @SuppressWarnings("unchecked")
        List<Number> campaignIds = campaignQuery.getResultList();

        if (campaignIds.isEmpty()) {
            return emptyOverview();
        }

        // GMV 子查询（限定该房东的活动）
        String gmvSql = "SELECT COALESCE(SUM(ops.payable_amount), 0) " +
                "FROM (SELECT DISTINCT pu.order_id FROM promotion_usage pu " +
                "WHERE pu.campaign_id IN (SELECT id FROM promotion_campaign WHERE host_id = ?) ";
        List<Object> gmvParams = new ArrayList<>();
        gmvParams.add(hostId);
        if (startDate != null) {
            gmvSql += "AND pu.created_at >= ? ";
            gmvParams.add(startDate);
        }
        if (endDate != null) {
            gmvSql += "AND pu.created_at <= ? ";
            gmvParams.add(endDate);
        }
        gmvSql += ") distinct_orders " +
                "JOIN order_price_snapshot ops ON distinct_orders.order_id = ops.order_id";

        Query gmvQuery = entityManager.createNativeQuery(gmvSql);
        for (int i = 0; i < gmvParams.size(); i++) {
            gmvQuery.setParameter(i + 1, gmvParams.get(i));
        }
        BigDecimal totalGmv = (BigDecimal) gmvQuery.getSingleResult();

        // 总成本统计
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("  COALESCE(SUM(pu.discount_amount), 0) as total_discount_cost, ");
        sql.append("  COUNT(DISTINCT pu.order_id) as total_orders, ");
        sql.append("  COUNT(*) as total_usage_count, ");
        sql.append("  SUM(CASE WHEN pu.status = 'USED' THEN 1 ELSE 0 END) as used_count, ");
        sql.append("  COALESCE(SUM(CASE WHEN pu.bearer = 'PLATFORM' THEN pu.discount_amount ELSE 0 END), 0) as platform_cost, ");
        sql.append("  COALESCE(SUM(CASE WHEN pu.bearer = 'HOST' THEN pu.discount_amount ELSE 0 END), 0) as host_cost ");
        sql.append("FROM promotion_usage pu ");
        sql.append("WHERE pu.campaign_id IN (SELECT id FROM promotion_campaign WHERE host_id = ?) ");

        List<Object> params = new ArrayList<>();
        params.add(hostId);
        if (startDate != null) {
            sql.append("AND pu.created_at >= ? ");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append("AND pu.created_at <= ? ");
            params.add(endDate);
        }

        Query query = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        Object[] row = (Object[]) query.getSingleResult();
        BigDecimal totalDiscountCost = toBigDecimal(row[0]);
        Number totalOrders = (Number) row[1];
        Number totalUsageCount = (Number) row[2];
        Number usedCount = (Number) row[3];
        BigDecimal platformCost = toBigDecimal(row[4]);
        BigDecimal hostCost = toBigDecimal(row[5]);

        return buildOverview(totalGmv, totalDiscountCost, totalOrders, totalUsageCount, usedCount, platformCost, hostCost);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RoiCampaignDTO> getHostCampaignRoi(Long hostId, LocalDateTime startDate, LocalDateTime endDate) {
        // 构建 GMV 子查询（限定该房东的活动）
        StringBuilder gmvSubSql = new StringBuilder();
        gmvSubSql.append("SELECT pu.campaign_id, COALESCE(SUM(ops.payable_amount), 0) as gmv ");
        gmvSubSql.append("FROM (SELECT DISTINCT campaign_id, order_id FROM promotion_usage WHERE campaign_id IS NOT NULL ");
        List<Object> gmvSubParams = new ArrayList<>();
        if (startDate != null) {
            gmvSubSql.append("AND created_at >= ? ");
            gmvSubParams.add(startDate);
        }
        if (endDate != null) {
            gmvSubSql.append("AND created_at <= ? ");
            gmvSubParams.add(endDate);
        }
        gmvSubSql.append(") pu ");
        gmvSubSql.append("JOIN promotion_campaign pc ON pu.campaign_id = pc.id ");
        gmvSubSql.append("JOIN order_price_snapshot ops ON pu.order_id = ops.order_id ");
        gmvSubSql.append("WHERE pc.host_id = ? ");
        gmvSubParams.add(hostId);
        gmvSubSql.append("GROUP BY pu.campaign_id");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("  pc.id as campaign_id, ");
        sql.append("  pc.name as campaign_name, ");
        sql.append("  pc.campaign_type, ");
        sql.append("  pc.subsidy_bearer, ");
        sql.append("  COALESCE(SUM(pu.discount_amount), 0) as discount_cost, ");
        sql.append("  COUNT(DISTINCT pu.order_id) as order_count, ");
        sql.append("  COUNT(*) as usage_count, ");
        sql.append("  SUM(CASE WHEN pu.status = 'USED' THEN 1 ELSE 0 END) as used_count, ");
        sql.append("  pc.budget_total, ");
        sql.append("  pc.budget_used, ");
        sql.append("  COALESCE(gmv.gmv, 0) as gmv ");
        sql.append("FROM promotion_usage pu ");
        sql.append("JOIN promotion_campaign pc ON pu.campaign_id = pc.id ");
        sql.append("LEFT JOIN (").append(gmvSubSql).append(") gmv ON gmv.campaign_id = pc.id ");
        sql.append("WHERE pc.host_id = ? ");

        List<Object> params = new ArrayList<>();
        params.addAll(gmvSubParams);
        params.add(hostId);
        if (startDate != null) {
            sql.append("AND pu.created_at >= ? ");
            params.add(startDate);
        }
        if (endDate != null) {
            sql.append("AND pu.created_at <= ? ");
            params.add(endDate);
        }
        sql.append("GROUP BY pc.id, pc.name, pc.campaign_type, pc.subsidy_bearer, pc.budget_total, pc.budget_used, gmv.gmv ");
        sql.append("ORDER BY discount_cost DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }

        List<Object[]> rows = query.getResultList();
        List<RoiCampaignDTO> result = new ArrayList<>();

        for (Object[] row : rows) {
            Long campaignId = ((Number) row[0]).longValue();
            String campaignName = (String) row[1];
            String campaignType = (String) row[2];
            String subsidyBearer = (String) row[3];
            BigDecimal discountCost = toBigDecimal(row[4]);
            Number orderCount = (Number) row[5];
            Number usageCount = (Number) row[6];
            Number usedCount = (Number) row[7];
            BigDecimal budgetTotal = toBigDecimal(row[8]);
            BigDecimal budgetUsed = toBigDecimal(row[9]);
            BigDecimal gmv = toBigDecimal(row[10]);

            BigDecimal roi = calculateRoi(gmv, discountCost);
            BigDecimal usageRate = calculateRate(usedCount, usageCount);
            BigDecimal budgetUsageRate = calculateRate(budgetUsed, budgetTotal);

            result.add(RoiCampaignDTO.builder()
                    .campaignId(campaignId)
                    .campaignName(campaignName)
                    .campaignType(campaignType)
                    .subsidyBearer(subsidyBearer)
                    .gmv(gmv)
                    .discountCost(discountCost)
                    .orderCount(orderCount != null ? orderCount.longValue() : 0L)
                    .usageCount(usageCount != null ? usageCount.longValue() : 0L)
                    .usedCount(usedCount != null ? usedCount.longValue() : 0L)
                    .roi(roi)
                    .usageRate(usageRate)
                    .budgetUsageRate(budgetUsageRate)
                    .budgetTotal(budgetTotal)
                    .budgetUsed(budgetUsed)
                    .build());
        }

        return result;
    }

    private BigDecimal getCampaignGmv(Long campaignId, LocalDateTime startDate, LocalDateTime endDate) {
        String gmvSql = "SELECT COALESCE(SUM(ops.payable_amount), 0) " +
                "FROM (SELECT DISTINCT order_id FROM promotion_usage WHERE campaign_id = ? ";
        List<Object> gmvParams = new ArrayList<>();
        gmvParams.add(campaignId);
        if (startDate != null) {
            gmvSql += "AND created_at >= ? ";
            gmvParams.add(startDate);
        }
        if (endDate != null) {
            gmvSql += "AND created_at <= ? ";
            gmvParams.add(endDate);
        }
        gmvSql += ") distinct_orders " +
                "JOIN order_price_snapshot ops ON distinct_orders.order_id = ops.order_id";

        Query gmvQuery = entityManager.createNativeQuery(gmvSql);
        for (int i = 0; i < gmvParams.size(); i++) {
            gmvQuery.setParameter(i + 1, gmvParams.get(i));
        }
        return (BigDecimal) gmvQuery.getSingleResult();
    }

    private RoiOverviewDTO buildOverview(BigDecimal totalGmv, BigDecimal totalDiscountCost,
                                         Number totalOrders, Number totalUsageCount, Number usedCount,
                                         BigDecimal platformCost, BigDecimal hostCost) {
        BigDecimal roi = calculateRoi(totalGmv, totalDiscountCost);
        BigDecimal usageRate = calculateRate(usedCount, totalUsageCount);

        long orderCount = totalOrders != null ? totalOrders.longValue() : 0L;
        BigDecimal avgDiscountPerOrder = orderCount > 0
                ? totalDiscountCost.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal avgOrderValue = orderCount > 0
                ? totalGmv.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return RoiOverviewDTO.builder()
                .totalGmv(totalGmv)
                .totalDiscountCost(totalDiscountCost)
                .totalOrders(orderCount)
                .totalUsageCount(totalUsageCount != null ? totalUsageCount.longValue() : 0L)
                .usedCount(usedCount != null ? usedCount.longValue() : 0L)
                .platformCost(platformCost)
                .hostCost(hostCost)
                .roi(roi)
                .usageRate(usageRate)
                .avgDiscountPerOrder(avgDiscountPerOrder)
                .avgOrderValue(avgOrderValue)
                .build();
    }

    private RoiOverviewDTO emptyOverview() {
        return RoiOverviewDTO.builder()
                .totalGmv(BigDecimal.ZERO)
                .totalDiscountCost(BigDecimal.ZERO)
                .totalOrders(0L)
                .totalUsageCount(0L)
                .usedCount(0L)
                .platformCost(BigDecimal.ZERO)
                .hostCost(BigDecimal.ZERO)
                .roi(BigDecimal.ZERO)
                .usageRate(BigDecimal.ZERO)
                .avgDiscountPerOrder(BigDecimal.ZERO)
                .avgOrderValue(BigDecimal.ZERO)
                .build();
    }

    private BigDecimal calculateRoi(BigDecimal gmv, BigDecimal cost) {
        if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return gmv.divide(cost, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRate(Number numerator, Number denominator) {
        if (denominator == null || denominator.longValue() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator != null ? numerator.longValue() : 0)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator.longValue()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal toBigDecimal(Object obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj;
        }
        if (obj instanceof Number) {
            return BigDecimal.valueOf(((Number) obj).doubleValue());
        }
        return BigDecimal.ZERO;
    }
}
