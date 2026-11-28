package edu.itc.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth

                        // Public GET requests
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // Public registration (must be BEFORE the /api/** rule)
                        .requestMatchers(HttpMethod.POST, "/api/register").permitAll()

                        // Only ADMIN can DELETE
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")

                        // Other API requests require authentication
                        .requestMatchers("/api/**").authenticated()

                        // Deny all remaining requests
                        .anyRequest().denyAll()
                )

                // Enable HTTP Basic
                .httpBasic(Customizer.withDefaults())

                // Disable form login
                .formLogin(form -> form.disable())

                // CSRF disabled: this chain accepts no cookie-based credentials (Basic / Bearer only)
                .csrf(csrf -> csrf.disable())

                // Stateless session management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Custom 401 and 403 responses
                .exceptionHandling(exceptions -> exceptions

                        // 401 Unauthorized
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(401);
                            response.setHeader("WWW-Authenticate", "Basic realm=\"Realm\"");
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType("application/problem+json");
                            response.getWriter().write("""
                        {"type":"about:blank","title":"Unauthorized","status":401,\
                        "detail":"Authentication required","instance":"%s"}
                        """.formatted(request.getRequestURI()));
                        })

                        // 403 Forbidden
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(403);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType("application/problem+json");
                            response.getWriter().write("""
                        {"type":"about:blank","title":"Forbidden","status":403,\
                        "detail":"Access denied","instance":"%s"}
                        """.formatted(request.getRequestURI()));
                        })
                );

        return http.build();
    }
}