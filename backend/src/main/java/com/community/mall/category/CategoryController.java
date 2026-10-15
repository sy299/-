package com.community.mall.category;

import com.community.mall.common.ApiResponse;
import com.community.mall.common.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
  private final JdbcTemplate db;
  public CategoryController(JdbcTemplate db) { this.db = db; }

  @GetMapping
  public ApiResponse<?> list(@RequestParam(defaultValue = "false") boolean homeOnly) {
    String sql = homeOnly ? "select id,name,logo,show_on_home showOnHome from product_category where show_on_home=1 order by id" : "select id,name,logo,show_on_home showOnHome from product_category order by id";
    return ApiResponse.ok(db.queryForList(sql));
  }

  @PreAuthorize("hasRole('ADMIN')")
  @DeleteMapping("/{id}")
  public ApiResponse<?> delete(@PathVariable Long id) {
    if (!db.queryForList("select id from product where category_id=? limit 1", id).isEmpty()) throw new BusinessException("该分类下仍有商品，不能删除");
    db.update("delete from product_category where id=?", id);
    return ApiResponse.ok();
  }
}
