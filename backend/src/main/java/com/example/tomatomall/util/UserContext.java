package com.example.tomatomall.util;

import com.example.tomatomall.po.Account;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Component
public class UserContext {
    public Account getCurrentUser() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes()).getRequest();
            return (Account) request.getSession().getAttribute("currentUser");
        } catch (Exception e) {
            return null; // 在非HTTP请求上下文中返回null
        }
    }
}
