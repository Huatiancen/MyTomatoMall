package com.example.tomatomall.repository;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Cart;
import com.example.tomatomall.po.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Integer> {
    // 根据USER信息查询user_id符合条件的所有购物车商品信息
    List<Cart> findAllByUserAndStatus(Account user, String status);
    // 根据USER信息和商品ID查询购物车商品信息
    Cart findByUserAndProductAndStatus(Account user, Product product, String status);
    List<Cart> findByUser(Account user);
}
