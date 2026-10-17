package com.community.mall.auth;
import com.community.mall.common.*;
import com.community.mall.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/auth")
public class AuthController {
  private final JdbcTemplate db; private final PasswordEncoder encoder; private final JwtService jwt;
  public AuthController(JdbcTemplate db, PasswordEncoder encoder, JwtService jwt){this.db=db;this.encoder=encoder;this.jwt=jwt;}
  public record Login(@NotBlank String username,@NotBlank String password){}
  public record Register(@NotBlank String username,@NotBlank @Size(min=6,max=72) String password,@NotBlank String nickname,String avatar,String phone){}
  public record Profile(@NotBlank @Size(max = 50) String nickname,
                         @Size(max = 500) String avatar,
                         @Size(max = 30) String phone){}
  @PostMapping("/login") public ApiResponse<?> login(@Valid @RequestBody Login in){
    var rows=db.queryForList("select id,username,password,nickname,avatar,role from sys_user where username=? and enabled=1",in.username());
    if(rows.isEmpty() || !encoder.matches(in.password(),(String)rows.get(0).get("password"))) throw new BusinessException("用户名或密码错误");
    var u=rows.get(0); String token=jwt.create(((Number)u.get("id")).longValue(),(String)u.get("username"),(String)u.get("role"));
    var safeUser = new java.util.HashMap<String,Object>(u); safeUser.remove("password");
    return ApiResponse.ok(Map.of("token",token,"user",safeUser));
  }
  @PostMapping("/register") public ApiResponse<?> register(@Valid @RequestBody Register in){
    if(!db.queryForList("select id from sys_user where username=?",in.username()).isEmpty()) throw new BusinessException("用户名已存在");
    db.update("insert into sys_user(username,password,nickname,avatar,phone,role) values(?,?,?,?,?,'USER')",in.username(),encoder.encode(in.password()),in.nickname(),in.avatar(),in.phone());
    return ApiResponse.ok();
  }
  @GetMapping("/me") public ApiResponse<?> me(java.security.Principal p){ return ApiResponse.ok(db.queryForMap("select id,username,nickname,avatar,phone,role from sys_user where id=?",p.getName())); }
  @PutMapping("/profile") public ApiResponse<?> profile(@Valid @RequestBody Profile in,java.security.Principal p){
    db.update("update sys_user set nickname=?,avatar=?,phone=? where id=?",in.nickname(),in.avatar(),in.phone(),p.getName());
    return ApiResponse.ok(db.queryForMap("select id,username,nickname,avatar,phone,role from sys_user where id=?",p.getName()));
  }
}
