package com.example.demo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.sql.SQLQueryFactory;

import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.transaction.Transactional;

@Service
public class DemoService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private SQLQueryFactory sqlQueryFactory;
    @Autowired
    private JPQLQueryFactory jpaQueryFactory;

    class UserSpecification {
        static public Specification<User> join() {
            return (root, query, cb) -> {
                // ページング処理と組み合わせると、カウント用SQLで関連テーブルを使用していないエラーが出るので
                // 戻り値の型が Long.class 以外の場合に結合するようにした。
                // https://hepokon365.hatenablog.com/entry/2021/12/31/160502
                // https://stackoverflow.com/questions/29348742/spring-data-jpa-creating-specification-query-fetch-joins
                // ここでは Long.class 以外に long.class とも比較しているが、 Long.class だけで動いている。
                if (Long.class != query.getResultType()) {
                    Fetch<User, UserRole> userRole = root.fetch(User_.userRoles, JoinType.LEFT);
                    userRole.fetch(UserRole_.role, JoinType.LEFT);
                }
                return cb.conjunction();
            };
        }

        static public Specification<User> equalsID(Integer id) {
            return id == 0 ? null
                    : (root, query, cb) -> cb.equal(root.get(User_.id), id);
        }

        static public Specification<User> existsRole(String[] roles) {
            return roles == null || roles.length == 0 ? null
                    : (root, query, cb) -> {
                        Subquery<Integer> subquery = query.subquery(Integer.class);
                        Root<User> subRoot = subquery.from(User.class);
                        ListJoin<User, UserRole> subUserRole = subRoot.join(User_.userRoles, JoinType.INNER);
                        Join<UserRole, Role> subRole = subUserRole.join(UserRole_.role, JoinType.INNER);
                        return cb.exists(subquery.select(cb.literal(1))
                                .where(
                                        cb.and(
                                                cb.equal(root.get(User_.id), subRoot.get(User_.id)),
                                                cb.in(subRole.get(Role_.NAME)).value(Arrays.asList(roles)))));
                    };
        }
    }
     
    @Transactional
    public List<User> listing() {
        QUser     qUser     = QUser.user;
        QUserRole qUserRole = QUserRole.userRole;
        QRole     qRole     = QRole.role;
        BooleanExpression whereClause = qUser.id.gt(0);
        whereClause = whereClause.and(JPAExpressions.selectOne()
                .from(qUserRole)
                .innerJoin(qUserRole.role, qRole)
                .where(qUserRole.pk.userId.eq(qUser.id)
                        .and(qRole.name.in("ROLE_ADMIN", "ROLE_GENERAL")))
                .exists());
        List<User> userList = jpaQueryFactory
                .selectFrom(qUser)
                .leftJoin(qUser.userRoles, qUserRole).fetchJoin()
                .leftJoin(qUserRole.role, qRole).fetchJoin()
                .where(whereClause)
                .fetch();
        logger.info("JPQLQueryFactory: "+userList.toString());

        // SQLQueryFactory
        var userList2 = sqlQueryFactory.select(SLoginUser.loginUser.all())
                .from(SLoginUser.loginUser)
                .fetch(); // error here
        logger.info("SQLQueryFactory: "+userList2.toString());

        return userList;
    }

    @Transactional
    public void jpaTest() {
        // JPA
        Page<User> userList3 = userRepository.findAll(
                Specification.where(UserSpecification.join())
                .and(UserSpecification.equalsID(0))
                .and(UserSpecification.existsRole(new String[] {"ROLE_ADMIN", "ROLE_GENERAL"}))
                , PageRequest.of(0, 10, Sort.by(Direction.ASC, "id"))
        );
        logger.info("JPA: "+userList3.getContent().toString());

        User user = userList3.getContent().get(0);

        user.setName("I am a new name");
        UserRole userRole = user.getUserRoles().get(0);
        userRole.setBeginDate(LocalDate.parse("2000-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        userRepository.save(userRole.getUser()); // Update User and UserRole
        
        userRoleRepository.deleteAll(user.getUserRoles());
        userRepository.delete(user); // Delete User and UserRole
    }
}
