package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.overseas.purchase.common.JwtUtil;
import com.overseas.purchase.dto.LoginDTO;
import com.overseas.purchase.dto.UserDTO;
import com.overseas.purchase.entity.Address;
import com.overseas.purchase.entity.Cart;
import com.overseas.purchase.entity.Order;
import com.overseas.purchase.entity.Product;
import com.overseas.purchase.entity.User;
import com.overseas.purchase.mapper.AddressMapper;
import com.overseas.purchase.mapper.CartMapper;
import com.overseas.purchase.mapper.OrderMapper;
import com.overseas.purchase.mapper.ProductMapper;
import com.overseas.purchase.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;
    private final JwtUtil jwtUtil;

    public Map<String, Object> login(LoginDTO loginDTO) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername())
                .eq(User::getDeleted, 0));
        if (user == null) {
            throw new RuntimeException("Invalid username or password");
        }

        String password = DigestUtils.md5DigestAsHex(loginDTO.getPassword().getBytes());
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid username or password");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("Account disabled");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userDTO);
        return result;
    }

    public void register(User user) {
        User existUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername())
                .eq(User::getDeleted, 0));
        if (existUser != null) {
            throw new RuntimeException("Username already exists");
        }

        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        if (!"SELLER".equals(user.getRole())) {
            user.setRole("USER");
        }
        if ("SELLER".equals(user.getRole())) {
            user.setKycStatus("UNSUBMITTED");
        }
        user.setStatus(1);
        userMapper.insert(user);
    }

    public UserDTO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            return null;
        }
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        return userDTO;
    }

    public Page<UserDTO> getUserList(Integer page, Integer size, String keyword) {
        Page<User> userPage = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }

        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(userPage, wrapper);

        Page<UserDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream().map(user -> {
            UserDTO dto = new UserDTO();
            BeanUtils.copyProperties(user, dto);
            return dto;
        }).collect(Collectors.toList()));
        return dtoPage;
    }

    public void updateUser(User user) {
        userMapper.updateById(user);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        orderMapper.delete(new LambdaQueryWrapper<Order>().eq(Order::getBuyerId, id));
        cartMapper.delete(new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, id));

        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>().eq(Product::getSellerId, id));
        for (Product product : products) {
            Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>().eq(Order::getProductId, product.getId()));
            if (orderCount != null && orderCount > 0) {
                product.setStatus("OFF_SALE");
                productMapper.updateById(product);
            } else {
                productMapper.deleteById(product.getId());
            }
        }

        addressMapper.delete(new LambdaQueryWrapper<Address>().eq(Address::getUserId, id));
        userMapper.deleteById(id);
    }

    public void resetPasswordByEmailAndPhone(String username, String email, String phone, String newPassword) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .eq(User::getDeleted, 0));
        if (user == null) {
            throw new RuntimeException("User does not exist");
        }
        if (!StringUtils.hasText(user.getEmail()) || !user.getEmail().equals(email)) {
            throw new RuntimeException("Email mismatch");
        }
        if (!StringUtils.hasText(user.getPhone()) || !user.getPhone().equals(phone)) {
            throw new RuntimeException("Phone mismatch");
        }
        user.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        userMapper.updateById(user);
    }

    @Transactional
    public void submitKyc(Long userId, String kycFiles, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new RuntimeException("User does not exist");
        }
        if (!"SELLER".equals(user.getRole())) {
            throw new RuntimeException("Only seller can submit KYC");
        }
        validateKycPayload(kycFiles);
        user.setKycStatus("PENDING");
        user.setKycFiles(kycFiles);
        if (remark != null) {
            user.setKycRemark(remark);
        }
        userMapper.updateById(user);
    }

    @Transactional
    public void auditKyc(Long userId, String action, String remark) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new RuntimeException("User does not exist");
        }
        if (!"SELLER".equals(user.getRole())) {
            throw new RuntimeException("Not a seller");
        }

        if ("APPROVE".equalsIgnoreCase(action)) {
            user.setKycStatus("APPROVED");
        } else if ("REJECT".equalsIgnoreCase(action)) {
            user.setKycStatus("REJECTED");
        } else {
            throw new RuntimeException("Invalid action");
        }
        user.setKycRemark(remark);
        userMapper.updateById(user);
    }

    private void validateKycPayload(String kycFiles) {
        if (!StringUtils.hasText(kycFiles)) {
            throw new RuntimeException("KYC materials are required");
        }
        String normalized = kycFiles.replace(" ", "").replace("\n", "");
        if (!normalized.contains("identityDocUrl")
                || !normalized.contains("sourceDescription")
                || !normalized.contains("paymentQrUrl")) {
            throw new RuntimeException("KYC materials must include identityDocUrl, sourceDescription and paymentQrUrl");
        }
    }
}
