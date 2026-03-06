package com.overseas.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.Result;
import com.overseas.purchase.dto.LoginDTO;
import com.overseas.purchase.dto.UserDTO;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 用户控制器
 * 
 * @author System
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Validated @RequestBody LoginDTO loginDTO) {
        try {
            Map<String, Object> result = userService.login(loginDTO);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody User user) {
        try {
            // 调试日志：查看接收到的用户数据
            System.out.println("注册接口 - 接收到的用户数据:");
            System.out.println("  用户名: " + user.getUsername());
            System.out.println("  昵称: " + user.getNickname());
            System.out.println("  角色: " + user.getRole());
            System.out.println("  邮箱: " + user.getEmail());
            
            userService.register(user);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/info")
    public Result<UserDTO> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserDTO user = userService.getUserById(userId);
        return Result.success(user);
    }
    
    /**
     * 更新用户信息
     */
    @PutMapping("/update")
    public Result<Void> updateUser(@RequestBody User user, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            String role = (String) request.getAttribute("role");
            
            // 普通用户只能更新自己的信息
            if (!"ADMIN".equals(role) && !userId.equals(user.getId())) {
                return Result.error("无权限操作");
            }
            
            userService.updateUser(user);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    /**
     * 管理员：分页查询用户列表
     */
    @GetMapping("/list")
    public Result<Page<UserDTO>> getUserList(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String keyword,
                                            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限访问");
        }
        
        Page<UserDTO> result = userService.getUserList(page, size, keyword);
        return Result.success(result);
    }
    
    /**
     * 管理员：删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限访问");
        }
        
        try {
            userService.deleteUser(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 通过邮箱和手机号找回密码（公开接口，无需认证）
     */
    @PostMapping("/reset-password-by-contact")
    public Result<Void> resetPasswordByEmailAndPhone(@RequestBody Map<String, Object> params) {
        try {
            String username = (String) params.get("username");
            String email = (String) params.get("email");
            String phone = (String) params.get("phone");
            String newPassword = (String) params.get("newPassword");

            if (username == null || email == null || phone == null || newPassword == null) {
                return Result.error("参数不完整");
            }

            userService.resetPasswordByEmailAndPhone(username, email, phone, newPassword);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
