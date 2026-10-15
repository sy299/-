package com.community.mall.cart;
import com.community.mall.common.*;
import jakarta.validation.constraints.Min;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/cart") @PreAuthorize("hasRole('USER')")
public class CartController {
  private final JdbcTemplate db; public CartController(JdbcTemplate db){this.db=db;}
  @GetMapping public ApiResponse<?> list(java.security.Principal p){return ApiResponse.ok(db.queryForList("select c.id,c.product_id productId,c.quantity,p.name,p.cover,p.price,p.stock,p.status from shopping_cart c join product p on p.id=c.product_id where c.user_id=?",p.getName()));}
  public record CartInput(Long productId,@Min(1) Integer quantity){}
  @PostMapping public ApiResponse<?> add(@RequestBody CartInput in,java.security.Principal p){
    var rows=db.queryForList("select id,stock from product where id=? and status='ON_SALE'",in.productId());if(rows.isEmpty())throw new BusinessException("商品不可购买");db.update("insert into shopping_cart(user_id,product_id,quantity) values(?,?,?) on duplicate key update quantity=quantity+values(quantity)",p.getName(),in.productId(),in.quantity());return ApiResponse.ok();}
  @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id,@RequestBody CartInput in,java.security.Principal p){int n=db.update("update shopping_cart set quantity=? where id=? and user_id=?",in.quantity(),id,p.getName());if(n==0)throw new BusinessException("购物车商品不存在");return ApiResponse.ok();}
  @DeleteMapping public ApiResponse<?> clear(java.security.Principal p){db.update("delete from shopping_cart where user_id=?",p.getName());return ApiResponse.ok();}
  @DeleteMapping("/{id}") public ApiResponse<?> remove(@PathVariable Long id,java.security.Principal p){db.update("delete from shopping_cart where id=? and user_id=?",id,p.getName());return ApiResponse.ok();}
}
