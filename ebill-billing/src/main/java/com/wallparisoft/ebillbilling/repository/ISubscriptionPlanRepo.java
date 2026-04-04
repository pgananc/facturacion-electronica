package com.wallparisoft.ebillbilling.repository;

import com.wallparisoft.ebillbilling.entity.SubscriptionPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ISubscriptionPlanRepo extends JpaRepository<SubscriptionPlan, Long> {

  Optional<SubscriptionPlan> findByCode(String code);

  Optional<SubscriptionPlan> findByCodeAndIsActiveTrue(String code);
}
