package fi.haagahelia.messenger.service;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import fi.haagahelia.messenger.dto.AccessTokenPayloadDto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;

@Service
public class JwtService {
	private final long EXPIRATION_TIME = Duration.ofHours(8).toMillis();
	private final String PREFIX = "Bearer ";

	@Value("${auth.jwt-secret}")
	private String jwtSecret;

	public AccessTokenPayloadDto getAccessToken(String username) {
		Instant expiresAt = Instant.now().plusMillis(EXPIRATION_TIME);

		String accessToken = Jwts.builder().subject(username).expiration(Date.from(expiresAt))
				.signWith(getSigningKey())
				.compact();

		return new AccessTokenPayloadDto(accessToken, expiresAt);
	}

	public String getAuthUser(HttpServletRequest request) {
		String authorizationHeaderValue = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (authorizationHeaderValue == null) {
			return null;
		}

		try {
			String user = getJwtParser()
					.parseSignedClaims(authorizationHeaderValue.replace(PREFIX, ""))
					.getPayload()
					.getSubject();

			return user;
		} catch (Exception e) {
			return null;
		}
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	private JwtParser getJwtParser() {
		return Jwts.parser().verifyWith(getSigningKey()).build();
	}
}
