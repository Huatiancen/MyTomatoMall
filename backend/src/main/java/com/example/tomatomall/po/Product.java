package com.example.tomatomall.po;

import com.example.tomatomall.vo.ProductVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Basic
    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Basic
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private Double price;

    @Basic
    @Column(name = "rate", nullable = false)
    private Double rate;

    @Basic
    @Column(name = "description", length = 255)
    private String description;

    @Basic
    @Column(name = "cover", length = 500)
    private String cover;

    @Basic
    @Column(name = "detail", length = 500)
    private String detail;

    @ElementCollection
    @CollectionTable(name = "product_tags", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Basic
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Basic
    @Column(name = "sales_volume")
    private Integer salesVolume = 0;

    @Basic
    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    // 在创建对象时自动设置创建时间
    @PrePersist
    protected void onCreate() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
    }

    public ProductVO toVO(){
        ProductVO productVO = new ProductVO();
        productVO.setId(this.id);
        productVO.setTitle(this.title);
        productVO.setPrice(this.price);
        productVO.setRate(this.rate);
        productVO.setDescription(this.description);
        productVO.setCover(this.cover);
        productVO.setDetail(this.detail);
        productVO.setTags(this.tags);
        productVO.setCreateTime(this.createTime);
        productVO.setSalesVolume(this.salesVolume);
        productVO.setStatus(this.status);
        return productVO;
    }

}