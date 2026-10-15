package com.community.mall.home;
import com.community.mall.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/home")
public class HomeController {
  private final JdbcTemplate db; public HomeController(JdbcTemplate db){this.db=db;}
  @GetMapping public ApiResponse<?> home(){
    return ApiResponse.ok(Map.of(
      "categories",db.queryForList("select id,name,logo from product_category where show_on_home=1 order by id"),
      "products",db.queryForList("select p.id,p.name,p.cover,p.price,p.stock,u.nickname merchantName from product p join sys_user u on u.id=p.merchant_id where p.status='ON_SALE' order by p.created_at desc limit 20"),
      "banners",db.queryForList("select id,image_url imageUrl,product_id productId from banner where enabled=1 order by sort_no"),
      "announcements",db.queryForList("select id,title,content,publish_time publishTime from announcement where status='PUBLISHED' order by publish_time desc limit 5")));
  }
}
