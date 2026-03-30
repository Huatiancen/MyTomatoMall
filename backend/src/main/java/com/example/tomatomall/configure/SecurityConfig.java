package com.example.tomatomall.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 返回 BCryptPasswordEncoder 实例
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()  // 禁用CSRF保护
            .authorizeRequests()
            .anyRequest().permitAll()  // 暂时允许所有请求通过，让自定义拦截器处理权限
            .and()
            .formLogin().disable()  // 禁用表单登录
            .httpBasic().disable();  // 禁用HTTP基本认证

        return http.build();
    }
}
