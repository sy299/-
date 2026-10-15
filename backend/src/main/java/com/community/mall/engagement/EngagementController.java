package com.community.mall.engagement;
import com.community.mall.common.ApiResponse;
import com.community.mall.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api") @PreAuthorize("hasRole('USER')")
public class EngagementController {
  private final JdbcTemplate db; public EngagementController(JdbcTemplate db){this.db=db;}
  @GetMapping("/favorites") public ApiResponse<?> favorites(java.security.Principal p){return ApiResponse.ok(db.queryForList("select f.id,f.product_id productId,p.name,p.cover,p.price from favorite f join product p on p.id=f.product_id where f.user_id=?",p.getName()));}
  @PostMapping("/favorites/{productId}") public ApiResponse<?> favorite(@PathVariable Long productId,java.security.Principal p){db.update("insert ignore into favorite(user_id,product_id) values(?,?)",p.getName(),productId);return ApiResponse.ok();}
  @DeleteMapping("/favorites/{productId}") public ApiResponse<?> unfavorite(@PathVariable Long productId,java.security.Principal p){db.update("delete from favorite where user_id=? and product_id=?",p.getName(),productId);return ApiResponse.ok();}
  @PostMapping("/reviews") public ApiResponse<?> review(@RequestBody java.util.Map<String,Object> in,java.security.Principal p){var order=db.queryForList("select product_id from orders where id=? and user_id=? and status='COMPLETED'",in.get("orderId"),p.getName());if(order.isEmpty())throw new BusinessException("只有已完成订单可以评价");int rating=((Number)in.getOrDefault("rating",0)).intValue();if(rating<1||rating>5)throw new BusinessException("评分必须为1到5分");try{db.update("insert into product_review(order_id,product_id,user_id,rating,content) values(?,?,?,?,?)",in.get("orderId"),order.get(0).get("product_id"),p.getName(),rating,in.get("content"));}catch(Exception e){throw new BusinessException("该订单已经评价过");}return ApiResponse.ok();}
}
