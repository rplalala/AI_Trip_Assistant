package com.demo.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应格式
 * @param <T> data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiRespond<T> {
    private int code; // 响应码，1 代表成功，0 代表失败
    private String msg; // 提示信息。成功：success；失败：异常信息
    private T data; // 返回的数据

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
