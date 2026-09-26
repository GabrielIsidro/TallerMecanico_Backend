package com.taller.backend.core.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Generamos la llave criptográfica a partir del texto secreto
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // Extrae el email (username) de adentro de la pulserita VIP (Token)
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Revisa la fecha de vencimiento de la pulserita
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateTokenAdmin(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        String roles = userDetails.getAuthorities().toString();
        claims.put("role", roles); 
        return createToken(claims, userDetails.getUsername());
    }

    public String generateTokenTaller(com.taller.backend.modules.talleres.model.Usuario usuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "[ROLE_" + usuario.getRol().name() + "]"); 
        claims.put("taller_id", usuario.getTaller().getId().toString());
        return createToken(claims, usuario.getEmail());
    }

    public String extractTallerId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("taller_id", String.class);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject) // El "subject" es el email del usuario
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration)) // Usamos el valor inyectado
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // El guardia de seguridad usa esto para ver si el token es falso o verdadero
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
