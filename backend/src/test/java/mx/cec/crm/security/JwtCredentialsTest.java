package mx.cec.crm.security;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class JwtCredentialsTest {
    @Test
    void passwordChangeRevokesPreviouslyIssuedJwt() {
        var provider = new JwtTokenProvider("test-secret-with-at-least-thirty-two-bytes", 60000);
        String token = provider.generateToken(7L, "person@example.com", "old-bcrypt-hash");
        assertTrue(provider.validateToken(token));
        assertTrue(provider.matchesCredentials(token, "old-bcrypt-hash"));
        assertFalse(provider.matchesCredentials(token, "new-bcrypt-hash"));
        String fresh = provider.generateToken(7L, "person@example.com", "new-bcrypt-hash");
        assertTrue(provider.matchesCredentials(fresh, "new-bcrypt-hash"));
    }
}

