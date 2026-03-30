package com.example.tomatomall.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CheckoutVO {
    private List<Integer> cartItemIds;
    private ShoppingAddress shoppingAddress; // 如果需要，需要单独添加一个PO来存储信息
    private String paymentMethod;
}

class ShoppingAddress { // 可能之后需要单独独立出去作为一个VO类
    private String name;
    private String phone;
    private String zipCode;
    private String address;
}