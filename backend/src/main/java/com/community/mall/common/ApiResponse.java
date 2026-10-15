package com.community.mall.common;
public record ApiResponse<T>(boolean success, String message, T data) {
  public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true, "ok", data); }
  public static <T> ApiResponse<T> ok() { return ok(null); }
  public static <T> ApiResponse<T> fail(String message) { return new ApiResponse<>(false, message, null); }
}
