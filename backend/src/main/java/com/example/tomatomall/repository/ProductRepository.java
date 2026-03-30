package com.example.tomatomall.repository;

import com.example.tomatomall.po.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    Product findByTitle(String title);

    @Query(value = "SELECT DISTINCT p.* FROM products p JOIN product_tags pt ON p.id = pt.product_id WHERE pt.tag IN :tags AND p.status = 'ACTIVE'", nativeQuery = true)
    List<Product> findByAnyTagIn(@Param("tags") List<String> tags);

    @Query(value = "SELECT p.* FROM products p WHERE " +
            "(SELECT COUNT(DISTINCT pt.tag) FROM product_tags pt WHERE pt.product_id = p.id AND pt.tag IN :tags) = :tagCount "
            +
            "AND (SELECT COUNT(pt.tag) FROM product_tags pt WHERE pt.product_id = p.id) = :tagCount " +
            "AND p.status = 'ACTIVE'", nativeQuery = true)
    List<Product> findByAllTagsIn(@Param("tags") List<String> tags, @Param("tagCount") Long tagCount);

    @Query(value = "SELECT * FROM products WHERE status = 'ACTIVE' " +
            "ORDER BY create_time DESC " +
            "LIMIT :#{#limit != null ? #limit : 20000}",
            nativeQuery = true)
    List<Product> findTopNByCreateTimeDesc(@Param("limit") Integer limit);

    @Query(value = "SELECT * FROM products WHERE status = 'ACTIVE' " +
            "ORDER BY sales_volume DESC " +
            "LIMIT :#{#limit != null ? #limit : 20000}",
            nativeQuery = true)
    List<Product> findTopNBySalesVolumeDesc(@Param("limit") Integer limit);

    List<Product> findByStatus(String status);
}