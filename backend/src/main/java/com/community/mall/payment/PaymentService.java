package com.community.mall.payment;

import com.alipay.api.AlipayApiException;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import com.alipay.api.internal.util.AlipaySignature;
import com.community.mall.common.BusinessException;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.AutoCertificateNotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PaymentService {
  private final JdbcTemplate db;
  private final String mode;
  private final boolean alipayEnabled;
  private final boolean wechatEnabled;
  private final String alipayGatewayUrl;
  private final String alipayAppId;
  private final String alipayPrivateKey;
  private final String alipayPublicKey;
  private final String alipayNotifyUrl;
  private final String wechatAppId;
  private final String wechatMchId;
  private final String wechatPrivateKeyPath;
  private final String wechatMerchantSerialNumber;
  private final String wechatApiV3Key;
  private final String wechatNotifyUrl;

  public PaymentService(
      JdbcTemplate db,
      @Value("${mall.payment.mode:unconfigured}") String mode,
      @Value("${mall.payment.alipay.enabled:false}") boolean alipayEnabled,
      @Value("${mall.payment.wechat.enabled:false}") boolean wechatEnabled,
      @Value("${mall.payment.alipay.gateway-url:https://openapi.alipay.com/gateway.do}") String alipayGatewayUrl,
      @Value("${mall.payment.alipay.app-id:}") String alipayAppId,
      @Value("${mall.payment.alipay.private-key:}") String alipayPrivateKey,
      @Value("${mall.payment.alipay.public-key:}") String alipayPublicKey,
      @Value("${mall.payment.alipay.notify-url:}") String alipayNotifyUrl,
      @Value("${mall.payment.wechat.app-id:}") String wechatAppId,
      @Value("${mall.payment.wechat.mch-id:}") String wechatMchId,
      @Value("${mall.payment.wechat.private-key-path:}") String wechatPrivateKeyPath,
      @Value("${mall.payment.wechat.merchant-serial-number:}") String wechatMerchantSerialNumber,
      @Value("${mall.payment.wechat.api-v3-key:}") String wechatApiV3Key,
      @Value("${mall.payment.wechat.notify-url:}") String wechatNotifyUrl) {
    this.db = db;
    this.mode = mode;
    this.alipayEnabled = alipayEnabled;
    this.wechatEnabled = wechatEnabled;
    this.alipayGatewayUrl = alipayGatewayUrl;
    this.alipayAppId = alipayAppId;
    this.alipayPrivateKey = alipayPrivateKey;
    this.alipayPublicKey = alipayPublicKey;
    this.alipayNotifyUrl = alipayNotifyUrl;
    this.wechatAppId = wechatAppId;
    this.wechatMchId = wechatMchId;
    this.wechatPrivateKeyPath = wechatPrivateKeyPath;
    this.wechatMerchantSerialNumber = wechatMerchantSerialNumber;
    this.wechatApiV3Key = wechatApiV3Key;
    this.wechatNotifyUrl = wechatNotifyUrl;
  }

  public List<Map<String, Object>> options() {
    return List.of(
        Map.of("code", "ALIPAY", "name", "支付宝", "enabled", alipayEnabled),
        Map.of("code", "WECHAT", "name", "微信支付", "enabled", wechatEnabled));
  }

  public String handleAlipayNotify(Map<String, String> params) {
    if (!alipayEnabled || !"real".equalsIgnoreCase(mode)) return "fail";
    try {
      if (!AlipaySignature.rsaCheckV1(params, alipayPublicKey, "UTF-8", "RSA2")) return "fail";
    } catch (AlipayApiException e) {
      return "fail";
    }

    String appId = params.get("app_id");
    String orderNo = params.get("out_trade_no");
    String tradeStatus = params.get("trade_status");
    if (appId == null || !appId.equals(alipayAppId) || orderNo == null
        || !Set.of("TRADE_SUCCESS", "TRADE_FINISHED").contains(tradeStatus)) return "fail";
    return markPaid(orderNo, params.get("total_amount")) ? "success" : "fail";
  }

  public String handleWechatNotify(String serialNumber, String timestamp, String nonce,
      String signature, String body) {
    if (!wechatEnabled || !"real".equalsIgnoreCase(mode)) return "fail";
    try {
      AutoCertificateNotificationConfig config = new AutoCertificateNotificationConfig.Builder()
          .merchantId(wechatMchId)
          .privateKeyFromPath(wechatPrivateKeyPath)
          .merchantSerialNumber(wechatMerchantSerialNumber)
          .apiV3Key(wechatApiV3Key)
          .build();
      RequestParam requestParam = new RequestParam.Builder()
          .serialNumber(serialNumber)
          .timestamp(timestamp)
          .nonce(nonce)
          .signature(signature)
          .body(body)
          .build();
      Transaction transaction = new NotificationParser(config).parse(requestParam, Transaction.class);
      if (transaction.getTradeState() != Transaction.TradeStateEnum.SUCCESS
          || !wechatMchId.equals(transaction.getMchid())
          || transaction.getOutTradeNo() == null
          || transaction.getAmount() == null
          || transaction.getAmount().getTotal() == null) return "fail";
      return markPaid(transaction.getOutTradeNo(),
          BigDecimal.valueOf(transaction.getAmount().getTotal()).movePointLeft(2).toPlainString())
          ? "success" : "fail";
    } catch (RuntimeException e) {
      return "fail";
    }
  }

  public Map<String, Object> createPayment(Long orderId, Long userId, String provider) {
    String normalizedProvider = provider == null ? "" : provider.trim().toUpperCase();
    String providerName = switch (normalizedProvider) {
      case "ALIPAY" -> "支付宝";
      case "WECHAT" -> "微信支付";
      default -> throw new BusinessException("请选择支付宝或微信支付");
    };

    List<Map<String, Object>> rows = db.queryForList(
        "select id,order_no,total_amount,status,product_name_snapshot from orders where id=? and user_id=?",
        orderId, userId);
    if (rows.isEmpty()) throw new BusinessException("订单不存在");
    Map<String, Object> order = rows.get(0);
    if (!"PENDING".equals(order.get("status"))) throw new BusinessException("订单当前无需支付");

    boolean enabled = "ALIPAY".equals(normalizedProvider) ? alipayEnabled : wechatEnabled;
    if (!enabled) {
      throw new BusinessException(providerName + "支付尚未配置商户参数，请在后端 application.yml 中完成配置");
    }
    if (!"real".equalsIgnoreCase(mode)) {
      throw new BusinessException("支付网关当前未启用，请将 mall.payment.mode 设置为 real");
    }

    if ("ALIPAY".equals(normalizedProvider)) return createAlipayPayment(order);
    return createWechatPayment(order);
  }

  private Map<String, Object> createAlipayPayment(Map<String, Object> order) {
    try {
      AlipayClient client = new DefaultAlipayClient(alipayGatewayUrl, alipayAppId, alipayPrivateKey,
          "json", "UTF-8", alipayPublicKey, "RSA2");
      AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
      request.setNotifyUrl(alipayNotifyUrl);
      request.setBizContent(JSON.toJSONString(Map.of(
          "out_trade_no", order.get("order_no"),
          "total_amount", order.get("total_amount").toString(),
          "subject", order.get("product_name_snapshot"),
          "product_code", "FAST_INSTANT_TRADE_PAY")));
      AlipayTradePagePayResponse response = client.pageExecute(request, "GET");
      if (!response.isSuccess()) throw new BusinessException("支付宝下单失败，请检查商户配置");
      return paymentResult(order, "ALIPAY", response.getBody(), null);
    } catch (AlipayApiException | RuntimeException e) {
      if (e instanceof BusinessException businessException) throw businessException;
      throw new BusinessException("支付宝下单失败，请检查商户配置");
    }
  }

  private Map<String, Object> createWechatPayment(Map<String, Object> order) {
    try {
      RSAAutoCertificateConfig config = new RSAAutoCertificateConfig.Builder()
          .merchantId(wechatMchId)
          .privateKeyFromPath(wechatPrivateKeyPath)
          .merchantSerialNumber(wechatMerchantSerialNumber)
          .apiV3Key(wechatApiV3Key)
          .build();
      NativePayService service = new NativePayService.Builder().config(config).build();
      PrepayRequest request = new PrepayRequest();
      request.setAppid(wechatAppId);
      request.setMchid(wechatMchId);
      request.setDescription(String.valueOf(order.get("product_name_snapshot")));
      request.setOutTradeNo(String.valueOf(order.get("order_no")));
      request.setNotifyUrl(wechatNotifyUrl);
      Amount amount = new Amount();
      amount.setTotal(((BigDecimal) order.get("total_amount")).movePointRight(2).intValueExact());
      request.setAmount(amount);
      PrepayResponse response = service.prepay(request);
      return paymentResult(order, "WECHAT", null, response.getCodeUrl());
    } catch (RuntimeException e) {
      throw new BusinessException("微信支付下单失败，请检查商户证书和 API v3 密钥");
    }
  }

  private Map<String, Object> paymentResult(Map<String, Object> order, String provider, String paymentForm, String paymentUrl) {
    Map<String, Object> result = new java.util.HashMap<>();
    result.put("orderId", order.get("id"));
    result.put("orderNo", order.get("order_no"));
    result.put("provider", provider);
    result.put("status", "READY");
    result.put("totalAmount", order.get("total_amount"));
    result.put("message", "支付请求已创建，请在第三方收银台完成付款");
    if (paymentForm != null) result.put("paymentForm", paymentForm);
    if (paymentUrl != null) result.put("paymentUrl", paymentUrl);
    return result;
  }

  private boolean markPaid(String orderNo, String amountText) {
    if (orderNo == null || amountText == null) return false;
    List<Map<String, Object>> rows = db.queryForList(
        "select id,total_amount,status from orders where order_no = ?", orderNo);
    if (rows.isEmpty()) return false;
    Map<String, Object> order = rows.get(0);
    BigDecimal expected = (BigDecimal) order.get("total_amount");
    BigDecimal actual;
    try {
      actual = new BigDecimal(amountText);
    } catch (NumberFormatException e) {
      return false;
    }
    if (expected.compareTo(actual) != 0) return false;
    String status = String.valueOf(order.get("status"));
    if ("PAID".equals(status)) return true;
    if (!"PENDING".equals(status)) return false;
    return db.update("update orders set status='PAID', paid_at=coalesce(paid_at, now()) "
        + "where id=? and status='PENDING' and total_amount=?", order.get("id"), expected) == 1;
  }
}
