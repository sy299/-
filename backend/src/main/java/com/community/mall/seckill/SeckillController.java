package com.community.mall.seckill;

import com.community.mall.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {
  private final SeckillService service;

  public SeckillController(SeckillService service) {
    this.service = service;
  }

  public record Purchase(@NotBlank String receiverName, @NotBlank String receiverPhone, @NotBlank String receiverAddress) {}

  @GetMapping("/activities")
  public ApiResponse<?> activities() {
    return ApiResponse.ok(service.listActivities());
  }

  @PreAuthorize("hasRole('USER')")
  @PostMapping("/activities/{activityId}/purchase")
  public ApiResponse<?> purchase(@PathVariable Long activityId, @Valid @RequestBody Purchase input, Principal principal) {
    return ApiResponse.ok(service.purchase(activityId, Long.valueOf(principal.getName()), input));
  }
}
