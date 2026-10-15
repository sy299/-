package com.community.mall.product;
import com.community.mall.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
@RestController @RequestMapping("/api/products")
public class ProductController {
  private final JdbcTemplate db; public ProductController(JdbcTemplate db){this.db=db;}
  @GetMapping public ApiResponse<?> list(@RequestParam(required=false) Long categoryId,@RequestParam(required=false) String keyword,@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="12") int pageSize){
    String sql="select p.*,u.nickname merchantName,c.name categoryName from product p join sys_user u on u.id=p.merchant_id join product_category c on c.id=p.category_id where p.status='ON_SALE'"; List<Object> args=new ArrayList<>();
    if(categoryId!=null){sql+=" and p.category_id=?";args.add(categoryId);} if(keyword!=null&&!keyword.isBlank()){sql+=" and p.name like ?";args.add("%"+keyword+"%");} sql+=" order by p.created_at desc limit ? offset ?";
    int safePage=Math.max(page,1),safePageSize=Math.min(Math.max(pageSize,1),50);args.add(safePageSize);args.add((long)(safePage-1)*safePageSize);
    return ApiResponse.ok(db.queryForList(sql,args.toArray()));
  }
  @GetMapping("/{id}") public ApiResponse<?> detail(@PathVariable Long id){ var rows=db.queryForList("select p.*,u.nickname merchantName,c.name categoryName from product p join sys_user u on u.id=p.merchant_id join product_category c on c.id=p.category_id where p.id=?",id); if(rows.isEmpty()) throw new BusinessException("商品不存在"); return ApiResponse.ok(rows.get(0)); }
  public record ProductInput(@NotBlank String name,@NotBlank String cover,@NotNull Long categoryId,@NotNull @DecimalMin("0.01") BigDecimal price,@NotNull @Min(0) Integer stock,String detail,String images){}
  @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')") @PostMapping public ApiResponse<?> create(@Valid @RequestBody ProductInput in,java.security.Principal p){ db.update("insert into product(name,cover,merchant_id,category_id,price,detail,stock,images) values(?,?,?,?,?,?,?,?)",in.name(),in.cover(),Long.valueOf(p.getName()),in.categoryId(),in.price(),in.detail(),in.stock(),in.images()); return ApiResponse.ok(); }
  @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')") @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id,@Valid @RequestBody ProductInput in,java.security.Principal p){ int n=db.update("update product set name=?,cover=?,category_id=?,price=?,detail=?,stock=?,images=? where id=? and (merchant_id=? or ? in (select id from sys_user where role='ADMIN'))",in.name(),in.cover(),in.categoryId(),in.price(),in.detail(),in.stock(),in.images(),id,p.getName(),p.getName()); if(n==0) throw new BusinessException("无权修改该商品"); return ApiResponse.ok(); }
  @PreAuthorize("hasAnyRole('MERCHANT','ADMIN')") @DeleteMapping("/{id}") public ApiResponse<?> delete(@PathVariable Long id,java.security.Principal p){ db.update("update product set status='OFF_SALE' where id=? and (merchant_id=? or ? in (select id from sys_user where role='ADMIN'))",id,p.getName(),p.getName()); return ApiResponse.ok(); }
}
