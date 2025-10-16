package com.demo.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 格式化响应结果
 * @param <T>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiRespond<T> {
    private int code;
    private String msg;
    private T data;

    public static <Z> ApiRespond<Z> success(){
        ApiRespond<Z> apiRespond = new ApiRespond<>();
        apiRespond.code = 1;
        apiRespond.msg = "success";
        return apiRespond;
    }

    public static <Z> ApiRespond<Z> success(Z data){
        ApiRespond<Z> apiRespond = new ApiRespond<>();
        apiRespond.code = 1;
        apiRespond.msg = "success";
        apiRespond.data = data;
        return apiRespond;
    }

    public static <Z> ApiRespond<Z> error(String msg){
        ApiRespond<Z> apiRespond = new ApiRespond<>();
        apiRespond.code = 0;
        apiRespond.msg = msg;
        return apiRespond;
    }
}
