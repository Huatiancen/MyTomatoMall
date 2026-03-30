package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.exception.TomatoMallException;
import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Favorite;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.repository.AccountRepository;
import com.example.tomatomall.repository.FavoriteRepository;
import com.example.tomatomall.repository.ProductRepository;
import com.example.tomatomall.service.FavoriteService;
import com.example.tomatomall.util.UserContext;
import com.example.tomatomall.vo.FavoriteVO;
import com.example.tomatomall.vo.ProductVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl implements FavoriteService {


    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserContext userContext;

    @Override
    public String addFavorite(FavoriteVO favorite) {
        Account currentUser = userContext.getCurrentUser();
        if (currentUser == null) {
            throw TomatoMallException.notLogin();
        }
        Product product = productRepository.findById(favorite.getProductId())
                .orElseThrow(() -> TomatoMallException.productNotExist());
        Favorite existingFavorite = favoriteRepository.findByAccountAndProduct(currentUser, product);
        if (existingFavorite != null) {
            return "Product already in favorites";
        }
        Favorite newFavorite = new Favorite();
        newFavorite.setAccount(currentUser);
        newFavorite.setProduct(product);
        favoriteRepository.save(newFavorite);
        return "Product added to favorites";
    }

    @Override
    public String deleteFavorite(Integer productId) {
        Account currentUser = userContext.getCurrentUser();
        if (currentUser == null) {
            throw TomatoMallException.notLogin();
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> TomatoMallException.productNotExist());

        Favorite favorite = favoriteRepository.findByAccountAndProduct(currentUser, product);
        if (favorite == null) {
            return "Product not in favorites";
        }

        favoriteRepository.delete(favorite);
        return "Product removed from favorites";
    }

    @Override
    public Boolean checkFavorite(Integer productId) {
        Account currentUser = userContext.getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return false;
        }

        return favoriteRepository.findByAccountAndProduct(currentUser, product) != null;
    }

    @Override
    public List<ProductVO> getFavorites() {
        Account currentUser = userContext.getCurrentUser();
        if (currentUser == null) {
            throw TomatoMallException.notLogin();
        }

        List<Favorite> favorites = favoriteRepository.findAllByAccount(currentUser);
        return favorites.stream()
                .map(favorite -> favorite.getProduct().toVO())
                .collect(Collectors.toList());
    }
}