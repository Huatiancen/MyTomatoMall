package com.example.tomatomall.controller;

import com.example.tomatomall.service.FavoriteService;
import com.example.tomatomall.vo.FavoriteVO;
import com.example.tomatomall.vo.ProductVO;
import com.example.tomatomall.vo.Response;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Resource
    FavoriteService favoriteService;

    @GetMapping
    public Response<List<ProductVO>> getFavorites() {
        return Response.buildSuccess(favoriteService.getFavorites());
    }

    @PostMapping
    public Response<String> addFavorite(@RequestBody FavoriteVO favorite) {
        return Response.buildSuccess(favoriteService.addFavorite(favorite));
    }

    @DeleteMapping("/{productId}")
    public Response<String> deleteFavorite(@PathVariable Integer productId) {
        return Response.buildSuccess(favoriteService.deleteFavorite(productId));
    }

    @GetMapping("/check/{productId}")
    public Response<Boolean> checkFavorite(@PathVariable Integer productId) {
        return Response.buildSuccess(favoriteService.checkFavorite(productId));
    }
}
