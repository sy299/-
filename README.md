# 优选商城

基于前后端分离 B/S 架构的品质购物平台示例实现。

## 技术栈

- 后端：Java 17+（推荐 24/25）、Spring Boot 3.4、MyBatis-Plus、Spring Security、JWT、MySQL 8
- 前端：Node 18+（推荐 24）、Vue 3、Vite、Element Plus、Vue Router、Axios
- 目录：backend 后端服务，frontend 管理与用户端 SPA，database/schema.sql 数据库脚本

## 快速启动

1. 创建数据库并导入 database/schema.sql；已有旧数据库则额外执行 database/migration-v2.sql 和 database/migration-v3-products.sql。
2. 修改 backend/src/main/resources/application.yml 中的数据库账号密码。
3. 后端执行 mvn spring-boot:run（或使用 IDE 启动 CommunityMallApplication）。
4. 前端执行 npm install、npm run dev，访问 Vite 输出地址。

默认种子账号：admin / admin123（管理员）、merchant / merchant123（商家）、user / user123（普通用户）。生产环境请立即修改密码与 JWT 密钥。

## 核心模块

用户注册/资料、商家/管理员与 JWT 登录、商品分类与商品、购物车合并结算、收货地址、立即购买、订单状态与退款、确认收货与评价、收藏、公告、轮播图、商家平台标签。管理员和商家接口通过角色注解进行隔离，订单的商品名称、封面、价格及收货信息会保存快照。

## 支付接入

下单后订单保持 `PENDING`，用户可在订单中心选择支付宝或微信支付。后端接口为 `POST /api/payments/orders/{orderId}`，请求体为 `{ "provider": "ALIPAY" }` 或 `{ "provider": "WECHAT" }`。

项目已引入支付宝和微信支付官方 Java SDK，当前默认配置为关闭支付：

```yaml
mall:
  payment:
    mode: unconfigured # 正式环境改为 real
    alipay:
      enabled: false
      app-id: ${ALIPAY_APP_ID:}
      private-key: ${ALIPAY_PRIVATE_KEY:}
      public-key: ${ALIPAY_PUBLIC_KEY:}
      notify-url: ${ALIPAY_NOTIFY_URL:}
    wechat:
      enabled: false
      app-id: ${WECHAT_APP_ID:}
      mch-id: ${WECHAT_MCH_ID:}
      private-key-path: ${WECHAT_PRIVATE_KEY_PATH:}
      merchant-serial-number: ${WECHAT_MERCHANT_SERIAL_NUMBER:}
      api-v3-key: ${WECHAT_API_V3_KEY:}
      notify-url: ${WECHAT_NOTIFY_URL:}
```

正式上线前还需要配置商户号、应用 ID、私钥/证书、API 密钥和异步通知地址。项目已提供并放行以下异步回调接口，回调会先验签，再校验订单号和金额，最后幂等更新订单为 `PAID`：

- 支付宝：`POST /api/payments/callback/alipay`（表单回调）
- 微信支付：`POST /api/payments/callback/wechat`（JSON 回调，保留微信支付签名请求头）

支付宝商户平台的异步通知地址填 `ALIPAY_NOTIFY_URL`，微信支付商户平台的回调地址填 `WECHAT_NOTIFY_URL`。项目不会在用户点击“立即支付”时直接伪造支付成功，只有第三方异步通知验签成功才会更新订单状态。
