package com.example.tomatomall.po;

import com.example.tomatomall.vo.CartVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account user;

    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    @Basic
    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Basic
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    public CartVO toVO() {
        CartVO cartVO = new CartVO();
        cartVO.setCartItemId(this.id);
        cartVO.setProductId(this.product.getId());
        cartVO.setTitle(this.product.getTitle());
        cartVO.setPrice(this.product.getPrice());
        cartVO.setDescription(this.product.getDescription());
        cartVO.setCover(this.product.getCover());
        cartVO.setDetail(this.product.getDetail());
        cartVO.setQuantity(this.quantity);
        cartVO.setProductStatus(this.product.getStatus());
        return cartVO;
    }
}
