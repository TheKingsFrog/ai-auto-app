package com.org.aiautoproject.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.org.aiautoproject.base.BaseResponse;
import com.org.aiautoproject.utils.ResultUtils;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.org.aiautoproject.controller")
public class GlobalResHandler implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> type = returnType.getParameterType();

        // 已包装 / 文件流 / 二进制 / 无返回值：放行，不处理
        if (BaseResponse.class.isAssignableFrom(type)
                || Resource.class.isAssignableFrom(type)
                || byte[].class == type
                || void.class == type
                || Void.class == type) {
            return false;
        }

        return true;
    }

    @Override
    public @Nullable Object beforeBodyWrite(@Nullable Object body,
                                            MethodParameter returnType,
                                            MediaType selectedContentType,
                                            Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                            ServerHttpRequest request,
                                            ServerHttpResponse response) {

        // 1. 幂等：@ExceptionHandler 或业务里已经 return Result 的，原样放行
        if (body instanceof BaseResponse<?>) {
            return body;
        }

        // 2. String 返回值只能由 StringHttpMessageConverter 写出，
        //    这里手动序列化成 JSON 字符串返回，否则 StringHttpMessageConverter
        //    会尝试把 BaseResponse 强转为 String 导致 ClassCastException
        if (body instanceof CharSequence
                || StringHttpMessageConverter.class.isAssignableFrom(selectedConverterType)) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            return ResultUtils.toJsonString(ResultUtils.success(body));
        }

        // 3. 普通对象 / null
        return ResultUtils.success(body);
    }
}