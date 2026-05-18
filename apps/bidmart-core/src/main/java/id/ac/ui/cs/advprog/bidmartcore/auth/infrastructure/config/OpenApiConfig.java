package id.ac.ui.cs.advprog.bidmartcore.auth.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bidmartOpenAPI() {
        final String bearerSchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("BidMart Auth API")
                        .description("""
                                Authentication & Authorization API for the BidMart bidding platform.
                                
                                ## Authentication Flow
                                1. **Register** a new account via `POST /api/auth/register`
                                2. **Login** via `POST /api/auth/login`
                                   - If 2FA is disabled → returns `accessToken` + `refreshToken`
                                   - If 2FA is enabled → returns `preAuthToken` + `mfaType`
                                3. **Complete 2FA** (if required) via `POST /api/auth/mfa/verify`
                                4. Use the `accessToken` as `Authorization: Bearer <token>` header
                                5. **Refresh** expired access tokens via `POST /api/auth/refresh`
                                
                                ## Session Management
                                - Max **5 active sessions** per user (configurable)
                                - Users can list and revoke individual sessions
                                
                                ## 2FA Support
                                - **TOTP** (Google Authenticator compatible)
                                - **Email OTP** (6-digit code)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("BidMart Team")
                                .url("https://github.com/bidmart")))
                .components(new Components()
                        .addSecuritySchemes(bearerSchemeName, new SecurityScheme()
                                .name(bearerSchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Access Token — obtain via `/api/auth/login` or `/api/auth/mfa/verify`")))
                .addSecurityItem(new SecurityRequirement().addList(bearerSchemeName))
                .tags(List.of(
                        new Tag().name("Authentication").description("Register, Login, Logout, Token Refresh"),
                        new Tag().name("Session Management").description("List, revoke, and manage active sessions"),
                        new Tag().name("Profile").description("View and update user profile, account deactivation"),
                        new Tag().name("Multi-Factor Authentication").description("Setup/manage TOTP and Email OTP 2FA")
                ));
    }
}

