package com.jichi.prompt.controller;

import com.jichi.prompt.service.SqlToJpaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/code-gen")
public class SqlToJpaController {

    private final SqlToJpaService sqlToJpaService;

    public SqlToJpaController(SqlToJpaService sqlToJpaService) {
        this.sqlToJpaService = sqlToJpaService;
    }

    record SqlRequest(String sql, String entityName) {
    }

    @PostMapping("/sql-to-jpa")
    public String convertSqlToJpa(@RequestBody SqlRequest req) {
        return sqlToJpaService.convertSqlToJpa(req.sql(), req.entityName());
    }
}