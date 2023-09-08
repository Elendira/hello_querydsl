package com.example.demo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
// import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    // @EntityGraphで指定すると関連テーブルが必要ない時も結合してしまうのでUserSpecification.join()で都度指定するようにした
    // @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);

    void delete(User user);
}
