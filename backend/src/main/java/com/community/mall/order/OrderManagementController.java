package com.community.mall.order;

import com.community.mall.common.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@RestController
@RequestMapping("/api/manage/orders")
@PreAuthorize("hasAnyRole('MERCHANT','ADMIN')")
public class OrderManagementController {
  private final JdbcTemplate db;
  public OrderManagementController(JdbcTemplate db){this.db=db;}
  @GetMapping
  public ApiResponse<?> list(@RequestParam(required=false) String status,java.security.Principal p,org.springframework.security.core.Authentication auth){
    String sql="select o.*,u.nickname userNickname from orders o join sys_user u on u.id=o.user_id where 1=1";List<Object> args=new ArrayList<>();
    if(auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MERCHANT"))){sql+=" and o.merchant_id=?";args.add(p.getName());}
    if(status!=null&&!status.isBlank()){sql+=" and o.status=?";args.add(status);}sql+=" order by o.created_at desc";return ApiResponse.ok(db.queryForList(sql,args.toArray()));
  }
  public record Status(String status,String reason){}
  @PutMapping("/{id}/status")
  @Transactional
  public ApiResponse<?> updateStatus(@PathVariable Long id,@RequestBody Status in,java.security.Principal p,org.springframework.security.core.Authentication auth){
    if(!Set.of("PAID","SHIPPED","COMPLETED","REFUND_APPROVED","REFUND_REJECTED").contains(in.status()))throw new BusinessException("不支持的订单状态");
    var old=db.queryForList("select status,quantity,product_id from orders where id=?" ,id);if(old.isEmpty())throw new BusinessException("订单不存在");
    boolean merchant=auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_MERCHANT"));String scope=merchant?" and merchant_id=?":"";
    List<Object> args=new ArrayList<>();String timestamp="";
    if("SHIPPED".equals(in.status()))timestamp=",shipped_at=now()";if("COMPLETED".equals(in.status()))timestamp=",completed_at=now()";
    String sql="update orders set status=?"+timestamp+("REFUND_REJECTED".equals(in.status())?",refund_reason=?":"")+" where id=?"+scope;args.add(in.status());if("REFUND_REJECTED".equals(in.status()))args.add(in.reason());args.add(id);if(merchant)args.add(p.getName());
    int n=db.update(sql,args.toArray());if(n==0)throw new BusinessException("无权处理该订单");
    if("REFUND_APPROVED".equals(in.status())&&!"REFUND_APPROVED".equals(old.get(0).get("status")))db.update("update product set stock=stock+? where id=?",old.get(0).get("quantity"),old.get(0).get("product_id"));
    return ApiResponse.ok();
  }
}
