package com.example.tomatomall.service;

import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.CartsVO;
import com.example.tomatomall.vo.CheckoutVO;
import com.example.tomatomall.vo.OrderVO;

public interface CartService {
    public CartVO addCart(CartVO cart);
    public String deleteCart(Integer cart_item_id);
    public String updateCart(Integer cart_item_id, Integer quantity);
    public CartsVO getCarts(); // 返回一个数组，包含所有的购物车商品
    public OrderVO checkout(CheckoutVO checkoutVO); // 结算购物车中的商品
}
