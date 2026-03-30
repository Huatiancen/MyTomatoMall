package com.example.tomatomall.vo;

import com.alipay.api.domain.Amount;
import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.repository.AccountRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class OrderVO {
    private Integer orderId;
    private String username;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String status;
    private LocalDateTime createTime;

    public Order toPO(AccountRepository accountRepository) {
        Order order = new Order();
        order.setId(this.orderId);
        order.setUser(accountRepository.findByUsername(username));
        order.setTotalAmount(this.totalAmount);
        order.setPaymentMethod(this.paymentMethod);
        order.setStatus(this.status); //创建时间自动获取，无需设置
        return order;
    }
}
