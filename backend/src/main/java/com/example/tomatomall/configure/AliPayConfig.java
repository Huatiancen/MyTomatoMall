package com.example.tomatomall.configure;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Data
@Component
@ConfigurationProperties(prefix = "alipay")
public class AliPayConfig {
    private String appId;
    private String appPrivateKey;
    private String alipayPublicKey;
    private String notifyUrl;
    private String serverUrl;
    private String returnUrl;


    @PostConstruct
    public void init() {
        // 设置参数（全局只需设置一次）
        Config config = new Config();
        config.protocol = "https";
        config.gatewayHost = "openapi.alipaydev.com";
        config.signType = "RSA2";
        config.appId = this.appId;
        config.merchantPrivateKey = this.appPrivateKey;
        config.alipayPublicKey = this.alipayPublicKey;
        config.notifyUrl = this.notifyUrl;
        
        // 打印配置信息
        System.out.println("支付宝配置信息：");
        System.out.println("网关：https://" + config.gatewayHost + "/gateway.do");
        System.out.println("应用ID：" + config.appId);
        System.out.println("通知地址：" + config.notifyUrl);
        System.out.println("系统时间：" + new java.util.Date());
        Factory.setOptions(config);
        System.out.println("=======支付宝SDK初始化成功=======");
    }
}
