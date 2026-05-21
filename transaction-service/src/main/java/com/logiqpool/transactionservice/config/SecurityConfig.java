package com.logiqpool.transactionservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import java.time.Duration; // ◀ IMPORT THIS
import javax.crypto.spec.SecretKeySpec;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // CRITICAL: This secret key MUST be exactly 256-bits (32 characters) long!
    //private static final String MOCK_SECRET = "my_ultra_secure_mock_secret_key_32_bytes!!";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
                // Disable CSRF because we are working with stateless JWTs
                .csrf(csrf -> csrf.disable())

                // Protect your endpoints
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**").permitAll() // Allow internal health checks
                        .anyRequest().authenticated()                // Everything else requires a valid token
                )

                // Turn on OAuth2 Resource Server support to automatically parse JWTs
                /*.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> {})
                );*/

                /*.oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(mockJwtDecoder())) // Tell Spring to use our local secret decoder
                );*/

                // ⚡ FIX: Explicitly bind your custom bypass decoder bean into the filter engine!
                    .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                );

        return http.build();
    }

    /*@Bean
    public JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {
        // Connects to Keycloak inside Docker to pull public verification keys
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();

        // Validates timestamps and cryptographic signatures, bypassing strict host string mismatches
        OAuth2TokenValidator<Jwt> validator = new JwtTimestampValidator();
        jwtDecoder.setJwtValidator(validator);

        return jwtDecoder;
    }*/

    /*@Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {

        // 1. Build the decoder using the internal Docker key set location
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // 2. Enforce standard cryptographic signature and timestamp checks
        //OAuth2TokenValidator<Jwt> defaultValidators = new JwtTimestampValidator();
        // 🚀 THE FIX: Give the timestamp verification a massive 5-hour grace window
        // to completely neutralize the Docker container time sync gap!

        // 🚀 THE FIX: Pass the 5-hour grace window directly into the constructor!
        OAuth2TokenValidator<Jwt> defaultValidators = new JwtTimestampValidator(Duration.ofHours(5));

        // 3. Explicitly enforce the validator to match the text token 'iss' (http://localhost:8080)
        OAuth2TokenValidator<Jwt> issuerValidator = new JwtIssuerValidator(issuerUri);

        // Combine them together cleanly without triggering auto-lookup errors
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(defaultValidators, issuerValidator);

        jwtDecoder.setJwtValidator(combinedValidator);
        return jwtDecoder;
    }*/

    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri) {

        // 1. Point the decoder strictly to the internal Docker network URL for cert download
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // 2. Add our 5-hour clock skew grace window for the Docker time gap
        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator(Duration.ofHours(5));

        // 3. Custom String-matching Issuer Validator: Prevents Spring from attempting a
        // broken background network call to http://localhost:8080 from inside the container.
        OAuth2TokenValidator<Jwt> customIssuerValidator = jwt -> {
            if (jwt.getIssuer() != null && jwt.getIssuer().toString().equals(issuerUri)) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_issuer", "The token issuer does not match the configured issuer-uri.", null)
            );
        };

        // Combine our custom string issuer check with the timestamp tolerance rules
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(timestampValidator, customIssuerValidator);

        jwtDecoder.setJwtValidator(combinedValidator);
        return jwtDecoder;
    }

    /*@Bean
    public JwtDecoder mockJwtDecoder() {
        // Creates a local signature validator using plain text symmetry
        return NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(MOCK_SECRET.getBytes(), "HmacSHA256")
        ).build();
    }*/
}