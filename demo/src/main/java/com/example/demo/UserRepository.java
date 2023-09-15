package com.example.demo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
// import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    // @EntityGraphで指定すると関連テーブルが必要ない時も結合してしまうのでUserSpecification.join()で都度指定するようにした
    // @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    void delete(User user);

    @Query("SELECT new com.example.demo.UserRoleCountView(u.id, u.name, count(ur)) FROM User u LEFT JOIN u.userRoles ur GROUP BY u.id, u.name")
    List<UserRoleCountView> findUserRoleCount();

    long count(Specification<User> spec);
}
