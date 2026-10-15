package com.community.mall.security;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwt; public JwtAuthFilter(JwtService jwt) { this.jwt = jwt; }
  protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
    String h=req.getHeader("Authorization");
    if(h!=null && h.startsWith("Bearer ")) try { Claims c=jwt.parse(h.substring(7)); String role=c.get("role",String.class); var auth=new UsernamePasswordAuthenticationToken(c.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_"+role))); SecurityContextHolder.getContext().setAuthentication(auth); } catch(Exception ignored) {}
    chain.doFilter(req,res);
  }
}
