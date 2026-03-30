package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.exception.TomatoMallException;
import com.example.tomatomall.po.Cart;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.po.Product;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.repository.*;
import com.example.tomatomall.service.CartService;
import com.example.tomatomall.util.UserContext;
import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.CartsVO;
import com.example.tomatomall.vo.CheckoutVO;
import com.example.tomatomall.vo.OrderVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    CartRepository cartRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    StockpileRepository stockpileRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    UserContext userContext;

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public CartVO addCart(CartVO cart) { // 这里理论上只会有product_id和quantity
        Product product = productRepository.findById(cart.getProductId()).orElse(null);
        if (product == null) {
            throw TomatoMallException.productNotExist();
        }
        // 检查是否有用户和商品相同的购物车商品，如果有，则报错
        Cart existingCart = cartRepository.findByUserAndProductAndStatus(userContext.getCurrentUser(), product, "ACTIVE");
        if (existingCart != null) {
            throw TomatoMallException.cartAlreadyExist();
        }
        Stockpile stockpile = stockpileRepository.findByProduct(product);
        if (stockpile == null) {
            throw TomatoMallException.productStockNotExist();
        }
        if (stockpile.getAmount() < cart.getQuantity()) {
            throw TomatoMallException.productStockNotEnough();
        }
        cart.setTitle(product.getTitle());
        cart.setPrice(product.getPrice());
        cart.setDescription(product.getDescription());
        cart.setCover(product.getCover());
        cart.setDetail(product.getDetail());
        Cart cartPO = cartRepository.save(cart.toPO(productRepository, userContext));
        return cartPO.toVO();
    }

    @Override
    public String deleteCart(Integer cart_item_id) {
        Cart cart = cartRepository.findById(cart_item_id).orElse(null);
        if (cart == null) {
            throw TomatoMallException.cartNotExist();
        }
        cartRepository.delete(cart);
        return "删除成功";
    }

    @Override
    public String updateCart(Integer cart_item_id, Integer quantity) {
        Cart cart = cartRepository.findById(cart_item_id).orElse(null);
        if (cart == null) {
            throw TomatoMallException.cartNotExist();
        }
        Product product = productRepository.findById(cart.getProduct().getId()).orElse(null);
        if (product == null) {
            throw TomatoMallException.cartNotExist();
        }
        Stockpile stockpile = stockpileRepository.findByProduct(product);
        if (stockpile == null) {
            throw TomatoMallException.productStockNotExist();
        }
        if (stockpile.getAmount() < quantity) {
            throw TomatoMallException.productStockNotEnough();
        }
        cart.setQuantity(quantity);
        cartRepository.save(cart);
        return "修改数量成功";
    }

    @Override
    public CartsVO getCarts() { // 返回一个数组，包含所有的购物车商品
        List<Cart> carts_list = cartRepository.findAllByUserAndStatus(userContext.getCurrentUser(), "ACTIVE");
        if (carts_list.isEmpty()) {
            return new CartsVO(new CartVO[0]);
        }
        return new CartsVO(carts_list.stream().map(Cart::toVO).toArray(CartVO[]::new));
    }

    @Override
    public OrderVO checkout(CheckoutVO checkoutVO) {
        List<Cart> carts = cartRepository.findAllById(checkoutVO.getCartItemIds());
        if (carts.isEmpty()) {
            throw TomatoMallException.cartNotExist();
        }
        double totalAmount = 0.0;
        for (Cart cart : carts) {
            Product product = productRepository.findById(cart.getProduct().getId()).orElse(null);
            if (product == null) {
                throw TomatoMallException.productNotExist();
            }
            Stockpile stockpile = stockpileRepository.findByProduct(product);
            if (stockpile == null) {
                throw TomatoMallException.productStockNotExist();
            }
            if (stockpile.getAmount() < cart.getQuantity()) {
                throw TomatoMallException.productStockNotEnough();
            }
            totalAmount += cart.getQuantity() * product.getPrice();
        }
        // 创建订单
        OrderVO orderVO = new OrderVO();
        orderVO.setTotalAmount(new BigDecimal(totalAmount));
        orderVO.setStatus("PENDING");
        orderVO.setUsername(userContext.getCurrentUser().getUsername());
        orderVO.setPaymentMethod("ALIPAY");

        Order order = orderVO.toPO(accountRepository);
        order.getCartItems().addAll(carts);
        orderRepository.save(order); // 创建订单完成，自动创建关联
        // 接下来，冻结所选商品库存
        for (Cart cart : carts) {
            Product product = productRepository.findById(cart.getProduct().getId()).orElse(null);
            Stockpile stockpile = stockpileRepository.findByProduct(product);
            stockpile.setAmount(stockpile.getAmount() - cart.getQuantity());
            stockpile.setFrozen(cart.getQuantity());
            stockpileRepository.save(stockpile);
        }
        return order.toVO();
    }
}
