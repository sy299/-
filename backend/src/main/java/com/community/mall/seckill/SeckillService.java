package com.community.mall.seckill;

import com.community.mall.common.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SeckillService {
  private final JdbcTemplate db;

  public SeckillService(JdbcTemplate db) {
    this.db = db;
  }

  public List<Map<String, Object>> listActivities() {
    return db.queryForList("""
        select a.id, a.product_id productId, p.name productName, p.cover, p.price originalPrice,
               a.seckill_price seckillPrice, a.stock availableStock,
               a.start_time startsAt, a.end_time endsAt,
               (a.start_time <= now() and a.end_time > now()) active
        from seckill_activity a
        join product p on p.id = a.product_id
        where a.status = 'PUBLISHED' and a.end_time > now() and p.status = 'ON_SALE'
        order by a.start_time, a.id
        """);
  }

  @Transactional(timeout = 2)
  public Map<String, Object> purchase(Long activityId, Long userId, SeckillController.Purchase input) {
    List<Map<String, Object>> rows = db.queryForList("""
        select a.product_id, a.seckill_price, a.status activity_status, a.stock,
               (a.start_time <= now() and a.end_time > now()) active,
               p.name, p.cover, p.merchant_id, p.status product_status, p.stock product_stock
        from seckill_activity a join product p on p.id = a.product_id
        where a.id = ?
        """, activityId);
    if (rows.isEmpty()) throw new BusinessException("秒杀活动不存在");
    Map<String, Object> item = rows.get(0);
    String productName = (String) item.get("name");

    if (!"PUBLISHED".equals(item.get("activity_status"))) throw new BusinessException("秒杀活动当前不可参与");
    if (!truthy(item.get("active"))) throw new BusinessException("秒杀活动尚未开始或已结束");
    if (!"ON_SALE".equals(item.get("product_status"))) throw new BusinessException("商品已下架，无法参加秒杀");
    if (number(item.get("stock")) < 1) throw new BusinessException("秒杀商品已售罄");
    if (number(item.get("product_stock")) < 1) throw new BusinessException("商品「" + productName + "」库存不足");
    if (db.queryForObject("select count(*) from seckill_order where activity_id = ? and user_id = ?", Long.class, activityId, userId) > 0) {
      throw new BusinessException("您已参加过本场秒杀");
    }

    int activityUpdated = db.update("""
        update seckill_activity
        set stock = stock - 1
        where id = ? and status = 'PUBLISHED' and start_time <= now() and end_time > now() and stock > 0
        """, activityId);
    if (activityUpdated != 1) throw new BusinessException(activityFailure(activityId));

    long productId = number(item.get("product_id"));
    int productUpdated = db.update("update product set stock = stock - 1 where id = ? and status = 'ON_SALE' and stock > 0", productId);
    if (productUpdated != 1) throw new BusinessException("商品「" + productName + "」库存不足");

    BigDecimal price = (BigDecimal) item.get("seckill_price");
    String orderNo = "SK" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    db.update("""
        insert into orders(order_no,user_id,product_id,merchant_id,quantity,total_amount,receiver_name,receiver_phone,
                           receiver_address,product_name_snapshot,product_cover_snapshot,unit_price_snapshot)
        values(?,?,?,?,1,?,?,?,?,?,?,?)
        """, orderNo, userId, productId, item.get("merchant_id"), price, input.receiverName(), input.receiverPhone(),
        input.receiverAddress(), productName, item.get("cover"), price);
    long orderId;
    try {
      orderId = db.queryForObject("select id from orders where order_no = ?", Long.class, orderNo);
      db.update("insert into seckill_order(activity_id,user_id,product_id,order_id) values(?,?,?,?)", activityId, userId, productId, orderId);
    } catch (DuplicateKeyException e) {
      throw new BusinessException("您已参加过本场秒杀");
    }
    return Map.of("orderId", orderId, "orderNo", orderNo, "productName", productName, "totalAmount", price);
  }

  private String activityFailure(Long activityId) {
    List<Map<String, Object>> rows = db.queryForList("""
        select status, stock,
               (start_time > now()) not_started, (end_time <= now()) ended
        from seckill_activity where id = ?
        """, activityId);
    if (rows.isEmpty()) return "秒杀活动不存在";
    Map<String, Object> activity = rows.get(0);
    if (!"PUBLISHED".equals(activity.get("status"))) return "秒杀活动当前不可参与";
    if (truthy(activity.get("not_started"))) return "秒杀活动尚未开始";
    if (truthy(activity.get("ended"))) return "秒杀活动已结束";
    if (number(activity.get("stock")) < 1) return "秒杀商品已售罄";
    return "当前抢购请求较多，请稍后重试";
  }

  private static long number(Object value) {
    return ((Number) value).longValue();
  }

  private static boolean truthy(Object value) {
    return Boolean.TRUE.equals(value) || (value instanceof Number number && number.intValue() != 0);
  }
}
