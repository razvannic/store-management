package local.dev.storemanager.application.dto;


public record LoginResponseDto(String accessToken, String tokenType, long expiresIn) {}
