package com.homestay3.homestaybackend.repository;

import com.homestay3.homestaybackend.entity.PromotionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionRuleRepository extends JpaRepository<PromotionRule, Long> {

    List<PromotionRule> findByCampaignId(Long campaignId);
}
