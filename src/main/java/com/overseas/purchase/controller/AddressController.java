package com.overseas.purchase.controller;

import com.overseas.purchase.common.Result;
import com.overseas.purchase.entity.Address;
import com.overseas.purchase.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 收货地址控制器
 * 
 * @author System
 */
@RestController
@RequestMapping("/address")
@RequiredArgsConstructor
public class AddressController {
    
    private final AddressService addressService;
    
    /**
     * 添加收货地址
     */
    @PostMapping("/add")
    public Result<Void> addAddress(@RequestBody Address address, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            address.setUserId(userId);
            addressService.addAddress(address);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }
    
    /**
     * 查询用户的收货地址列表
     */
    @GetMapping("/list")
    public Result<List<Address>> getAddressList(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Address> addresses = addressService.getAddressList(userId);
        return Result.success(addresses);
    }
    
    /**
     * 更新收货地址
     */
    @PutMapping("/update")
    public Result<Void> updateAddress(@RequestBody Address address, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            addressService.updateAddress(address, userId);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }
    
    /**
     * 删除收货地址
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            addressService.deleteAddress(id, userId);
            return Result.success();
        } catch (Exception e) {
            return com.overseas.purchase.common.PublicErrorResponse.from("请求处理失败，请稍后重试", e);
        }
    }
}
