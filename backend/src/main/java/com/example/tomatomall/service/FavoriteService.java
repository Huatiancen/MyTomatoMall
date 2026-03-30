package com.example.tomatomall.service;

import com.example.tomatomall.vo.FavoriteVO;
import com.example.tomatomall.vo.ProductVO;
import com.example.tomatomall.vo.Response;

import java.util.List;

public interface FavoriteService {
    public List<ProductVO> getFavorites();

    public String addFavorite(FavoriteVO favorite);

    public String deleteFavorite(Integer productId);

    public Boolean checkFavorite(Integer productId);

}
