package com.example.tomatomall.vo;

import com.example.tomatomall.po.Favorite;
import com.example.tomatomall.repository.ProductRepository;
import com.example.tomatomall.util.UserContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FavoriteVO {
    private Integer productId;
    private Integer FavoriteId;

    public Favorite toPO(ProductRepository productRepository, UserContext userContext) {
        Favorite favorite = new Favorite();
        favorite.setId(this.FavoriteId);
        favorite.setProduct(productRepository.findById(this.productId).get());
        favorite.setAccount(userContext.getCurrentUser());
        return favorite;
    }
}
