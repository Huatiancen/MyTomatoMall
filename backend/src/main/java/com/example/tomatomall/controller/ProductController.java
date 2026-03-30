package com.example.tomatomall.controller;

import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.service.ProductService;
import com.example.tomatomall.vo.ProductVO;
import com.example.tomatomall.vo.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Resource
    ProductService productService;

    @GetMapping()
    public Response<List<ProductVO>> getProducts() {
        return Response.buildSuccess(productService.getAllProductsWithSpecifications());
    }
    @GetMapping("/{id}")
    public Response<ProductVO> getProduct(@PathVariable String id) {
        return Response.buildSuccess(productService.getSpecificProductWithSpecifications(id));
    }

    @PutMapping()
    public Response<String> updateProduct(@RequestBody ProductVO productVO) {
        return Response.buildSuccess(productService.updateProduct(productVO));
    }

    @PostMapping()
    public Response<ProductVO> createProduct(@RequestBody ProductVO productVO) {
        return Response.buildSuccess(productService.createProduct(productVO));
    }

    @DeleteMapping("/{id}")
    public Response<String> deleteProduct(@PathVariable String id) {
        return Response.buildSuccess(productService.deleteProduct(id));
    }

    @PatchMapping("/stockpile/{productId}")
    public Response<String> updateStockpile(@PathVariable String productId, @RequestBody Stockpile stockpile) {
        return Response.buildSuccess(productService.updateStockpile(productId, stockpile));
    }

    @GetMapping("/stockpile/{productId}")
    public Response<Stockpile> getStockpile(@PathVariable String productId) {
        return Response.buildSuccess(productService.getStockpile(productId));
    }

    @GetMapping("/{productId}/tags")
    public Response<List<String>> getProductTags(@PathVariable String productId) {
        return Response.buildSuccess(productService.getProductTags(productId));
    }

    @PutMapping("/{productId}/tags")
    public Response<String> updateProductTags(@PathVariable String productId, @RequestBody ProductVO productVO) {
        return Response.buildSuccess(productService.updateProductTags(productId, productVO.getTags()));
    }

    @GetMapping("/filter")
    public Response<List<ProductVO>> filterProducts(@RequestBody ProductVO productVO) {
        return Response.buildSuccess(productService.filterProducts(productVO.getTags()));
    }

    @GetMapping("/ranking/latest")
    public Response<List<ProductVO>> getLatestProducts(@RequestParam(required = false) Integer count) {
        return Response.buildSuccess(productService.getLatestProducts(count));
    }

    @GetMapping("/ranking/sales")
    public Response<List<ProductVO>> getTopRatedProducts(@RequestParam(required = false) Integer count) {
        return Response.buildSuccess(productService.getTopRatedProducts(count));
    }

    @GetMapping("/ranking/recommend")
    public Response<List<ProductVO>> getRecommendedProducts(@RequestParam(required = true) Integer count) {
        return Response.buildSuccess(productService.getRecommendedProducts(count));
    }

    @PutMapping("/{productId}/publish")
    public Response<String> publishProduct(@PathVariable String productId) {
        return Response.buildSuccess(productService.publishProduct(productId));
    }
}
