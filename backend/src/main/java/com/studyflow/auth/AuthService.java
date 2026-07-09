package com.studyflow.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.studyflow.auth.dto.LoginRequest;
import com.studyflow.auth.dto.LoginResponse;
import com.studyflow.auth.dto.RegisterRequest;
import com.studyflow.common.BusinessException;
import com.studyflow.community.member.CommunityMemberService;
import com.studyflow.security.JwtService;
import com.studyflow.user.User;
import com.studyflow.user.UserMapper;
import com.studyflow.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CommunityMemberService communityMemberService;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CommunityMemberService communityMemberService
    ) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.communityMemberService = communityMemberService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (existsByUsername(request.username())) {
            throw new BusinessException(400, "用户名已存在");
        }
        if (existsByEmail(request.email())) {
            throw new BusinessException(400, "邮箱已存在");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("MEMBER");
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        communityMemberService.ensureDefaultMembership(user.getId(), user.getUsername());

        return UserResponse.from(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, request.username()));

        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(400, "用户名或密码错误");
        }

        String token = jwtService.generateToken(user.getId(), user.getUsername());
        return new LoginResponse(token, UserResponse.from(user));
    }

    private boolean existsByUsername(String username) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
        return count > 0;
    }

    private boolean existsByEmail(String email) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        return count > 0;
    }
}
