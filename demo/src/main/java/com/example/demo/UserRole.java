package com.example.demo;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(schema = UserRole.SCHEMA_NAME, name = UserRole.TABLE_NAME)
public class UserRole implements Serializable {
    public static final String SCHEMA_NAME = "";
    public static final String TABLE_NAME  = "user_role";
    @EmbeddedId
    private PrimaryKey pk;

    @Column(name = "begin_date")
    private LocalDate beginDate;

    @ManyToOne
    @MapsId("userId")
    private User user;

    @ManyToOne
    @MapsId("roleId")
    private Role role;

    public UserRole(User user, Role role, LocalDate beginDate) {
        this.user      = user;
        this.role      = role;
        this.beginDate = beginDate;
    }

    @Getter
    @Embeddable
    public static class PrimaryKey implements Serializable{
        @Column(name = "user_id")
        private Integer userId;
        @Column(name = "role_id")
        private Integer roleId;
    }

    @Override
    public String toString() {
        return "UserRole [pk=" + pk + ", beginDate=" + beginDate + ", role=" + role + "]";
    }
}
