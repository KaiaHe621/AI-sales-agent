package com.jichi.salesAgent.repository;

import com.jichi.salesAgent.entity.SalesRep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

import java.util.List;

@Repository
public interface SalesRepRepository extends JpaRepository<SalesRep, Long> {

    List<SalesRep> findByRegionId(Long regionId);

    List<SalesRep> findByRole(String role);

    Optional<SalesRep> findByName(String name);

    List<SalesRep> findByRoleAndRegionId(String role, Long regionId);
}
