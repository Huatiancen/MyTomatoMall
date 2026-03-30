package com.example.tomatomall.exception;

public class TomatoMallException extends RuntimeException {
    public TomatoMallException(String message) {
        super(message);
    }
    public static TomatoMallException userNotExist() {
        return new TomatoMallException("用户不存在");
    }
    public static TomatoMallException userAlreadyExist() {
        return new TomatoMallException("用户名已存在");
    }
    public static TomatoMallException passwordError() {
        return new TomatoMallException("用户密码错误");
    }
    public static TomatoMallException notLogin() {
        return new TomatoMallException("用户未登录");
    }
    public static TomatoMallException fileUploadFail() {
        return new TomatoMallException("文件上传失败");
    }
    public static TomatoMallException productAlreadyExist() {
        return new TomatoMallException("商品已存在");
    }
    public static TomatoMallException productNotExist() {
        return new TomatoMallException("商品不存在");
    }
    public static TomatoMallException productStockNotEnough() {return new TomatoMallException("商品库存不足");}
    public static TomatoMallException productStockNotExist() {return new TomatoMallException("商品库存不存在");}
    public static TomatoMallException cartNotExist() {return new TomatoMallException("购物车商品不存在");}
    public static TomatoMallException cartAlreadyExist() {return new TomatoMallException("购物车商品已存在");}
    public static TomatoMallException cartsEmpty() {return new TomatoMallException("购物车为空");}
    public static TomatoMallException priceNotMatch() {return new TomatoMallException("支付金额不匹配");}
    public static TomatoMallException advertisementNotExist() {
        return new TomatoMallException("广告不存在");
    }
    public static TomatoMallException tagsTooLong() { return new TomatoMallException("传入标签过多");}
    public static TomatoMallException orderNotExist() {return new TomatoMallException("订单不存在");}
    public static TomatoMallException orderStatusWrong() {return new TomatoMallException("订单状态不正确");}
}