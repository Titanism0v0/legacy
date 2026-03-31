package com.overseas.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.LoginDTO;
import com.overseas.purchase.dto.RegisterRequest;
import com.overseas.purchase.dto.UserDTO;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Validated @RequestBody LoginDTO loginDTO) {
        try {
            Map<String, Object> result = userService.login(loginDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result<Void> register(@Validated @RequestBody RegisterRequest request) {
        try {
            userService.register(request);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/info")
    public Result<UserDTO> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserDTO user = userService.getUserById(userId);
        return Result.success(user);
    }

    @PutMapping("/update")
    public Result<Void> updateUser(@RequestBody User user, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");

            if (!"ADMIN".equals(role) && !userId.equals(user.getId())) {
                return Result.error("No permission");
            }

            userService.updateUser(user);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public Result<Page<UserDTO>> getUserList(@RequestParam(defaultValue = "1") Integer page,
                                             @RequestParam(defaultValue = "10") Integer size,
                                             @RequestParam(required = false) String keyword,
                                             HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "No permission");
        }

        Page<UserDTO> result = userService.getUserList(page, size, keyword);
        return Result.success(result);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "No permission");
        }

        try {
            userService.deleteUser(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/reset-password-by-contact")
    public Result<Void> resetPasswordByEmailAndPhone(@RequestBody Map<String, Object> params) {
        try {
            String username = (String) params.get("username");
            String email = (String) params.get("email");
            String phone = (String) params.get("phone");
            String newPassword = (String) params.get("newPassword");

            if (username == null || email == null || phone == null || newPassword == null) {
                return Result.error("Missing required parameters");
            }

            userService.resetPasswordByEmailAndPhone(username, email, phone, newPassword);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/kyc/submit")
    public Result<Void> submitKyc(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        try {
            String role = (String) request.getAttribute("role");
            if (!"SELLER".equals(role)) {
                return Result.error("No permission");
            }
            Long userId = (Long) request.getAttribute("userId");
            String kycFiles = params.get("kycFiles") == null ? null : params.get("kycFiles").toString();
            String remark = params.get("remark") == null ? null : params.get("remark").toString();
            userService.submitKyc(userId, kycFiles, remark);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/kyc/audit")
    public Result<Void> auditKyc(@RequestBody Map<String, Object> params, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "No permission");
        }
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            String action = params.get("action").toString();
            String remark = params.get("remark") == null ? null : params.get("remark").toString();
            userService.auditKyc(userId, action, remark);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}