package com.example.tomatomall.service;

import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.OrderVO;

import java.util.List;
import java.util.Map;

public interface OrderService {
    void completeOrder(String orderId, String alipayTradeNo, String amount);
    Map<String, Object> createPaymentForm(Integer orderId);
    List<OrderVO> getAllOrders();
    OrderVO getOrderById(Integer orderId);
    List<CartVO> getOrderCarts(Integer orderId);
    String deleteOrder(Integer orderId);
    String cancelOrder(Integer orderId);

} 