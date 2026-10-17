package com.community.mall.common;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  @ExceptionHandler(BusinessException.class) ResponseEntity<ApiResponse<Void>> business(BusinessException e){ return ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage())); }
  @ExceptionHandler(TooManyRequestsException.class) ResponseEntity<ApiResponse<Void>> busy(TooManyRequestsException e){ return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.fail(e.getMessage())); }
  @ExceptionHandler({PessimisticLockingFailureException.class, QueryTimeoutException.class, CannotGetJdbcConnectionException.class, TransactionTimedOutException.class})
  ResponseEntity<ApiResponse<Void>> overloaded(Exception e){ return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(ApiResponse.fail("当前抢购请求较多，请稍后重试")); }
  @ExceptionHandler({RedisConnectionFailureException.class, RedisSystemException.class})
  ResponseEntity<ApiResponse<Void>> redisUnavailable(Exception e){ return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ApiResponse.fail("秒杀缓存服务暂不可用，请稍后重试")); }
  @ExceptionHandler(Exception.class) ResponseEntity<ApiResponse<Void>> other(Exception e){
    log.error("Unhandled API exception", e);
    return ResponseEntity.internalServerError().body(ApiResponse.fail("服务器暂时无法处理请求"));
  }
}
