package com.integrafty.opexy.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Primary
    @Bean(name = "primaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate primaryJdbcTemplate(@Qualifier("primaryDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "dashboardDataSource")
    @ConfigurationProperties(prefix = "app.datasource.dashboard")
    public DataSource dashboardDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "dashboardJdbcTemplate")
    public JdbcTemplate dashboardJdbcTemplate(@Qualifier("dashboardDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
