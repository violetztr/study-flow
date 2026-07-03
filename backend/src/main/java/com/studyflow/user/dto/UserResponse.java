package com.studyflow.user.dto;

import com.studyflow.user.User;

public record UserResponse(Long id, String username, String email) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
