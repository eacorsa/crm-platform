package mx.cec.crm.service;

import lombok.RequiredArgsConstructor;
import mx.cec.crm.dto.AuthResponse;
import mx.cec.crm.dto.LoginRequest;
import mx.cec.crm.entity.User;
import mx.cec.crm.repository.UserRepository;
import mx.cec.crm.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final LoginRateLimiter rateLimiter;

    public AuthResponse login(LoginRequest request) {
        String key = request.email().trim().toLowerCase(Locale.ROOT);
        rateLimiter.acquire(key);
        User user = userRepository.findByEmailAndActiveTrue(key)
                .orElseThrow(() -> new BadCredentialsException("Credenciales incorrectas."));
        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new BadCredentialsException("Credenciales incorrectas.");
        rateLimiter.reset(key);
        String token = tokenProvider.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }
}
