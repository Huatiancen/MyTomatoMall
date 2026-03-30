package com.example.tomatomall.service;

import com.example.tomatomall.vo.AddressVO;

import java.util.List;

public interface AddressService {

    /**
     * 获取用户的所有地址信息
     */
    List<AddressVO> getAddresses();

    /**
     * 创建新的地址记录
     */
    AddressVO createAddress(AddressVO addressVO);

    /**
     * 删除地址
     */
    String deleteAddress(Integer shoppingAddressId);

    /**
     * 更新地址信息
     */
    String updateAddress(AddressVO addressVO);
}