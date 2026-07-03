package com.studyflow.user;

import com.studyflow.common.ApiResponse;
import com.studyflow.common.BusinessException;
import com.studyflow.security.UserPrincipal;
import com.studyflow.user.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new BusinessException(401, "未登录");
        }

        User user = userMapper.selectById(principal.userId());
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        return ApiResponse.success(UserResponse.from(user));
    }
}
