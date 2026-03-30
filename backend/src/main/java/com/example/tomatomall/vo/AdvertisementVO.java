package com.example.tomatomall.vo;

import com.example.tomatomall.po.Advertisement;
import com.example.tomatomall.repository.ProductRepository;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdvertisementVO {
    private Integer id;
    private String title;
    private String content;
    private String ImgUrl;
    private Integer productId;

    public Advertisement toPO(ProductRepository productRepository) {
        Advertisement advertisement = new Advertisement();
        advertisement.setId(this.id);
        advertisement.setTitle(this.title);
        advertisement.setContent(this.content);
        advertisement.setImageUrl(this.ImgUrl);
        advertisement.setProduct(productRepository.findById(this.productId).get());
        return advertisement;
    }


}