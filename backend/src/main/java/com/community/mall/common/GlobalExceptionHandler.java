package com.community.mall.common;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BusinessException.class) ResponseEntity<ApiResponse<Void>> business(BusinessException e){ return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage())); }
  @ExceptionHandler(TooManyRequestsException.class) ResponseEntity<ApiResponse<Void>> busy(TooManyRequestsException e){ return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.fail(e.getMessage())); }
  @ExceptionHandler({PessimisticLockingFailureException.class, QueryTimeoutException.class, CannotGetJdbcConnectionException.class, TransactionTimedOutException.class})
  ResponseEntity<ApiResponse<Void>> overloaded(Exception e){ return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.fail("当前抢购请求较多，请稍后重试")); }
  @ExceptionHandler(Exception.class) ResponseEntity<ApiResponse<Void>> other(Exception e){ return ResponseEntity.internalServerError().body(ApiResponse.fail("服务器暂时无法处理请求")); }
}
