package com.example.tomatomall.service;

import com.example.tomatomall.vo.AdvertisementVO;
import com.example.tomatomall.vo.Response;

import java.util.List;

public interface AdvertisementService {
    Response<List<AdvertisementVO>> getAllAdvertisements();

    Response<String> updateAdvertisement(AdvertisementVO advertisementVO);

    Response<AdvertisementVO> addAdvertisement(AdvertisementVO advertisementVO);

    Response<String> deleteAdvertisement(Integer id);
}