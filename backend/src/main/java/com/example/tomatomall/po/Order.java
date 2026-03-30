package com.example.tomatomall.po;

import com.example.tomatomall.vo.OrderVO;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Entity(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account user;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CartsOrdersRelation> cartRelations = new HashSet<>();

    @Transient // 不与数据库进行映射，主要方便高层处理
    private Set<Cart> cartItems = new HashSet<>();

    @PostLoad
    private void loadCartItems() {
        cartItems = cartRelations.stream()
                .map(CartsOrdersRelation::getCartItem)
                .collect(Collectors.toSet());
    }

    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
        // 通过cartItems创建CartsOrdersRelation
        for (Cart cartItem : cartItems) {
            CartsOrdersRelation relation = new CartsOrdersRelation(this, cartItem);
            cartRelations.add(relation);
        }
    }

    public OrderVO toVO() {
        OrderVO orderVO = new OrderVO();
        orderVO.setOrderId(this.id);
        orderVO.setUsername(this.user.getName());
        orderVO.setTotalAmount(this.totalAmount);
        orderVO.setPaymentMethod(this.paymentMethod);
        orderVO.setStatus(this.status);
        orderVO.setCreateTime(this.createTime);
        return orderVO;
    }
}