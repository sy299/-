package com.community.mall.seckill;

import com.community.mall.common.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SeckillService {
  private static final Logger log = LoggerFactory.getLogger(SeckillService.class);

  private static final DefaultRedisScript<Long> RESERVE_SCRIPT = new DefaultRedisScript<>("""
      if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
        return 2
      end
      local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
      if stock <= 0 then
        return 0
      end
      redis.call('DECR', KEYS[1])
      redis.call('SADD', KEYS[2], ARGV[1])
      return 1
      """, Long.class);

  private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>("""
      if redis.call('SREM', KEYS[2], ARGV[1]) == 1 then
        redis.call('INCR', KEYS[1])
        return 1
      end
      return 0
      """, Long.class);

  private static final DefaultRedisScript<Long> RESERVE_WITHOUT_USER_DEDUP_SCRIPT = new DefaultRedisScript<>("""
      local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
      if stock <= 0 then
        return 0
      end
      redis.call('DECR', KEYS[1])
      return 1
      """, Long.class);

  private final JdbcTemplate db;
  private final StringRedisTemplate redis;
  private final String keyPrefix;
  private final boolean seedDemoActivities;
  private final boolean enforceOneOrderPerUser;

  public SeckillService(
      JdbcTemplate db,
      StringRedisTemplate redis,
      @Value("${mall.seckill.redis-key-prefix:mall:seckill}") String keyPrefix,
      @Value("${mall.seckill.seed-demo-activities:true}") boolean seedDemoActivities,
      @Value("${mall.seckill.enforce-one-order-per-user:true}") boolean enforceOneOrderPerUser) {
    this.db = db;
    this.redis = redis;
    this.keyPrefix = keyPrefix;
    this.seedDemoActivities = seedDemoActivities;
    this.enforceOneOrderPerUser = enforceOneOrderPerUser;
    if (!enforceOneOrderPerUser) {
      log.warn("Seckill one-order-per-user protection is disabled for local load testing.");
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void warmUpRedis() {
    try {
      if (seedDemoActivities) seedDemoActivities();
      db.queryForList("""
          select a.id, a.product_id productId, p.name productName, p.cover,
                 p.price originalPrice, a.seckill_price seckillPrice, a.stock availableStock,
                 p.merchant_id merchantId, a.status activityStatus, p.status productStatus,
                 a.start_time startsAt, a.end_time endsAt
          from seckill_activity a
          join product p on p.id = a.product_id
          where a.status = 'PUBLISHED' and a.end_time > now() and p.status = 'ON_SALE'
          """).forEach(this::cacheActivity);
    } catch (Exception ignored) {
      // Redis may be started after the application; the first activity request retries warm-up.
    }
  }

  private void seedDemoActivities() {
    db.update("""
        insert ignore into seckill_activity
          (id, product_id, title, seckill_price, stock, start_time, end_time, status)
        select p.id, p.id, concat(p.name, '限时秒杀'),
               case p.id
                 when 1 then 9.90
                 when 2 then 39.90
                 when 3 then 19.90
                 when 4 then 32.90
                 when 5 then 39.90
                 when 6 then 59.90
               end,
               case p.id
                 when 1 then 20
                 when 2 then 18
                 when 3 then 24
                 when 4 then 16
                 when 5 then 30
                 when 6 then 14
               end,
               date_sub(now(), interval 1 minute),
               date_add(now(), interval 1 day),
               'PUBLISHED'
        from product p
        where p.id between 1 and 6
        """);
    db.update("""
        insert ignore into seckill_activity
          (id, product_id, title, seckill_price, stock, start_time, end_time, status)
        select case p.id
                 when 3 then 7
                 when 7 then 8
                 when 8 then 9
                 when 9 then 10
                 when 10 then 11
                 when 11 then 12
                 when 12 then 13
                 when 13 then 14
               end,
               p.id,
               concat(p.name, '限时秒杀'),
               case p.id
                 when 3 then 19.90
                 when 7 then 14.90
                 when 8 then 29.90
                 when 9 then 24.90
                 when 10 then 12.90
                 when 11 then 39.90
                 when 12 then 59.90
                 when 13 then 79.90
               end,
               case p.id
                 when 3 then 24
                 when 7 then 20
                 when 8 then 30
                 when 9 then 25
                 when 10 then 40
                 when 11 then 20
                 when 12 then 15
                 when 13 then 18
               end,
               date_sub(now(), interval 1 minute),
               date_add(now(), interval 1 day),
               'PUBLISHED'
        from product p
        where p.id in (3, 7, 8, 9, 10, 11, 12, 13)
          and p.status = 'ON_SALE'
          and p.stock > 0
        """);
  }

  public List<Map<String, Object>> listActivities() {
    List<Map<String, Object>> activities = db.queryForList("""
        select a.id, a.product_id productId, p.name productName, p.cover, p.price originalPrice,
               a.seckill_price seckillPrice, a.stock availableStock, p.merchant_id merchantId,
               a.status activityStatus, p.status productStatus,
               a.start_time startsAt, a.end_time endsAt,
               (a.start_time <= now() and a.end_time > now()) active
        from seckill_activity a
        join product p on p.id = a.product_id
        where a.status = 'PUBLISHED' and a.end_time > now() and p.status = 'ON_SALE'
        order by a.start_time, a.id
        """);
    boolean redisAvailable = true;
    for (Map<String, Object> activity : activities) {
      if (!redisAvailable) continue;
      try {
        cacheActivity(activity);
        activity.put("availableStock", readStock(activityId(activity)));
      } catch (RuntimeException ignored) {
        // Keep the read-only activity page usable while Redis is starting.
        redisAvailable = false;
      }
    }
    return activities;
  }

  @Transactional(timeout = 3)
  public Map<String, Object> purchase(Long activityId, Long userId, SeckillController.Purchase input) {
    Map<String, String> activity = loadActivity(activityId);
    validateActivity(activity);

    int currentStock = ensureStock(activity);
    if (currentStock < 1) throw new BusinessException("秒杀商品已售罄");

    int reserved = reserve(activityId, userId, activity);
    if (reserved == 2) throw new BusinessException("您已参加过本场秒杀");
    if (reserved != 1) throw new BusinessException("秒杀商品已售罄");

    try {
      long productId = Long.parseLong(activity.get("productId"));
      int activityUpdated = db.update("""
          update seckill_activity
          set stock = stock - 1
          where id = ? and status = 'PUBLISHED'
            and start_time <= now() and end_time > now() and stock > 0
          """, activityId);
      if (activityUpdated != 1) throw new BusinessException("秒杀活动已售罄或已结束");

      int productUpdated = db.update("""
          update product
          set stock = stock - 1
          where id = ? and status = 'ON_SALE' and stock > 0
          """, productId);
      if (productUpdated != 1) {
        throw new BusinessException("商品「" + activity.get("productName") + "」库存不足");
      }

      BigDecimal price = new BigDecimal(activity.get("seckillPrice"));
      String orderNo = "SK" + System.currentTimeMillis()
          + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
      db.update("""
          insert into orders(order_no,user_id,product_id,merchant_id,quantity,total_amount,receiver_name,receiver_phone,
                             receiver_address,product_name_snapshot,product_cover_snapshot,unit_price_snapshot)
          values(?,?,?,?,1,?,?,?,?,?,?,?)
          """, orderNo, userId, productId, activity.get("merchantId"), price, input.receiverName(),
          input.receiverPhone(), input.receiverAddress(), activity.get("productName"), activity.get("cover"), price);

      Long orderId = db.queryForObject("select id from orders where order_no = ?", Long.class, orderNo);
      if (enforceOneOrderPerUser) {
        db.update("insert into seckill_order(activity_id,user_id,product_id,order_id) values(?,?,?,?)",
            activityId, userId, productId, orderId);
      }
      return Map.of(
          "orderId", orderId,
          "orderNo", orderNo,
          "productName", activity.get("productName"),
          "totalAmount", price
      );
    } catch (DuplicateKeyException e) {
      release(activityId, userId);
      throw new BusinessException("您已参加过本场秒杀");
    } catch (RuntimeException e) {
      release(activityId, userId);
      throw e;
    }
  }

  private Map<String, String> loadActivity(Long activityId) {
    Map<String, String> cached = redis.<String, String>opsForHash().entries(metaKey(activityId));
    if (!cached.isEmpty()) return cached;

    List<Map<String, Object>> rows = db.queryForList("""
        select a.id, a.product_id productId, p.name productName, p.cover,
               p.price originalPrice, a.seckill_price seckillPrice, a.stock availableStock,
               p.merchant_id merchantId, a.status activityStatus, p.status productStatus,
               a.start_time startsAt, a.end_time endsAt
        from seckill_activity a
        join product p on p.id = a.product_id
        where a.id = ?
        """, activityId);
    if (rows.isEmpty()) throw new BusinessException("秒杀活动不存在");
    cacheActivity(rows.get(0));
    return redis.<String, String>opsForHash().entries(metaKey(activityId));
  }

  private void validateActivity(Map<String, String> activity) {
    if (!"PUBLISHED".equals(activity.get("activityStatus"))) {
      throw new BusinessException("秒杀活动当前不可参与");
    }
    long now = System.currentTimeMillis();
    if (now < Long.parseLong(activity.get("startsAt"))) {
      throw new BusinessException("秒杀活动尚未开始");
    }
    if (now >= Long.parseLong(activity.get("endsAt"))) {
      throw new BusinessException("秒杀活动已结束");
    }
    if (!"ON_SALE".equals(activity.get("productStatus"))) {
      throw new BusinessException("商品已下架，无法参加秒杀");
    }
  }

  private int ensureStock(Map<String, String> activity) {
    String key = stockKey(Long.parseLong(activity.get("activityId")));
    String stock = redis.opsForValue().get(key);
    if (stock == null) {
      redis.opsForValue().setIfAbsent(key, activity.get("activityStock"), ttl(activity));
      stock = redis.opsForValue().get(key);
    }
    return stock == null ? 0 : Integer.parseInt(stock);
  }

  private int reserve(Long activityId, Long userId, Map<String, String> activity) {
    Long result = redis.execute(
        enforceOneOrderPerUser ? RESERVE_SCRIPT : RESERVE_WITHOUT_USER_DEDUP_SCRIPT,
        List.of(stockKey(activityId), buyersKey(activityId)),
        String.valueOf(userId)
    );
    if (enforceOneOrderPerUser) {
      redis.expire(buyersKey(activityId), ttl(activity));
    }
    return result == null ? -1 : result.intValue();
  }

  private void release(Long activityId, Long userId) {
    if (enforceOneOrderPerUser) {
      redis.execute(
          RELEASE_SCRIPT,
          List.of(stockKey(activityId), buyersKey(activityId)),
          String.valueOf(userId)
      );
    } else {
      redis.opsForValue().increment(stockKey(activityId));
    }
  }

  private void cacheActivity(Map<String, Object> activity) {
    Long id = activityId(activity);
    Map<String, String> metadata = new HashMap<>();
    metadata.put("activityId", String.valueOf(id));
    metadata.put("productId", value(activity, "productId"));
    metadata.put("productName", value(activity, "productName"));
    metadata.put("cover", value(activity, "cover"));
    metadata.put("originalPrice", value(activity, "originalPrice"));
    metadata.put("seckillPrice", value(activity, "seckillPrice"));
    metadata.put("activityStock", value(activity, "availableStock"));
    metadata.put("merchantId", value(activity, "merchantId"));
    metadata.put("activityStatus", value(activity, "activityStatus"));
    metadata.put("productStatus", value(activity, "productStatus"));
    metadata.put("startsAt", String.valueOf(epochMillis(activity.get("startsAt"))));
    metadata.put("endsAt", String.valueOf(epochMillis(activity.get("endsAt"))));
    redis.opsForHash().putAll(metaKey(id), metadata);
    redis.expire(metaKey(id), ttl(metadata));
    redis.opsForValue().setIfAbsent(stockKey(id), metadata.get("activityStock"), ttl(metadata));
  }

  private Long activityId(Map<String, Object> activity) {
    return ((Number) activity.get("id")).longValue();
  }

  private int readStock(Long activityId) {
    String stock = redis.opsForValue().get(stockKey(activityId));
    return stock == null ? 0 : Integer.parseInt(stock);
  }

  private String metaKey(Long activityId) {
    return keyPrefix + ":activity:{" + activityId + "}:meta";
  }

  private String stockKey(Long activityId) {
    return keyPrefix + ":activity:{" + activityId + "}:stock";
  }

  private String buyersKey(Long activityId) {
    return keyPrefix + ":activity:{" + activityId + "}:buyers";
  }

  private static Duration ttl(Map<String, String> activity) {
    long millis = Long.parseLong(activity.get("endsAt")) - System.currentTimeMillis() + 86_400_000L;
    return Duration.ofSeconds(Math.max(60, millis / 1000));
  }

  private static String value(Map<String, Object> row, String key) {
    Object value = row.get(key);
    return value == null ? "" : String.valueOf(value);
  }

  private static long epochMillis(Object value) {
    if (value instanceof java.util.Date date) return date.getTime();
    String text = String.valueOf(value).replace('T', ' ');
    return java.sql.Timestamp.valueOf(text).getTime();
  }
}
