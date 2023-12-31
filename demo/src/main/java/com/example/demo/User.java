package com.example.demo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Table(schema = User.SCHEMA_NAME, name = User.TABLE_NAME)
public class User implements Serializable {
    public static final String SCHEMA_NAME = "";
    public static final String TABLE_NAME  = "login_user";
    @Id
    private Integer id;
    private String  name;

    @Column(name="created_by")
    private String createdBy;
    @Column(name="created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user")
    private List<UserRole> userRoles = new ArrayList<>();
}