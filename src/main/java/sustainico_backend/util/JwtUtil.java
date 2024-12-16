package sustainico_backend.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

//    @Value("${jwt.secretKey}")
//    private String secretKey;

    private final String secret = "qhgvqqx123o41hkvxqeo147c14hvc2oy32c5o2ychvc2o3y12ochkjc345o8g345gc314hc";

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractContactNo(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser().setSigningKey(secret).build().parseClaimsJws(token).getBody();
        } catch (Exception e) {
//            System.out.println("JWT token extraction failed: " + e.getMessage());
            throw e;
        }
    }

    public String getContactNoFromJWT(String token) {
        return extractAllClaims(token).get("contactNo", String.class);
    }


    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String userId) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userId, 1000 * 60 * 60 * 24 * 15);
    }

    public String generateRefreshToken(String contactNo) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, contactNo, 1000 * 60 * 60 * 24 * 15); // 15 days expiration
    }

    private String createToken(Map<String, Object> claims, String subject, long expirationTime) {
        try {
//            System.out.println("SECRET KEY :- " + secret);
            return Jwts.builder()
                    .setClaims(claims)
                    .setSubject(subject)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                    .signWith(SignatureAlgorithm.HS256, secret)
                    .compact();
        } catch (Exception err) {
            err.printStackTrace();
            return null;
        }
    }

    public Boolean validateToken(String token, String contactNo) {
        final String extractedContactNo = extractContactNo(token);
        return (extractedContactNo.equals(contactNo) && !isTokenExpired(token));
    }

    public FirebaseToken verifyFirebaseToken(String firebaseToken) throws Exception {
//        System.out.println("FIREBASE DECODE START!");
        return FirebaseAuth.getInstance().verifyIdToken(firebaseToken);
    }

}
