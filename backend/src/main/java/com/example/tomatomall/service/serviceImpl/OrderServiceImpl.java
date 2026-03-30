package com.example.tomatomall.service.serviceImpl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeCloseRequest;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.example.tomatomall.exception.TomatoMallException;
import com.example.tomatomall.po.*;
import com.example.tomatomall.repository.CartRepository;
import com.example.tomatomall.repository.OrderRepository;
import com.example.tomatomall.repository.ProductRepository;
import com.example.tomatomall.repository.StockpileRepository;
import com.example.tomatomall.service.OrderService;
import com.example.tomatomall.configure.AliPayConfig;
import com.example.tomatomall.util.UserContext;
import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.OrderVO;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    @Autowired
    private final OrderRepository orderRepository;

    @Autowired
    private final StockpileRepository stockpileRepository;

    @Autowired
    private final ProductRepository productRepository;

    @Autowired
    private final AliPayConfig aliPayConfig;

    @Autowired
    private final UserContext userContext;
    
    // 支付宝网关（沙箱环境）
    // private static final String GATEWAY_URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String FORMAT = "json";
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";

    @Autowired
    private CartRepository cartRepository;

    @Override
    @Transactional
    public void completeOrder(String orderId, String alipayTradeNo, String amount) {
        Order order = orderRepository.findById(Integer.parseInt(orderId))
                .orElseThrow(() -> new TomatoMallException("订单不存在"));

        // 检查订单状态，防止重复处理
        if (!"PENDING".equals(order.getStatus())) {
            return;
        }

        // 验证支付金额
        if (!order.getTotalAmount().toString().equals(amount)) {
            throw new TomatoMallException("支付金额不匹配");
        }

        // 扣减库存, 并增加产品销量
        for (Cart cart : order.getCartItems()) {
            Product product = cart.getProduct();
            // 根据商品查询库存
            Stockpile stockpile = stockpileRepository.findByProduct(product);
            // 库存减去购买数量，此时如果正确创建订单，相关商品的库存应该已经冻结，所以这里减去冻结库存
            stockpile.setFrozen(stockpile.getFrozen() - cart.getQuantity());
            // 保存库存
            stockpileRepository.save(stockpile);
            // 更新购物车状态
            cart.setStatus("PAID");
            // 保存购物车
            cartRepository.save(cart);
            // 更新产品销量
            product.setSalesVolume(product.getSalesVolume() + cart.getQuantity());
            // 保存产品
            productRepository.save(product);
        }

        // 更新订单状态
        order.setStatus("SUCCESS");
        // 保存订单
        orderRepository.save(order);
    }

    @Override
    public Map<String, Object> createPaymentForm(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new TomatoMallException("订单不存在"));

        if (!"PENDING".equals(order.getStatus())) {
            throw new TomatoMallException("订单状态不正确");
        }

        // 创建支付宝客户端
        AlipayClient alipayClient = new DefaultAlipayClient(
                aliPayConfig.getServerUrl(),
                aliPayConfig.getAppId(),
                aliPayConfig.getAppPrivateKey(),
                FORMAT,
                CHARSET,
                aliPayConfig.getAlipayPublicKey(),
                SIGN_TYPE);
        
        // 创建支付请求
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(aliPayConfig.getNotifyUrl());
        request.setReturnUrl(aliPayConfig.getReturnUrl());
        
        // 组装请求参数
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", order.getId().toString());
        bizContent.put("total_amount", order.getTotalAmount().toString());
        bizContent.put("subject", "TomatoMall订单-" + order.getId());
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContent.put("timeout_express", "30m");
        request.setBizContent(bizContent.toString());
        
        // 生成表单
        String form;
        try {
            form = alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
            throw new TomatoMallException("生成支付表单失败");
        }

        // 构建返回数据
        Map<String, Object> result = new HashMap<>();
        result.put("paymentForm", form);
        result.put("orderId", order.getId());
        result.put("totalAmount", order.getTotalAmount());
        result.put("paymentMethod", "Alipay");
        return result;
    }

    @Override
    public List<OrderVO> getAllOrders() {
        Account user = userContext.getCurrentUser();
        List<Order> orders = orderRepository.findByUserId(user.getId());
        return orders.stream()
                .map(Order::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderVO getOrderById(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(TomatoMallException::orderNotExist);
        return order.toVO();
    }

    @Override
    public List<CartVO> getOrderCarts(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(TomatoMallException::orderNotExist);
        return order.getCartItems().stream()
                .map(Cart::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public String cancelOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(TomatoMallException::orderNotExist);

        if (!"PENDING".equals(order.getStatus())) {
            throw TomatoMallException.orderStatusWrong();
        }

        // 调用支付宝交易关闭接口
        try {
            closeAlipayTrade(order.getId().toString());
        } catch (Exception e) {
            // 记录日志但不阻止订单取消
            e.printStackTrace();
        }

        // 恢复库存
        for (Cart cart : order.getCartItems()) {
            Stockpile stockpile = stockpileRepository.findByProduct(cart.getProduct());
            if (stockpile != null) {
                stockpile.setFrozen(stockpile.getFrozen() - cart.getQuantity());
                stockpile.setAmount(stockpile.getAmount() + cart.getQuantity());
                stockpileRepository.save(stockpile);
            }
        }

        order.setStatus("EXPIRED");
        orderRepository.save(order);
        return "取消订单成功";
    }

    /**
     * 关闭支付宝交易
     */
    private void closeAlipayTrade(String outTradeNo) throws AlipayApiException {
        AlipayClient alipayClient = new DefaultAlipayClient(
                aliPayConfig.getServerUrl(),
                aliPayConfig.getAppId(),
                aliPayConfig.getAppPrivateKey(),
                FORMAT,
                CHARSET,
                aliPayConfig.getAlipayPublicKey(),
                SIGN_TYPE);

        AlipayTradeCloseRequest request = new AlipayTradeCloseRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", outTradeNo);
        request.setBizContent(bizContent.toString());

        try {
            alipayClient.execute(request);
        } catch (AlipayApiException e) {
            // 如果是"交易不存在"的错误，则忽略它（因为这是预期内的情况）
            if ("40004".equals(e.getErrCode()) && (e.getErrMsg().contains("ACQ.TRADE_NOT_EXIST") || e.getErrMsg().contains("交易不存在"))) {
                return; // 交易不存在属于正常情况，直接返回
            }
            // 其它错误则继续抛出
            throw e;
        }
    }

    @Override
    public String deleteOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(TomatoMallException::orderNotExist);
        if ("PENDING".equals(order.getStatus())) {
            throw TomatoMallException.orderStatusWrong();
        }
        orderRepository.delete(order);
        return "删除订单成功";
    }
} 