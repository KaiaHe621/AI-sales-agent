package com.jichi.voiceshopping.repository;

import com.jichi.voiceshopping.entity.UserProfileStaticEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileStaticRepository extends JpaRepository<UserProfileStaticEntity, Long> {
}