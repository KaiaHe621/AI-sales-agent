package com.jichi.voiceshopping.repository;

import com.jichi.voiceshopping.entity.SessionStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SessionStateRepository extends JpaRepository<SessionStateEntity, String> {
}