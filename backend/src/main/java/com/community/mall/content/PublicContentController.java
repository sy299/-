package com.community.mall.content;

import com.community.mall.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class PublicContentController {
  private final JdbcTemplate db;
  public PublicContentController(JdbcTemplate db) { this.db = db; }
  @GetMapping("/api/announcements")
  public ApiResponse<?> announcements() { return ApiResponse.ok(db.queryForList("select id,title,content,publish_time publishTime from announcement where status='PUBLISHED' order by publish_time desc")); }
  @GetMapping("/api/banners")
  public ApiResponse<?> banners() { return ApiResponse.ok(db.queryForList("select b.id,b.image_url imageUrl,b.product_id productId,p.name productName from banner b left join product p on p.id=b.product_id where b.enabled=1 order by b.sort_no")); }
  @GetMapping("/api/merchants/{merchantId}/tags")
  public ApiResponse<?> merchantTags(@PathVariable Long merchantId) { return ApiResponse.ok(db.queryForList("select id,tag,description from merchant_tag where merchant_id=? order by created_at desc",merchantId)); }
}
