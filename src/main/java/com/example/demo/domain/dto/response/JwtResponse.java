package com.example.demo.domain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {

    private String token;
    @Builder.Default
    private String type = "Bearer";
    private UserResponse user;

    public JwtResponse(String token, UserResponse user) {
        this.token = token;
        this.user = user;
    }
}
