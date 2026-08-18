package mx.cec.crm.dto;

public record AuthResponse(
        String token,
        String name,
        String email,
        String role
) {}
