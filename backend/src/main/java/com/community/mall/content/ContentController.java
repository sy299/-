package com.community.mall.content;
import com.community.mall.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/content") @PreAuthorize("hasRole('ADMIN')")
public class ContentController {
  private final JdbcTemplate db; public ContentController(JdbcTemplate db){this.db=db;}
  @GetMapping("/announcements") public ApiResponse<?> announcements(){return ApiResponse.ok(db.queryForList("select * from announcement order by publish_time desc"));}
  @PostMapping("/announcements") public ApiResponse<?> announcement(@RequestBody java.util.Map<String,Object> in){db.update("insert into announcement(title,content) values(?,?)",in.get("title"),in.get("content"));return ApiResponse.ok();}
  @PutMapping("/announcements/{id}") public ApiResponse<?> updateAnnouncement(@PathVariable Long id,@RequestBody java.util.Map<String,Object> in){db.update("update announcement set title=?,content=?,status=? where id=?",in.get("title"),in.get("content"),in.getOrDefault("status","PUBLISHED"),id);return ApiResponse.ok();}
  @DeleteMapping("/announcements/{id}") public ApiResponse<?> deleteAnnouncement(@PathVariable Long id){db.update("delete from announcement where id=?",id);return ApiResponse.ok();}
  @GetMapping("/banners") public ApiResponse<?> banners(){return ApiResponse.ok(db.queryForList("select * from banner order by sort_no"));}
  @PostMapping("/banners") public ApiResponse<?> banner(@RequestBody java.util.Map<String,Object> in){db.update("insert into banner(image_url,product_id,sort_no,enabled) values(?,?,?,?)",in.get("imageUrl"),in.get("productId"),in.getOrDefault("sortNo",0),in.getOrDefault("enabled",true));return ApiResponse.ok();}
  @PutMapping("/banners/{id}") public ApiResponse<?> updateBanner(@PathVariable Long id,@RequestBody java.util.Map<String,Object> in){db.update("update banner set image_url=?,product_id=?,sort_no=?,enabled=? where id=?",in.get("imageUrl"),in.get("productId"),in.getOrDefault("sortNo",0),in.getOrDefault("enabled",true),id);return ApiResponse.ok();}
  @DeleteMapping("/banners/{id}") public ApiResponse<?> deleteBanner(@PathVariable Long id){db.update("delete from banner where id=?",id);return ApiResponse.ok();}
}
