package com.assignment.auth.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppUser {
    private Long id;
    private String username;
    private String passwordHash;
    private Role role;
    private Long managerId;
}
