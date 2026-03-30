package com.example.tomatomall.controller;

import com.example.tomatomall.service.CartService;
import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.CartsVO;
import com.example.tomatomall.vo.CheckoutVO;
import com.example.tomatomall.vo.OrderVO;
import com.example.tomatomall.vo.Response;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    @Resource
    CartService cartService;

    @PostMapping
    public Response<CartVO> addCart(@RequestBody CartVO cart) {
        return Response.buildSuccess(cartService.addCart(cart));
    }

    @DeleteMapping("/{cart_item_id}")
    public Response<String> deleteCart(@PathVariable Integer cart_item_id) {
        return Response.buildSuccess(cartService.deleteCart(cart_item_id));
    }

    @PatchMapping("/{cart_item_id}")
    public Response<String> updateCart(@PathVariable Integer cart_item_id, @RequestBody CartVO cartVO) {
        return Response.buildSuccess(cartService.updateCart(cart_item_id, cartVO.getQuantity()));
    }

    @GetMapping()
    public Response<CartsVO> getCart() {
        return Response.buildSuccess(cartService.getCarts());
    }

    @PostMapping("/checkout")
    public Response<OrderVO> checkout(@RequestBody CheckoutVO checkoutVO) {
        return Response.buildSuccess(cartService.checkout(checkoutVO));
    }
}
