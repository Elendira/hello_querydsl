package com.example.demo;

import java.time.LocalDateTime;

import com.querydsl.core.annotations.QueryEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(schema = User.SCHEMA_NAME, name = User.TABLE_NAME)
@Data
@QueryEntity
@Builder
@AllArgsConstructor( staticName = "of" )
@NoArgsConstructor
public class User {
    public static final String SCHEMA_NAME = "";
    public static final String TABLE_NAME  = "login_user";
    @Id
    private Integer id;
    private String  name;

    @Column(name="created_by")
    private String createdBy;
    @Column(name="created_at")
    private LocalDateTime createdAt;
}