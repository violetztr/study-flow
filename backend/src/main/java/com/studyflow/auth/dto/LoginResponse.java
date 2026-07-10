package com.studyflow.auth.dto;

import com.studyflow.user.dto.UserResponse;
import com.studyflow.wallet.dto.UserWalletResponse;

public record LoginResponse(String token, UserResponse user, UserWalletResponse wallet) {
}
