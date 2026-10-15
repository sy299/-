package com.community.mall.payment;

import com.community.mall.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
  private final PaymentService service;

  public PaymentController(PaymentService service) {
    this.service = service;
  }

  public record Create(@NotBlank String provider) {}

  @GetMapping("/options")
  @PreAuthorize("hasRole('USER')")
  public ApiResponse<?> options() {
    return ApiResponse.ok(service.options());
  }

  @PostMapping("/orders/{orderId}")
  @PreAuthorize("hasRole('USER')")
  public ApiResponse<?> create(@PathVariable Long orderId, @Valid @RequestBody Create input, Principal principal) {
    return ApiResponse.ok(service.createPayment(orderId, Long.valueOf(principal.getName()), input.provider()));
  }

  @PostMapping(value = "/callback/alipay", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  @PreAuthorize("permitAll()")
  @ResponseBody
  public String alipayNotify(@RequestParam Map<String, String> params) {
    return service.handleAlipayNotify(params);
  }

  @PostMapping(value = "/callback/wechat", consumes = MediaType.APPLICATION_JSON_VALUE)
  @PreAuthorize("permitAll()")
  @ResponseBody
  public String wechatNotify(
      @RequestHeader("Wechatpay-Serial") String serialNumber,
      @RequestHeader("Wechatpay-Timestamp") String timestamp,
      @RequestHeader("Wechatpay-Nonce") String nonce,
      @RequestHeader("Wechatpay-Signature") String signature,
      @RequestBody String body) {
    return service.handleWechatNotify(serialNumber, timestamp, nonce, signature, body);
  }
}
