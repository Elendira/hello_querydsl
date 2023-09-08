package com.example.demo;

import java.io.Serializable;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"})
@Table(schema = Role.SCHEMA_NAME, name = Role.TABLE_NAME)
public class Role implements Serializable {
    public static final String SCHEMA_NAME = "";
    public static final String TABLE_NAME  = "roles2";
    @Id
    @Column(nullable = false, unique = true)
    private Integer id;
    @Column(nullable = false)
    private String name;
    @Column(name="created_by")
    private String createdBy;
    @Column(name="created_at")
    private LocalDateTime createdAt;
}