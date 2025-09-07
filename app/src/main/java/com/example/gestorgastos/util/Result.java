package com.example.gestorgastos.util;

public class Result<T> {
    private final T data;
    private final Exception error;
    private final boolean isSuccess;
    
    private Result(T data, Exception error, boolean isSuccess) {
        this.data = data;
        this.error = error;
        this.isSuccess = isSuccess;
    }
    
    public static <T> Result<T> success(T data) {
        return new Result<>(data, null, true);
    }
    
    public static <T> Result<T> error(Exception error) {
        return new Result<>(null, error, false);
    }
    
    public T getData() {
        return data;
    }
    
    public Exception getError() {
        return error;
    }
    
    public boolean isSuccess() {
        return isSuccess;
    }
    
    public boolean isError() {
        return !isSuccess;
    }
}





