package com.example.tomatomall.controller;

import com.example.tomatomall.service.AddressService;
import com.example.tomatomall.vo.AddressVO;
import com.example.tomatomall.vo.Response;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    @Resource
    AddressService addressService;

    /**
     * 获取用户的所有地址信息
     */
    @GetMapping
    public Response<List<AddressVO>> getAddressesByUserId() {
        return Response.buildSuccess(addressService.getAddresses());
    }

    /**
     * 创建新的地址
     */
    @PostMapping
    public Response<AddressVO> createAddress(@RequestBody AddressVO addressVO) {
        return Response.buildSuccess(addressService.createAddress(addressVO));
    }

    /**
     * 删除地址
     */
    @DeleteMapping("/{shoppingAddressId}")
    public Response<String> deleteAddress(@PathVariable Integer shoppingAddressId) {
        return Response.buildSuccess(addressService.deleteAddress(shoppingAddressId));

    }

    /**
     * 更新地址信息
     */
    @PutMapping
    public Response<String> updateAddress(@RequestBody AddressVO addressVO) {
        return Response.buildSuccess(addressService.updateAddress(addressVO));
    }
}