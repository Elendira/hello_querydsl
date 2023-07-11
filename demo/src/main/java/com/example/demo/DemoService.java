package com.example.demo;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        // JPQLQueryFactory
        List<User> userList = jpaQueryFactory.selectFrom(qUser).fetch();
        logger.info("JPQLQueryFactory: "+userList.toString());

        // SQLQueryFactory
        var userList2 = sqlQueryFactory.select(SLoginUser.loginUser.all())
                .from(SLoginUser.loginUser)
                .fetch(); // error here
        logger.info("SQLQueryFactory: "+userList2.toString());
        return userList;
    }
}
