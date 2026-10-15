package com.community.mall.seckill;

import com.community.mall.common.ApiResponse;
import com.community.mall.common.TooManyRequestsException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {
  private static final int LOCK_STRIPES = 256;
  private final SeckillService service;
  private final Semaphore permits;
  private final ReentrantLock[] activityLocks = new ReentrantLock[LOCK_STRIPES];

  public SeckillController(SeckillService service, @Value("${mall.seckill.max-concurrency:24}") int maxConcurrency) {
    this.service = service;
    this.permits = new Semaphore(Math.max(1, maxConcurrency));
    for (int i = 0; i < activityLocks.length; i++) activityLocks[i] = new ReentrantLock();
  }

  public record Purchase(@NotBlank String receiverName, @NotBlank String receiverPhone, @NotBlank String receiverAddress) {}

  @GetMapping("/activities")
  public ApiResponse<?> activities() {
    return ApiResponse.ok(service.listActivities());
  }

  @PreAuthorize("hasRole('USER')")
  @PostMapping("/activities/{activityId}/purchase")
  public ApiResponse<?> purchase(@PathVariable Long activityId, @Valid @RequestBody Purchase input, Principal principal) {
    if (!permits.tryAcquire()) throw new TooManyRequestsException("当前抢购请求较多，请稍后重试");
    ReentrantLock lock = activityLocks[Math.floorMod(activityId.hashCode(), LOCK_STRIPES)];
    try {
      if (!lock.tryLock()) throw new TooManyRequestsException("该场次正在处理中，请稍后重试");
      try {
        return ApiResponse.ok(service.purchase(activityId, Long.valueOf(principal.getName()), input));
      } finally {
        lock.unlock();
      }
    } finally {
      permits.release();
    }
  }
}
