package com.studyflow.wallet.dto;

public record UserWalletResponse(
        Integer pigBalance,
        Boolean todayGranted
) {
}
