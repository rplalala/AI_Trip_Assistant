package com.demo.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified response format
 * @param <T> data
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiRespond<T> {
    private int code; // Response code: 1 means success, 0 means failure
    private String msg; // Message. Success: success; Failure: exception message
    private T data; // Returned data

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
