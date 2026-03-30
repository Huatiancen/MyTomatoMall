package com.example.tomatomall.controller;


import com.example.tomatomall.service.AdvertisementService;
import com.example.tomatomall.vo.AdvertisementVO;
import com.example.tomatomall.vo.Response;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/advertisements")
public class AdvertisementController {

    @Resource
    AdvertisementService advertisementService;

    @GetMapping()
    public Response<List<AdvertisementVO>> getAllAdvertisements() {
        return advertisementService.getAllAdvertisements();
    }

    @PutMapping()
    public Response<String> updateAdvertisement(@RequestBody AdvertisementVO advertisementVO) {
        return advertisementService.updateAdvertisement(advertisementVO);
    }

    @PostMapping()
    public Response<AdvertisementVO> addAdvertisement(@RequestBody AdvertisementVO advertisementVO) {
        return advertisementService.addAdvertisement(advertisementVO);
    }

    @DeleteMapping("/{id}")
    public Response<String> deleteAdvertisement(@PathVariable Integer id) {
        return advertisementService.deleteAdvertisement(id);
    }

}
