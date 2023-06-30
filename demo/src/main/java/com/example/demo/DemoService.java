package com.example.demo;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.sql.SQLQueryFactory;

import jakarta.transaction.Transactional;

@Service
public class DemoService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private SQLQueryFactory sqlQueryFactory;
    @Autowired
    private JPQLQueryFactory jpaQueryFactory;

    @Transactional
    public List<User> listing() {
        QUser qUser = QUser.user;
        // JPQLQueryFactory is OK
        List<User> userList = jpaQueryFactory.selectFrom(qUser).fetch();
        logger.info("JPQLQueryFactory: "+userList.toString());

        // SQLQueryFactory is NG
        List<Tuple> list = sqlQueryFactory.select(
                qUser.id, qUser.name, qUser.createdBy, qUser.createdAt)
                .from(qUser)
                .fetch(); // error here
        userList = list.stream()
            .map(tuple -> {
                User user = User.builder()
                    .id(tuple.get(qUser.id))
                    .name(tuple.get(qUser.name))
                    .createdBy(tuple.get(qUser.createdBy))
                    .createdAt(tuple.get(qUser.createdAt))
                    .build();
                return user;
            }).collect(Collectors.toList());
        logger.info("SQLQueryFactory: "+userList.toString());
        return userList;
    }
}
