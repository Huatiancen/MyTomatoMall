package com.example.tomatomall.service;

import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.vo.ProductVO;

import java.util.List;

public interface ProductService {
    public List<ProductVO> getAllProductsWithSpecifications();
    public ProductVO getSpecificProductWithSpecifications(String id);
    public String updateProduct(ProductVO productVO);
    public ProductVO createProduct(ProductVO productVO);
    public String deleteProduct(String id);
    public String updateStockpile(String productId, Stockpile stockpile);
    public Stockpile getStockpile(String productId);
    public List<String> getProductTags(String productId);
    public String updateProductTags(String productId, List<String> tags);
    public List<ProductVO> filterProducts(List<String> tags);
    public List<ProductVO> getLatestProducts(Integer limit);
    public List<ProductVO> getTopRatedProducts(Integer limit);
    public String publishProduct(String productId);
    public List<ProductVO> getRecommendedProducts(Integer limit);
}
