package com.example.tomatomall.controller;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.example.tomatomall.configure.AliPayConfig;
import com.example.tomatomall.exception.TomatoMallException;
import com.example.tomatomall.po.Cart;
import com.example.tomatomall.po.Order;
import com.example.tomatomall.repository.OrderRepository;
import com.example.tomatomall.service.OrderService;
import com.example.tomatomall.vo.CartVO;
import com.example.tomatomall.vo.OrderVO;
import com.example.tomatomall.vo.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@Transactional(rollbackFor = Exception.class)
public class OrderController {
    @Resource
    private AliPayConfig aliPayConfig;

    @Autowired
    private OrderRepository orderRepository;

    @Resource
    private OrderService orderService;

    // 支付宝网关（沙箱环境）
    private static final String GATEWAY_URL = "https://openapi.alipaydev.com/gateway.do";
    private static final String FORMAT = "json";
    private static final String CHARSET = "UTF-8";
    private static final String SIGN_TYPE = "RSA2";

    @PostMapping("/{order_id}/pay")
    public Response<Map<String, Object>> createPayment(@PathVariable("order_id") Integer orderId) {
        return Response.buildSuccess(orderService.createPaymentForm(orderId));
    }

    @PostMapping("/notify")
    public void handleAlipayNotify(HttpServletRequest request, HttpServletResponse response) throws IOException, AlipayApiException {
        // 1. 解析支付宝回调参数（通常是 application/x-www-form-urlencoded）
        Map<String, String> params = request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));

        // 2. 验证支付宝签名（防止伪造请求）
        boolean signVerified = AlipaySignature.rsaCheckV1(params, aliPayConfig.getAlipayPublicKey(), "UTF-8", "RSA2");
        if (!signVerified) {
            response.getWriter().print("fail"); // 签名验证失败，返回 fail
            return;
        }

        // 3. 处理业务逻辑（更新订单、减库存等）
        String tradeStatus = params.get("trade_status");
        if ("TRADE_SUCCESS".equals(tradeStatus)) {
            String orderId = params.get("out_trade_no"); // 您的订单号
            String alipayTradeNo = params.get("trade_no"); // 支付宝交易号
            String amount = params.get("total_amount"); // 支付金额

            // 更新订单状态（注意幂等性，防止重复处理）, 扣减库存
            orderService.completeOrder(orderId, alipayTradeNo, amount);
        }

        // 4. 必须返回纯文本的 "success"（支付宝要求）
        response.getWriter().print("success");
    }

    @GetMapping
    public Response<List<OrderVO>> getOrders() {
        return Response.buildSuccess(orderService.getAllOrders());
    }

    @GetMapping("/{orderId}")
    public Response<OrderVO> getOrder(@PathVariable Integer orderId) {
        return Response.buildSuccess(orderService.getOrderById(orderId));
    }

    @GetMapping("/{orderId}/cart")
    public Response<List<CartVO>> getOrderCarts(@PathVariable Integer orderId) {
        return Response.buildSuccess(orderService.getOrderCarts(orderId));
    }

    @DeleteMapping("/{orderId}")
    public Response<String> deleteOrder(@PathVariable Integer orderId) {
        return Response.buildSuccess(orderService.deleteOrder(orderId));
    }

    @PutMapping("/{orderId}")
    public Response<String> cancelOrder(@PathVariable Integer orderId) {
        return Response.buildSuccess(orderService.cancelOrder(orderId));
    }
}
