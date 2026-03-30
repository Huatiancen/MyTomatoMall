package com.example.tomatomall.po;

import com.example.tomatomall.vo.AdvertisementVO;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Getter
@Setter
@NoArgsConstructor
@Entity(name = "advertisements")
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "product_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Product product;

    public AdvertisementVO toVO() {
        AdvertisementVO advertisementVO = new AdvertisementVO();
        advertisementVO.setId(this.id);
        advertisementVO.setTitle(this.title);
        advertisementVO.setContent(this.content);
        advertisementVO.setImgUrl(this.imageUrl);
        advertisementVO.setProductId(this.product.getId());
        return advertisementVO;
    }
}