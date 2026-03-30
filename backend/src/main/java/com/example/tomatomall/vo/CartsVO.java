package com.example.tomatomall.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CartsVO {
    private CartVO[] items;
    private int total;
    private double totalAmount; // 购物车总金额, 保留两位小数

    public CartsVO(CartVO[] items) {
        this.items = items;
        this.total = items.length;
        this.totalAmount = 0.0;
        for (CartVO item : items) {
            this.totalAmount += item.getQuantity() * item.getPrice();
        }
    }
}
