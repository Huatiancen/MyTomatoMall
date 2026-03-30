package com.example.tomatomall.vo;

import com.example.tomatomall.po.Product;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductVO {
    private Integer id;
    private String title;
    private Double price;
    private Double rate;
    private String description;
    private String cover;
    private String detail;
    private List<SpecificationVO> specifications;
    private List<String> tags;
    private Integer salesVolume = 0;
    private LocalDateTime createTime;
    private String status;

    public Product toPO() {
        Product product = new Product();
        product.setId(this.id);
        product.setTitle(this.title);
        product.setPrice(this.price);
        product.setRate(this.rate);
        product.setDescription(this.description);
        product.setCover(this.cover);
        product.setDetail(this.detail);
        product.setTags(this.tags);
        product.setSalesVolume(this.salesVolume);
        product.setCreateTime(this.createTime);
        product.setStatus(status);
        return product;
    }
}