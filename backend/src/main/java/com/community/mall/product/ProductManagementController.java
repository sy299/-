package com.community.mall.product;

import com.community.mall.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.community.mall.common.BusinessException;
import java.util.*;

@RestController
@RequestMapping("/api/manage/products")
@PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
public class ProductManagementController {
  private final JdbcTemplate db;
  public ProductManagementController(JdbcTemplate db){this.db=db;}
  @GetMapping
  public ApiResponse<?> list(java.security.Principal p,org.springframework.security.core.Authentication auth){
    String sql="select p.*,c.name categoryName,u.nickname merchantName from product p join product_category c on c.id=p.category_id join sys_user u on u.id=p.merchant_id";List<Object> args=new ArrayList<>();
    if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MERCHANT"))){sql+=" where p.merchant_id=?";args.add(p.getName());}return ApiResponse.ok(db.queryForList(sql+" order by p.created_at desc",args.toArray()));
  }
  @PutMapping("/{id}/status")
  public ApiResponse<?> updateStatus(@PathVariable Long id,@RequestBody Map<String,String> body,java.security.Principal p,org.springframework.security.core.Authentication auth){
    String status=body.get("status");if(!"ON_SALE".equals(status)&&!"OFF_SALE".equals(status))throw new BusinessException("商品状态只能是上架或下架");
    boolean admin=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
    String sql=admin?"update product set status=? where id=?":"update product set status=? where id=? and merchant_id=?";
    int n=admin?db.update(sql,status,id):db.update(sql,status,id,p.getName());if(n==0)throw new BusinessException("商品不存在或无权操作");return ApiResponse.ok();
  }
}
