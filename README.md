# 优选商城

基于前后端分离 B/S 架构的品质购物平台示例实现。

## 技术栈

- 后端：Java 17+（推荐 24/25）、Spring Boot 3.4、MyBatis-Plus、Spring Security、JWT、MySQL 8、Redis 7+
- 前端：Node 18+（推荐 24）、Vue 3、Vite、Element Plus、Vue Router、Axios
- 目录：backend 后端服务，frontend 管理与用户端 SPA，database/schema.sql 数据库脚本

## 快速启动

1. 创建数据库并导入 database/schema.sql、database/migration-v4-seckill.sql、database/seed-seckill.sql；已有旧数据库则额外执行 database/migration-v2.sql、database/migration-v3-products.sql、database/migration-v5-seckill-more.sql、database/migration-v6-seckill-on-sale.sql。
2. 修改 backend/src/main/resources/application.yml 中的数据库账号密码。
3. 启动 Redis（默认 `localhost:6379`，密码为 `radis`），例如：`docker run --name community-mall-redis -p 6379:6379 -d redis:7-alpine redis-server --requirepass radis`。
4. 首次启动前进入 `frontend` 执行 `npm install`。
5. 使用 IDE 启动 `CommunityMallApplication`，启动类会同时启动 Spring Boot 后端和 Vite 前端；访问 `http://localhost:5173`。

如果 `5173` 已经有前端进程在运行，启动类会复用现有前端，不会重复启动。

默认种子账号：admin / admin123（管理员）、merchant / merchant123（商家）、user / user123（普通用户）。生产环境请立即修改密码与 JWT 密钥。

## 核心模块

用户注册/资料、头像上传、商家/管理员与 JWT 登录、商品分类与商品、商品封面和轮播图上传、购物车合并结算、收货地址、立即购买、订单状态与退款、确认收货与评价、收藏、公告、轮播图、商家平台标签。管理员和商家接口通过角色注解进行隔离，订单的商品名称、封面、价格及收货信息会保存快照。

## 个人中心与秒杀

- 登录后从顶部“个人中心”进入个人资料页，可查看头像、账号、昵称、手机号、订单统计和最近订单，并修改头像地址、昵称及手机号。
- 秒杀库存会在活动开始前预热到 Redis。抢购请求通过 Lua 脚本原子完成库存扣减和用户去重，只有 Redis 抢到名额的请求才会进入 MySQL 事务，数据库仍负责最终库存、订单和“一人一单”约束。
- Redis 地址可通过 `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD` 覆盖，秒杀 key 前缀可通过 `mall.seckill.redis-key-prefix` 配置。
- 开发环境启动时会幂等补齐 1 到 6 号商品的演示秒杀活动；生产环境可将 `mall.seckill.seed-demo-activities` 设为 `false`，改用数据库迁移脚本管理活动。

这套链路针对十万级瞬时抢购请求设计，能把绝大多数请求挡在 Redis 层，避免请求全部争抢 MySQL 行锁。生产环境仍应使用 Redis 主从/哨兵或集群、负载均衡和至少 2 个后端实例，并在网关层限流；单台开发机和单个 Redis 实例不能据此保证十万真实并发的吞吐。

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
