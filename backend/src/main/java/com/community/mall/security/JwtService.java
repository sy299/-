package com.community.mall.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.util.Date;
@Service
public class JwtService {
  private final SecretKey key; private final long expireMs;
  public JwtService(@Value("${mall.jwt-secret}") String secret, @Value("${mall.jwt-expire-hours:24}") long hours) {
    byte[] bytes = Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(secret.getBytes()));
    this.key = Keys.hmacShaKeyFor(bytes); this.expireMs = hours * 3600_000L;
  }
  public String create(Long userId, String username, String role) { Date now = new Date(); return Jwts.builder().subject(String.valueOf(userId)).claim("username", username).claim("role", role).issuedAt(now).expiration(new Date(now.getTime()+expireMs)).signWith(key).compact(); }
  public Claims parse(String token) { return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload(); }
}
