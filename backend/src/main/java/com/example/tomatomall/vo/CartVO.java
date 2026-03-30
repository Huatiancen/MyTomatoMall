package com.example.tomatomall.vo;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Cart;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.repository.AccountRepository;
import com.example.tomatomall.repository.ProductRepository;
import com.example.tomatomall.util.UserContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
@Setter
@NoArgsConstructor
public class CartVO {
    private Integer cartItemId;
    private Integer productId;
    private String title;
    private Double price;
    private String description;
    private String cover;
    private String detail;
    private Integer quantity;
    private String productStatus;

    public Cart toPO(ProductRepository productRepository, UserContext userContext) {
        Cart cart = new Cart();
        cart.setId(this.cartItemId);
        cart.setUser(userContext.getCurrentUser());
        cart.setProduct(productRepository.findById(this.productId).get());
        cart.setQuantity(this.quantity);
        return cart;
    }
}
