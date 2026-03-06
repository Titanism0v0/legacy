package com.overseas.purchase.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
import com.overseas.purchase.common.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务类
 * 
 * @author System
 */
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserMapper userMapper;
    private final OrderMapper orderMapper;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final AddressMapper addressMapper;
    private final JwtUtil jwtUtil;
    
    /**
     * 用户登录
     */
    public Map<String, Object> login(LoginDTO loginDTO) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, loginDTO.getUsername())
                .eq(User::getDeleted, 0)
        );
        
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 简单密码加密验证（实际项目中应使用BCrypt等）
        String password = DigestUtils.md5DigestAsHex(loginDTO.getPassword().getBytes());
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        
        // 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        
        // 返回用户信息和Token
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", userDTO);
        
        return result;
    }
    
    /**
     * 用户注册
     */
    public void register(User user) {
        // 检查用户名是否已存在
        User existUser = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, user.getUsername())
                .eq(User::getDeleted, 0)
        );
        
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        // 密码加密
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        
        // 设置角色：如果未指定或指定为ADMIN，则默认为USER；只允许注册为USER或SELLER
        String originalRole = user.getRole();
        System.out.println("注册用户 - 接收到的角色: " + originalRole);
        
        if (originalRole == null || originalRole.isEmpty() || "ADMIN".equals(originalRole)) {
            user.setRole("USER");
            System.out.println("注册用户 - 角色为空或ADMIN，设置为USER");
        } else if ("SELLER".equals(originalRole)) {
            user.setRole("SELLER");
            System.out.println("注册用户 - 角色为SELLER，保持SELLER");
        } else if ("USER".equals(originalRole)) {
            user.setRole("USER");
            System.out.println("注册用户 - 角色为USER，保持USER");
        } else {
            // 其他未知角色，默认为USER
            user.setRole("USER");
            System.out.println("注册用户 - 未知角色: " + originalRole + "，设置为USER");
        }
        
        // 调试日志
        System.out.println("注册用户 - 最终保存的角色: " + user.getRole());
        System.out.println("注册用户 - 完整用户对象: " + user.toString());
        
        user.setStatus(1);
        
        userMapper.insert(user);
        
        // 插入后验证
        User insertedUser = userMapper.selectById(user.getId());
        System.out.println("注册用户 - 插入后查询的角色: " + (insertedUser != null ? insertedUser.getRole() : "null"));
    }
    
    /**
     * 根据ID查询用户
     */
    public UserDTO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            return null;
        }
        
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        return userDTO;
    }
    
    /**
     * 分页查询用户列表（管理员）
     */
    public Page<UserDTO> getUserList(Integer page, Integer size, String keyword) {
        Page<User> userPage = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getDeleted, 0);
        
        if (keyword != null && !keyword.isEmpty()) {
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
        }).collect(java.util.stream.Collectors.toList()));
        
        return dtoPage;
    }
    
    /**
     * 更新用户信息
     */
    public void updateUser(User user) {
        userMapper.updateById(user);
    }
    
    /**
     * 删除用户（级联逻辑删除）
     * 1. 删除用户发布的未售商品（状态不为OFF_SALE的待定，通常指在售或缺货但未成交的，这里根据需求：删除发布的商品，但不影响卖出给其他人的商品）
     *    需求解读：删除用户发布的商品。但不影响已经卖出给其他人的商品及其订单。
     *    实现：删除所有 seller_id = userId 的商品。如果商品已经关联了订单（已卖出），通常订单表存的是快照或引用。
     *    为了数据完整性，如果商品已经卖出（存在订单），该商品记录建议保留（或仅逻辑删除但订单还能查到）。
     *    本系统订单表存了 product_id。如果 product 被逻辑删除，MyBatisPlus 默认查不到。
     *    但需求说“不影响已经卖出给其他人的商品及其订单”。这意味着已售出的商品在订单里还能看到。
     *    通常订单会保存商品快照（如 title, price, image），查看 Order 实体确认。
     *    如果 Order 存了快照，那么删除 Product 表记录不影响订单查看。
     *    如果 Order 仅存 ID，删除 Product 后订单详情可能查不到商品信息。
     *    
     *    策略：
     *    1. 删除该用户作为买家的所有订单（buyer_id = userId）。
     *    2. 删除该用户购物车记录（user_id = userId）。
     *    3. 删除该用户发布的商品（seller_id = userId），但跳过那些“存在关联订单”的商品？
     *       或者：全部逻辑删除。只要订单查询时能通过 ID 查到（需要调整订单查询逻辑，允许查已删除商品）。
     *       需求：“不影响已经卖出给其他人的商品及其订单”。
     *       这意味着作为卖家的订单（seller_id = userId）不能删。
     *       这些订单关联的商品也不能删（或者删了但能查到）。
     *       
     *    修正策略：
     *    1. 删除用户本身。
     *    2. 删除用户作为买家的所有订单。
     *    3. 删除用户购物车。
     *    4. 处理用户发布的商品：
     *       查询该用户发布的所有商品。
     *       对于每个商品，检查是否存在关联的订单（作为卖家）。
     *       如果存在订单，则保留该商品（或仅下架处理，status=OFF_SALE，不逻辑删除）。
     *       如果不存在订单，则逻辑删除。
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        // 1. 删除用户作为买家的订单
        orderMapper.delete(new LambdaQueryWrapper<Order>()
                .eq(Order::getBuyerId, id));

        // 2. 删除用户的购物车记录
        cartMapper.delete(new LambdaQueryWrapper<Cart>()
                .eq(Cart::getUserId, id));
        
        // 3. 处理用户发布的商品
        // 查询用户发布的所有未删除商品
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<Product>()
                .eq(Product::getSellerId, id));
        
        for (Product product : products) {
            // 检查该商品是否有订单
            Long orderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getProductId, product.getId()));
            
            if (orderCount > 0) {
                // 有订单：保留商品记录，但设为下架状态，避免后续被购买
                product.setStatus("OFF_SALE");
                productMapper.updateById(product);
            } else {
                // 无订单：直接逻辑删除
                productMapper.deleteById(product.getId());
            }
        }

        // 4. 删除用户收货地址
        addressMapper.delete(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, id));

        // 5. 删除用户本身
        userMapper.deleteById(id);
    }

    /**
     * 通过邮箱和手机号找回密码
     * @param username 用户名
     * @param email 邮箱
     * @param phone 手机号
     * @param newPassword 新密码
     */
    public void resetPasswordByEmailAndPhone(String username, String email, String phone, String newPassword) {
        // 查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
                        .eq(User::getDeleted, 0)
        );

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证邮箱和手机号
        if (user.getEmail() == null || !user.getEmail().equals(email)) {
            throw new RuntimeException("邮箱不正确");
        }

        if (user.getPhone() == null || !user.getPhone().equals(phone)) {
            throw new RuntimeException("手机号不正确");
        }

        // 更新密码
        String encryptedPassword = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        user.setPassword(encryptedPassword);
        userMapper.updateById(user);
    }
}
