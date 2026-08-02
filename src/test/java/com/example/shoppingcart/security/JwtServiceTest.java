package com.example.shoppingcart.security;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private final JwtService jwtService =
            new JwtService("unit-test-secret-key-needs-32-bytes-min", 60);

    @Test
    void generateToken_thenExtractUsername_roundTrips() {
        String token = jwtService.generateToken("luis");

        assertEquals("luis", jwtService.extractUsername(token));
    }

    @Test
    void extractUsername_rejectsTokenSignedWithDifferentSecret() {
        JwtService otherService =
                new JwtService("another-secret-key-also-needs-32-bytes-min", 60);
        String token = otherService.generateToken("luis");

        assertThrows(JwtException.class, () -> jwtService.extractUsername(token));
    }

    @Test
    void extractUsername_rejectsGarbageToken() {
        assertThrows(JwtException.class, () -> jwtService.extractUsername("not-a-real-token"));
    }
}
