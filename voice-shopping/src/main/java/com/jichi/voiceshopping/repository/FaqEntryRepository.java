package com.jichi.voiceshopping.repository;

import com.jichi.voiceshopping.entity.FaqEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FaqEntryRepository extends JpaRepository<FaqEntryEntity, Long> {
    List<FaqEntryEntity> findByStatusAndMerchantIdIn(String status, List<Long> merchantIds);

    // @Modifying 必须搭配事务执行，这里直接在仓储方法上声明，调用方不用再关心事务边界
    @Modifying
    @Transactional
    @Query("update FaqEntryEntity f set f.hitCount = f.hitCount + 1 where f.id = :id")
    void incrHitCount(@Param("id") Long id);
}