package com.studyflow.wallet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.studyflow.wallet.dto.UserWalletResponse;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PigWalletService {
    private static final String REWARD_DAILY_LOGIN = "DAILY_LOGIN";

    private final UserWalletMapper userWalletMapper;
    private final UserDailyRewardMapper userDailyRewardMapper;

    public PigWalletService(
            UserWalletMapper userWalletMapper,
            UserDailyRewardMapper userDailyRewardMapper
    ) {
        this.userWalletMapper = userWalletMapper;
        this.userDailyRewardMapper = userDailyRewardMapper;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public UserWalletResponse grantDailyLoginPig(Long userId) {
        ensureWallet(userId);
        LocalDate today = LocalDate.now();
        UserDailyReward reward = new UserDailyReward();
        reward.setUserId(userId);
        reward.setRewardDate(today);
        reward.setRewardType(REWARD_DAILY_LOGIN);
        reward.setAmount(1);
        reward.setCreatedAt(LocalDateTime.now());

        boolean granted = false;
        try {
            userDailyRewardMapper.insert(reward);
            userWalletMapper.update(null, new LambdaUpdateWrapper<UserWallet>()
                    .eq(UserWallet::getUserId, userId)
                    .setSql("pig_balance = pig_balance + 1")
                    .set(UserWallet::getUpdatedAt, LocalDateTime.now()));
            granted = true;
        } catch (DuplicateKeyException ignored) {
            granted = false;
        }

        return new UserWalletResponse(currentPigBalance(userId), granted);
    }

    public UserWalletResponse currentWallet(Long userId) {
        ensureWallet(userId);
        return new UserWalletResponse(currentPigBalance(userId), false);
    }

    private void ensureWallet(Long userId) {
        UserWallet wallet = userWalletMapper.selectOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId));
        if (wallet != null) {
            return;
        }

        UserWallet newWallet = new UserWallet();
        newWallet.setUserId(userId);
        newWallet.setPigBalance(0);
        newWallet.setCreatedAt(LocalDateTime.now());
        newWallet.setUpdatedAt(LocalDateTime.now());
        try {
            userWalletMapper.insert(newWallet);
        } catch (DuplicateKeyException ignored) {
            // Created by a concurrent login; safe to continue.
        }
    }

    private Integer currentPigBalance(Long userId) {
        UserWallet wallet = userWalletMapper.selectOne(new LambdaQueryWrapper<UserWallet>()
                .eq(UserWallet::getUserId, userId));
        return wallet == null ? 0 : wallet.getPigBalance();
    }
}
