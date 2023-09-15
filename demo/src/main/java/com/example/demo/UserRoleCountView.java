package com.example.demo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserRoleCountView {
    private Integer id;
    private String  name;
    private Long    roleCount;
}
