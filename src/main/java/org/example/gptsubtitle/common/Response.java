package org.example.gptsubtitle.common;

import lombok.Data;

@Data
public class Response<T> {
    private T data;
    private String message;
    public boolean success;

    public static <T> Response<T> ok(T data) {
        Response<T> response = new Response<>();
        response.success = true;
        response.message = "OK";
        response.data = data;
        return response;
    }

    public static <T> Response<T> failed(T message) {
        Response<T> response = new Response<>();
        response.message = String.valueOf(message);
        return response;
    }
}
