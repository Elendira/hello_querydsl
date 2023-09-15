package com.example.demo;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.Union;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
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
    @Lazy
    @Autowired
    private UserSpecification userSpecification;

    @Component
    class UserSpecification {
        @Autowired
        private EntityManager entityManager;

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
                return null;
            };
        }

        static public Specification<User> equalsID(Integer id) {
            return id == 0 ? null
                    : (root, query, cb) -> cb.equal(root.get(User_.id), id);
        }

        static public Specification<User> equalsIDs(List<Integer> ids) {
            return ids == null || ids.isEmpty() ? null
                    : (root, query, cb) -> cb.in(root.get(User_.ID)).value(ids);
        }

        static public Specification<User> existsRole(String[] roles) {
            return roles == null || roles.length == 0 ? null
                    : (root, query, cb) -> {
                        Subquery<Integer> subquery = query.subquery(Integer.class);
                        Root<UserRole> subRoot = subquery.from(UserRole.class);
                        Join<UserRole, Role> subRole = subRoot.join(UserRole_.role, JoinType.INNER);
                        return cb.exists(subquery.select(cb.literal(1))
                                .where(
                                        cb.and(
                                                // userIdをリテラルで指定しているのはUserRole.PrimaryKeyのStaticMetamodelクラスが生成されていないため
                                                cb.equal(root.get(User_.id), subRoot.get(UserRole_.pk).get("userId")),
                                                cb.in(subRole.get(Role_.NAME)).value(Arrays.asList(roles)))));
                    };
        }

        public List<Integer> findIds(Specification<User> specification, Pageable pageable) {
            CriteriaBuilder builder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Integer> query = builder.createQuery(Integer.class);
            Root<User> root = query.from(User.class);
            query.select(root.get(User_.id)).where(specification.toPredicate(root, query, builder));
            query.orderBy(getOrderList(pageable, builder, root));
            TypedQuery<Integer> typedQuery = entityManager.createQuery(query);
            typedQuery.setFirstResult(pageable.getPageNumber() * pageable.getPageSize());
            typedQuery.setMaxResults(pageable.getPageSize());
            return typedQuery.getResultList();
        }
    
        private List<Order> getOrderList(Pageable pageable, CriteriaBuilder builder, Root<User> root) {
            List<Order> orders = new ArrayList<>();
            pageable.getSort().forEach(sort -> {
                if (sort.isAscending()) {
                    orders.add(builder.asc(root.get(sort.getProperty())));
                } else {
                    orders.add(builder.desc(root.get(sort.getProperty())));
                }
            });
            return orders;
        }
    }
   
    @Transactional
    public List<User> listing() {
        jpaTest();

        // JPQLQueryFactory
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

        List<UserRoleCountView> userRoleCountList = jpaQueryFactory
                .select(Projections.constructor(UserRoleCountView.class, qUser.id, qUser.name, qRole.id.count()))
                .from(qUser)
                .leftJoin(qUser.userRoles, qUserRole)
                .leftJoin(qUserRole.role, qRole)
                .where(whereClause)
                // .groupBy(qUser) // 全カラム指定
                .groupBy(qUser.id, qUser.name)
                .fetch();
        logger.info("userRoleCountList: " + userRoleCountList.toString());

        // querydslでUser, UserRoleの削除
        // Userだけ削除すると外部キー制約違反でエラーになる
        // User user = userList.get(0);
        // jpaQueryFactory.delete(qUserRole)
        //         .where(qUserRole.pk.userId.eq(user.getId()))
        //         .execute();
        // jpaQueryFactory.delete(qUser)
        //         .where(qUser.id.eq(user.getId()))
        //         .execute();

        // SQLQueryFactory
        SLoginUser sLoginUser = SLoginUser.loginUser;
        SUserRole  sUserRole  = SUserRole.userRole;
        SRoles2    sRoles     = SRoles2.roles2;
        BooleanExpression whereClause2 = JPAExpressions.selectOne()
	        .from(sUserRole)
            .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
            .where(
                sLoginUser.id.eq(sUserRole.userId).andAnyOf(
                    sRoles.name.eq("ROLE_ADMIN"),
                    sRoles.name.eq("ROLE_ADMIN1")
		        )
            ).exists();
        var userList2 = sqlQueryFactory.select(sLoginUser.all())
                .from(SLoginUser.loginUser)
                .fetch(); // error here
        logger.info("SQLQueryFactory: "+userList2.toString());

        // union test1
        SQLQuery<UserRoleCountView> query1 = sqlQueryFactory
                .select(Projections.constructor(UserRoleCountView.class, sLoginUser.id, sLoginUser.name,
                        sRoles.id.min().longValue()))
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .groupBy(sLoginUser.id, sLoginUser.name);
        SQLQuery<UserRoleCountView> query2 = sqlQueryFactory
                .select(Projections.constructor(UserRoleCountView.class, sLoginUser.id, sLoginUser.name,
                        sRoles.id.max().longValue()))
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .groupBy(sLoginUser.id, sLoginUser.name);
        userRoleCountList = sqlQueryFactory.query().unionAll(
            Arrays.asList(query1, query2)
        ).fetch();
        logger.info("union SQL: "+userRoleCountList.toString());

        // union test2
        Union<UserRoleCountView> union = sqlQueryFactory.query().unionAll(
                Arrays.asList(query1, query2));
        userRoleCountList = sqlQueryFactory.query().unionAll(
                Arrays.asList(union, query2)).fetch();
        logger.info("union SQL2: "+userRoleCountList.toString());

        // union test3
        PathBuilder<UserRoleCountView> unionPath = new PathBuilder<>(UserRoleCountView.class, "t");
        PathBuilder<Integer> idPath    = new PathBuilder<>(Integer.class, "id");
        PathBuilder<String>  namePath  = new PathBuilder<>(String.class , "name");
        PathBuilder<Long>    countPath = new PathBuilder<>(Long.class   , "count");
        SubQueryExpression<Tuple> query3 = SQLExpressions
                .select(sLoginUser.id, sLoginUser.name, sRoles.id.min().longValue().as(countPath))
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .groupBy(sLoginUser.id, sLoginUser.name);
        SubQueryExpression<Tuple> query4 = SQLExpressions
                .select(sLoginUser.id, sLoginUser.name, sRoles.id.max().longValue().as(countPath))
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .groupBy(sLoginUser.id, sLoginUser.name);
        Union<Tuple> union2 = SQLExpressions.unionAll(Arrays.asList(query3, query4));
        SQLQuery<?> query = sqlQueryFactory.query();
        List<UserRoleCountView> tupleList = query
                .select(Projections.constructor(UserRoleCountView.class, idPath, namePath, countPath))
                .from(union2, unionPath)
                .where(unionPath.get(namePath).eq("OOTANI"))
                .fetch();
        logger.info("union SQL3: \n" + query.getSQL().getSQL()+"\n"+tupleList.toString());

        // 完全マニュアル記述
        PathBuilder<User> userPath = new PathBuilder<>(User.class, "login_user");
        query = sqlQueryFactory.query();
        userList = query.select(
                Projections.fields(User.class, userPath.get(idPath), userPath.get(namePath)))
                .from(userPath)
                .fetch();
        logger.info("manual SQL: \n" + query.getSQL().getSQL() + "\n" + userList.toString());

        return userList;
    }

    @Transactional
    public void jpaTest() {
        // 非エンティテイクラスの結果を取得
        List<UserRoleCountView> userRoleCountList = userRepository.findUserRoleCount();
        logger.info("UserRoleCountView="+userRoleCountList.toString());

        String[] roles = new String[] {"ROLE_ADMIN", "ROLE_GENERAL"};
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Direction.ASC, "id"));

        // JPA findAll
        // 外部結合とページング処理を組み合わせると
        // 「WARN: HHH90003004: firstResult/maxResults specified with collection fetch; applying in memory」
        // メモリソートになるので潜在的エラーの要因として警告が出る
        // 対策としてはPK取得とデータ本体取得を分ける
        Page<User> userList = userRepository.findAll(
                Specification.where(UserSpecification.join())
                        .and(UserSpecification.equalsID(0))
                        .and(UserSpecification.existsRole(roles)),
                pageRequest);
        logger.info("JPA findAll: "+userList.getContent().toString());

        // WARN: HHH90003004対応策
        // レコード件数取得
        Specification<User> spec = Specification.where(UserSpecification.equalsID(0))
                        .and(UserSpecification.existsRole(roles));
        long userCount = userRepository.count(spec);
        logger.info("userCount=" + userCount);
        // 指定ページのPK取得
        List<Integer> userIdList = userSpecification.findIds(spec, pageRequest);
        logger.info("userIdList="+userIdList.toString());
        // データ本体取得
        List<User> users = userRepository.findAll(
                Specification.where(UserSpecification.join())
                .and(UserSpecification.equalsIDs(userIdList))
        );
        Page<User> accountPage = new PageImpl<User>(users, pageRequest, userCount);
        logger.info("JPA Page<User>: "+accountPage.toString());
        logger.info("JPA Page<User>.content: "+accountPage.getContent().toString());

        // User, UserRoleの更新
        User user = users.get(0);
        user.setName("I am a new name");
        UserRole userRole = user.getUserRoles().get(0);
        userRole.setBeginDate(LocalDate.parse("2000-01-01", DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        userRepository.save(userRole.getUser()); // Update User and UserRole

        // User, UserRoleの削除
        // Userだけ削除しても外部キー制約違反エラーにならない。Userも削除されない
        // userRoleRepository.deleteAll(user.getUserRoles());
        // userRepository.delete(user);
    }
}
