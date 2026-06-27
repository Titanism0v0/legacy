package com.overseas.purchase.interceptor;

import com.overseas.purchase.common.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (token != null && !token.isEmpty() && jwtUtil.validateToken(token)) {
            request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
            request.setAttribute("role", jwtUtil.getRoleFromToken(token));
        }

        if (isPublicRequest(request, path)) {
            return true;
        }

        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            return false;
        }
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(401);
            return false;
        }

        request.setAttribute("userId", jwtUtil.getUserIdFromToken(token));
        request.setAttribute("role", jwtUtil.getRoleFromToken(token));
        return true;
    }

    private boolean isPublicRequest(HttpServletRequest request, String path) {
        if (isPublicUploadRead(request, path)) {
            return true;
        }
        if (path.contains("/login")
                || path.contains("/register")
                || path.contains("/legal/current")
                || path.contains("/category/list")
                || path.contains("/category/top")
                || path.contains("/category/sub/")
                || path.contains("/product/list")
                || path.contains("/product/detail/")
                || path.contains("/order/status-flow")
                || path.contains("/seller-review/list")
                || path.contains("/payment/notify/wechat")
                || path.contains("/payment/notify/alipay")
                || path.contains("/static/")) {
            return true;
        }
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        if (path.contains("/admin/community")) {
            return false;
        }
        return path.contains("/community/posts");
    }

    private boolean isPublicUploadRead(HttpServletRequest request, String path) {
        boolean readMethod = "GET".equalsIgnoreCase(request.getMethod())
                || "HEAD".equalsIgnoreCase(request.getMethod());
        return readMethod && (path.contains("/upload/") || path.contains("/uploads/"));
    }
}
