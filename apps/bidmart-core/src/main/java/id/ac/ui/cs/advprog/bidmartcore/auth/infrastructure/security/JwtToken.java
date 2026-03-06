package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class JwtToken {
    private String token;
    private Instant expirationTime;
}
