package com.example.demo;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.sql.Configuration;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.SQLTemplates;
import com.querydsl.sql.spring.SpringConnectionProvider;
import com.querydsl.sql.spring.SpringExceptionTranslator;

import jakarta.persistence.EntityManager;

@org.springframework.context.annotation.Configuration
public class QueryDslConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private EntityManager entityManager;

    @Bean
    public Configuration configuration() {
        SQLTemplates templates = MySQLTemplates.builder().build();
        Configuration configuration = new Configuration(templates);
        configuration.setExceptionTranslator(new SpringExceptionTranslator());
        return configuration;
    }

    @Bean
    public JPQLQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }

    @Bean
    public SQLQueryFactory sqlQueryFactory() {
        return new SQLQueryFactory(configuration(), new SpringConnectionProvider(dataSource));
    }

}