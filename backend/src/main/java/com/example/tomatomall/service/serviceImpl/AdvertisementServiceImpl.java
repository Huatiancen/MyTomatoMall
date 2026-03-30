package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.po.Advertisement;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.repository.AdvertisementRepository;
import com.example.tomatomall.repository.ProductRepository;
import com.example.tomatomall.service.AdvertisementService;
import com.example.tomatomall.vo.AdvertisementVO;
import com.example.tomatomall.vo.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.tomatomall.exception.TomatoMallException;


import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdvertisementServiceImpl implements AdvertisementService {

    @Autowired
    private AdvertisementRepository advertisementRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public Response<List<AdvertisementVO>> getAllAdvertisements() {
        List<Advertisement> advertisements = advertisementRepository.findAll();
        List<AdvertisementVO> advertisementVOs = advertisements.stream()
                .map(Advertisement::toVO)
                .collect(Collectors.toList());
        return Response.buildSuccess(advertisementVOs);
    }

    @Override
    public Response<String> updateAdvertisement(AdvertisementVO advertisementVO) {
        Advertisement advertisement = advertisementRepository.findById(advertisementVO.getId()).get();
        if (!advertisementRepository.findById(advertisementVO.getId()).isPresent()) {
            throw TomatoMallException.advertisementNotExist();
        }
        if(!productRepository.findById(advertisementVO.getProductId()).isPresent()){
            throw TomatoMallException.productNotExist();
        }
        advertisement.setTitle(advertisementVO.getTitle());
        advertisement.setImageUrl(advertisementVO.getImgUrl());
        advertisement.setContent(advertisementVO.getContent());
        advertisement.setProduct(productRepository.findById(advertisementVO.getProductId()).get());
        advertisementRepository.save(advertisement);
        return Response.buildSuccess("更新成功");
    }

    @Override

    public Response<AdvertisementVO> addAdvertisement(AdvertisementVO advertisementVO) {
        System.out.println(advertisementVO.getProductId());
        Product product = productRepository.findById(advertisementVO.getProductId()).orElse(null);
        System.out.println(advertisementVO.getProductId());

        // 创建广告实体并设置属性
        Advertisement advertisement = new Advertisement();
        advertisement.setTitle(advertisementVO.getTitle());
        advertisement.setContent(advertisementVO.getContent());
        advertisement.setImageUrl(advertisementVO.getImgUrl());
        advertisement.setProduct(product);

        // 保存并返回
        advertisement = advertisementRepository.save(advertisement);
        return Response.buildSuccess(advertisement.toVO());
    }

    @Override
    public Response<String> deleteAdvertisement(Integer id) {
        if (!advertisementRepository.findById(id).isPresent()) {
            throw TomatoMallException.advertisementNotExist();
        }
        advertisementRepository.deleteById(id);
        return Response.buildSuccess("删除成功");
    }
}