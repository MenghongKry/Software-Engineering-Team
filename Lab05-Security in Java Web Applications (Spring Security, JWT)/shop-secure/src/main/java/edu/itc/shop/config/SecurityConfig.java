
package edu.itc.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain api(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(auth -> auth

                // Public GET requests
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/products/**"
                ).permitAll()

                // Only ADMIN can DELETE
                .requestMatchers(
                        HttpMethod.DELETE,
                        "/api/products/**"
                ).hasRole("ADMIN")

                // Other API requests require authentication
                .requestMatchers("/api/**").authenticated()

                // Deny all remaining requests
                .anyRequest().denyAll()
        )

        // Enable HTTP Basic
        .httpBasic(Customizer.withDefaults())

        // Disable form login
        .formLogin(form -> form.disable())

        // Safe for this stateless Basic/Bearer-only API.
        .csrf(csrf -> csrf.disable())

        // Stateless session management
        .sessionManagement(session ->
                session.sessionCreationPolicy(
                        SessionCreationPolicy.STATELESS
                )
        )

        // Custom 401 and 403 responses
        .exceptionHandling(exceptions -> exceptions

                // 401 Unauthorized
                .authenticationEntryPoint((request, response, ex) -> {

                    response.setStatus(401);
                    response.setContentType(
                            "application/problem+json"
                    );

                    response.getWriter().write("""
                        {
                            "type": "about:blank",
                            "title": "Unauthorized",
                            "status": 401,
                            "detail": "Authentication required"
                        }
                        """);
                })

                // 403 Forbidden
                .accessDeniedHandler((request, response, ex) -> {

                    response.setStatus(403);
                    response.setContentType(
                            "application/problem+json"
                    );

                    response.getWriter().write("""
                        {
                            "type": "about:blank",
                            "title": "Forbidden",
                            "status": 403,
                            "detail": "Access denied"
                        }
                        """);
                })
        );

        return http.build();
    }

    // Temporary in-memory users
    @Bean
    UserDetailsService temporaryUsers() {

        return new InMemoryUserDetailsManager(

                User.withUsername("admin")
                        .password("{noop}admin")
                        .roles("ADMIN")
                        .build(),

                User.withUsername("alice")
                        .password("{noop}alice")
                        .roles("CUSTOMER")
                        .build()
        );
    }
}
