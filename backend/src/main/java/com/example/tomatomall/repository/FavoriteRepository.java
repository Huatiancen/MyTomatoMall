package com.example.tomatomall.repository;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.po.Favorite;
import com.example.tomatomall.po.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {
    Favorite findByAccountAndProduct(Account currentUser, Product product);

    List<Favorite> findAllByAccount(Account currentUser);
}
