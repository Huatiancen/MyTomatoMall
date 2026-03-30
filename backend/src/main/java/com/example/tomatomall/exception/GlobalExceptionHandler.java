package com.example.tomatomall.exception;

import com.example.tomatomall.vo.Response;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(TomatoMallException.class)
    public Response<String> handleTomatoMallException(TomatoMallException e) {
        e.printStackTrace();
        if(e.getMessage().equals("用户未登录")) {
            return Response.buildFailure(e.getMessage(), "401"); // 401 Unauthorized
        }
        return Response.buildFailure(e.getMessage(), "400"); // 400 Bad Request
    }
}
