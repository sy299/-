package com.community.mall.address;
import com.community.mall.common.ApiResponse;
import com.community.mall.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/addresses") @PreAuthorize("hasRole('USER')")
public class AddressController {
  private final JdbcTemplate db; public AddressController(JdbcTemplate db){this.db=db;}
  @GetMapping public ApiResponse<?> list(java.security.Principal p){return ApiResponse.ok(db.queryForList("select * from address where user_id=? order by is_default desc,id desc",p.getName()));}
  public record AddressInput(@NotBlank String receiverName,@NotBlank String receiverPhone,@NotBlank String detailAddress,Boolean isDefault){}
  @PostMapping public ApiResponse<?> add(@Valid @RequestBody AddressInput in,java.security.Principal p){
    boolean isDefault=Boolean.TRUE.equals(in.isDefault()); if(isDefault) db.update("update address set is_default=0 where user_id=?",p.getName());
    db.update("insert into address(user_id,receiver_name,receiver_phone,detail_address,is_default) values(?,?,?,?,?)",p.getName(),in.receiverName(),in.receiverPhone(),in.detailAddress(),isDefault);return ApiResponse.ok();
  }
  @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable Long id,@Valid @RequestBody AddressInput in,java.security.Principal p){
    if(isDefault(in.isDefault())) db.update("update address set is_default=0 where user_id=?",p.getName());
    int n=db.update("update address set receiver_name=?,receiver_phone=?,detail_address=?,is_default=? where id=? and user_id=?",in.receiverName(),in.receiverPhone(),in.detailAddress(),isDefault(in.isDefault()),id,p.getName()); if(n==0) throw new BusinessException("收货地址不存在"); return ApiResponse.ok();
  }
  @PutMapping("/{id}/default") public ApiResponse<?> setDefault(@PathVariable Long id,java.security.Principal p){db.update("update address set is_default=0 where user_id=?",p.getName());int n=db.update("update address set is_default=1 where id=? and user_id=?",id,p.getName());if(n==0)throw new BusinessException("收货地址不存在");return ApiResponse.ok();}
  @DeleteMapping("/{id}") public ApiResponse<?> delete(@PathVariable Long id,java.security.Principal p){db.update("delete from address where id=? and user_id=?",id,p.getName());return ApiResponse.ok();}
  private boolean isDefault(Boolean value){return Boolean.TRUE.equals(value);}
}
