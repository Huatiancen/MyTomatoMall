package com.example.tomatomall.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "carts_orders_relation")
public class CartsOrdersRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id", referencedColumnName = "order_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "cart_item_id", referencedColumnName = "cart_item_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Cart cartItem;

    public CartsOrdersRelation(Order order, Cart cartItem) {
        this.order = order;
        this.cartItem = cartItem;
    }
} 