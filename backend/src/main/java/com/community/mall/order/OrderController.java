package com.community.mall.order;

import com.community.mall.common.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("hasRole('USER')")
public class OrderController {
  private final JdbcTemplate db;
  public OrderController(JdbcTemplate db){this.db=db;}

  @GetMapping
  public ApiResponse<?> list(java.security.Principal p){return ApiResponse.ok(db.queryForList("select * from orders where user_id=? order by created_at desc",p.getName()));}

  public record Create(@NotNull Long productId,@Min(1) Integer quantity,@NotBlank String receiverName,@NotBlank String receiverPhone,@NotBlank String receiverAddress){}
  public record CartCreate(List<Long> cartIds,@NotBlank String receiverName,@NotBlank String receiverPhone,@NotBlank String receiverAddress){}

  @PostMapping
  @Transactional
  public ApiResponse<?> create(@Valid @RequestBody Create in,java.security.Principal p){
    return ApiResponse.ok(createOrder(in.productId(),in.quantity(),in.receiverName(),in.receiverPhone(),in.receiverAddress(),p.getName()));
  }

  @PostMapping("/from-cart")
  @Transactional
  public ApiResponse<?> createFromCart(@Valid @RequestBody CartCreate in,java.security.Principal p){
    List<Map<String,Object>> rows;
    if(in.cartIds()==null || in.cartIds().isEmpty()) rows=db.queryForList("select c.id cart_id,c.product_id,p.name,p.cover,p.merchant_id,p.price,p.stock,p.status,c.quantity from shopping_cart c join product p on p.id=c.product_id where c.user_id=? for update",p.getName());
    else {String marks=String.join(",",Collections.nCopies(in.cartIds().size(),"?"));List<Object> args=new ArrayList<>();args.add(p.getName());args.addAll(in.cartIds());rows=db.queryForList("select c.id cart_id,c.product_id,p.name,p.cover,p.merchant_id,p.price,p.stock,p.status,c.quantity from shopping_cart c join product p on p.id=c.product_id where c.user_id=? and c.id in ("+marks+") for update",args.toArray());if(rows.size()!=new HashSet<>(in.cartIds()).size())throw new BusinessException("部分选中商品已从购物车移除，请刷新后重试");}
    if(rows.isEmpty()) throw new BusinessException("购物车中找不到选中的商品，可能已被移除");
    for(Map<String,Object> row:rows) if(!"ON_SALE".equals(row.get("status"))) throw new BusinessException("商品「"+row.get("name")+"」已下架，无法结算");
    List<Map<String,Object>> created=new ArrayList<>(); BigDecimal total=BigDecimal.ZERO;
    for(Map<String,Object> row:rows){int quantity=((Number)row.get("quantity")).intValue();long productId=((Number)row.get("product_id")).longValue();Map<String,Object> order=createOrder(productId,quantity,in.receiverName(),in.receiverPhone(),in.receiverAddress(),p.getName());created.add(order);total=total.add((BigDecimal)order.get("totalAmount"));db.update("delete from shopping_cart where id=? and user_id=?",row.get("cart_id"),p.getName());}
    return ApiResponse.ok(Map.of("orders",created,"totalAmount",total));
  }

  @PutMapping("/{id}/cancel") public ApiResponse<?> cancel(@PathVariable Long id,java.security.Principal p){int n=db.update("update orders set status='CANCELLED' where id=? and user_id=? and status='PENDING'",id,p.getName());if(n==0)throw new BusinessException("订单当前不能取消");return ApiResponse.ok();}
  @PutMapping("/{id}/confirm") public ApiResponse<?> confirm(@PathVariable Long id,java.security.Principal p){int n=db.update("update orders set status='COMPLETED',completed_at=now() where id=? and user_id=? and status='SHIPPED'",id,p.getName());if(n==0)throw new BusinessException("只有已发货订单可以确认收货");return ApiResponse.ok();}
  @PostMapping("/{id}/refund") public ApiResponse<?> refund(@PathVariable Long id,@RequestBody(required=false) Map<String,Object> body,java.security.Principal p){String reason=body==null?null:String.valueOf(body.getOrDefault("reason","用户申请退款"));int n=db.update("update orders set status='REFUND_REQUESTED',refund_reason=? where id=? and user_id=? and status in ('PENDING','PAID','SHIPPED','COMPLETED')",reason,id,p.getName());if(n==0)throw new BusinessException("订单当前不能申请退款");return ApiResponse.ok();}

  private Map<String,Object> createOrder(Long productId,int quantity,String receiverName,String receiverPhone,String receiverAddress,String userId){
    var rows=db.queryForList("select id,name,cover,merchant_id,price,stock from product where id=? and status='ON_SALE' for update",productId);if(rows.isEmpty())throw new BusinessException("商品不可购买");
    var x=rows.get(0);int stock=((Number)x.get("stock")).intValue();if(stock<quantity)throw new BusinessException("商品「"+x.get("name")+"」库存不足");
    BigDecimal total=((BigDecimal)x.get("price")).multiply(BigDecimal.valueOf(quantity));String no="CM"+System.currentTimeMillis()+UUID.randomUUID().toString().substring(0,4).toUpperCase();
    db.update("insert into orders(order_no,user_id,product_id,merchant_id,quantity,total_amount,receiver_name,receiver_phone,receiver_address,product_name_snapshot,product_cover_snapshot,unit_price_snapshot) values(?,?,?,?,?,?,?,?,?,?,?,?)",no,userId,productId,x.get("merchant_id"),quantity,total,receiverName,receiverPhone,receiverAddress,x.get("name"),x.get("cover"),x.get("price"));
    int updated=db.update("update product set stock=stock-? where id=? and status='ON_SALE' and stock>=?",quantity,productId,quantity);
    if(updated!=1) throw new BusinessException("商品「"+x.get("name")+"」库存不足");
    Long orderId = db.queryForObject("select id from orders where order_no=?", Long.class, no);
    return Map.of("orderId",orderId,"orderNo",no,"totalAmount",total,"productName",x.get("name"));
  }
}
