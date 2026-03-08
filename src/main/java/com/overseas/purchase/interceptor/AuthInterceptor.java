package com.overseas.purchase.interceptor;

import com.overseas.purchase.common.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 认证拦截器
 * 
 * @author System
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    
    private final JwtUtil jwtUtil;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行OPTIONS请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }
        
        // 放行静态资源和部分公开接口
        String path = request.getRequestURI();
        if (path.contains("/login")
                || path.contains("/register")
                || path.contains("/category/list")
                || path.contains("/product/list")
                || path.contains("/product/detail/")
                || path.contains("/seller-review/list") // 商家评价列表公开
                || path.contains("/upload/")
                || path.contains("/static/")) {
            return true;
        }
        
        // 获取Token
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        
        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            return false;
        }
        
        // 将用户信息存入request
        Long userId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);
        request.setAttribute("userId", userId);
        request.setAttribute("role", role);
        
        return true;
    }
}
