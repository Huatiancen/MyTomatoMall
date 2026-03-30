package com.example.tomatomall.util;

import com.example.tomatomall.po.Order;
import com.example.tomatomall.po.Cart;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.repository.OrderRepository;
import com.example.tomatomall.repository.StockpileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ScheduledTasks {

    private final OrderRepository orderRepository;
    private final StockpileRepository stockpileRepository;

    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Transactional
    public void unfreezeExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> pendingOrders = orderRepository.findByStatus("PENDING");
        
        for (Order order : pendingOrders) {
            // 计算订单创建时间到现在的时间差（分钟）
            long minutesSinceCreation = ChronoUnit.MINUTES.between(order.getCreateTime(), now);
            
            // 如果超过30分钟，则处理过期订单
            if (minutesSinceCreation >= 30) {
                Set<Cart> cartItems = order.getCartItems();
                for (Cart cart : cartItems) {
                    Stockpile stockpile = stockpileRepository.findByProduct(cart.getProduct());
                    if (stockpile != null) {
                        // 解冻库存
                        stockpile.setAmount(stockpile.getAmount() + cart.getQuantity());
                        stockpile.setFrozen(stockpile.getFrozen() - cart.getQuantity());
                        stockpileRepository.save(stockpile);
                    }
                }
                order.setStatus("EXPIRED");
                orderRepository.save(order);
            }
        }
    }
} 