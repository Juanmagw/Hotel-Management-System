package com.example.hotel_management_system.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@Profile("!test")
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/users/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/users/*/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/rooms/available")
                            .hasAnyRole("ADMIN", "RECEPTIONIST", "GUEST")
                        .requestMatchers(HttpMethod.GET, "/api/room-types", "/api/room-types/**")
                            .hasAnyRole("ADMIN", "RECEPTIONIST", "GUEST")
                        .requestMatchers(HttpMethod.POST, "/api/room-types", "/api/room-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/room-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/room-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/rooms", "/api/rooms/*")
                            .hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.POST, "/api/rooms", "/api/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/rooms/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/rooms/**")
                            .hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.GET, "/api/reservations", "/api/reservations/*")
                            .hasAnyRole("ADMIN", "RECEPTIONIST", "GUEST")
                        .requestMatchers(HttpMethod.POST, "/api/reservations")
                            .hasAnyRole("ADMIN", "RECEPTIONIST", "GUEST")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservations/*/status")
                            .hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.DELETE, "/api/reservations/*")
                            .hasAnyRole("ADMIN", "RECEPTIONIST", "GUEST")
                        .requestMatchers(HttpMethod.GET, "/api/reservations/*/payment")
                            .hasAnyRole("ADMIN", "RECEPTIONIST", "GUEST")
                        .requestMatchers(HttpMethod.POST, "/api/payments")
                            .hasAnyRole("ADMIN", "RECEPTIONIST", "GUEST")
                        .requestMatchers(HttpMethod.GET, "/api/payments/*")
                            .hasAnyRole("ADMIN", "RECEPTIONIST", "GUEST")
                        .requestMatchers(HttpMethod.PATCH, "/api/payments/*/status")
                            .hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers("/api/users/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
