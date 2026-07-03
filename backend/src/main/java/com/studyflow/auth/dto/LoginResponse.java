package com.studyflow.auth.dto;

import com.studyflow.user.dto.UserResponse;

public record LoginResponse(String token, UserResponse user) {
}
