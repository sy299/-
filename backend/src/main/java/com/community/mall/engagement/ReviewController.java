package com.community.mall.engagement;

import com.community.mall.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
public class ReviewController {
  private final JdbcTemplate db;
  public ReviewController(JdbcTemplate db){this.db=db;}
  @GetMapping("/{productId}/reviews") public ApiResponse<?> list(@PathVariable Long productId){return ApiResponse.ok(db.queryForList("select r.id,r.rating,r.content,r.created_at createdAt,u.nickname from product_review r join sys_user u on u.id=r.user_id where r.product_id=? order by r.created_at desc",productId));}
}
