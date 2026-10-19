package com.jichi.voiceshopping.config;

import com.pgvector.PGvector;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;

@Configuration
public class PgVectorConfig {

    /** 为当前 DataSource 下的连接注册 PGvector 类型。 */
    @Bean
    public CommandLineRunner registerPgVectorType(DataSource ds) {
        return args -> {
            try (Connection c = ds.getConnection()) {
                // pgvector-java 内部会自己 unwrap 到 PGConnection，业务代码不用管
                PGvector.addVectorType(c);
            }
        };
    }
}