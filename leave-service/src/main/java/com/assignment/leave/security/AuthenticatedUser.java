package com.assignment.leave.security;

import com.assignment.leave.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser {
    private Long userId;
    private Role role;
    private Long managerId;
    private String username;
}
