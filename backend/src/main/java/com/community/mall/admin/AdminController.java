package com.community.mall.admin;
import com.community.mall.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final JdbcTemplate db; public AdminController(JdbcTemplate db){this.db=db;}
  @GetMapping("/dashboard") public ApiResponse<?> dashboard(){return ApiResponse.ok(java.util.Map.of("users",count("sys_user"),"products",count("product"),"orders",count("orders"),"merchants",db.queryForObject("select count(*) from sys_user where role='MERCHANT'",Long.class)));}
  private long count(String t){return db.queryForObject("select count(*) from "+t,Long.class);}
  @GetMapping("/categories") public ApiResponse<?> categories(){return ApiResponse.ok(db.queryForList("select * from product_category order by id"));}
  @PostMapping("/categories") public ApiResponse<?> addCategory(@RequestBody java.util.Map<String,Object> in){db.update("insert into product_category(name,logo,show_on_home) values(?,?,?)",in.get("name"),in.get("logo"),in.getOrDefault("showOnHome",true));return ApiResponse.ok();}
  @PutMapping("/categories/{id}") public ApiResponse<?> category(@PathVariable Long id,@RequestBody java.util.Map<String,Object> in){db.update("update product_category set name=?,logo=?,show_on_home=? where id=?",in.get("name"),in.get("logo"),in.get("showOnHome"),id);return ApiResponse.ok();}
  @GetMapping("/tags") public ApiResponse<?> tags(){return ApiResponse.ok(db.queryForList("select t.*,u.nickname merchantName from merchant_tag t join sys_user u on u.id=t.merchant_id order by t.created_at desc"));}
  @PostMapping("/tags") public ApiResponse<?> addTag(@RequestBody java.util.Map<String,Object> in){db.update("insert into merchant_tag(merchant_id,tag,description) values(?,?,?)",in.get("merchantId"),in.get("tag"),in.get("description"));return ApiResponse.ok();}
  @PutMapping("/tags/{id}") public ApiResponse<?> updateTag(@PathVariable Long id,@RequestBody java.util.Map<String,Object> in){db.update("update merchant_tag set merchant_id=?,tag=?,description=? where id=?",in.get("merchantId"),in.get("tag"),in.get("description"),id);return ApiResponse.ok();}
  @DeleteMapping("/tags/{id}") public ApiResponse<?> deleteTag(@PathVariable Long id){db.update("delete from merchant_tag where id=?",id);return ApiResponse.ok();}
}
