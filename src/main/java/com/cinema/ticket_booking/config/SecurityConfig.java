package com.cinema.ticket_booking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password4j.Argon2Password4jPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import com.cinema.ticket_booking.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.security.csrf-enabled:false}")
    private boolean csrfEnabled;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2Password4jPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> { throw new UsernameNotFoundException("JWT authentication is used for " + username); };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        if (csrfEnabled) {
            http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
        } else {
            http.csrf(csrf -> csrf.disable());
        }

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/movies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/movies/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/movies/**").hasRole("ADMIN")
                        .requestMatchers("/api/tickets/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://192.168.49.2:30080")
        );
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(
                List.of("Authorization", "Content-Type", "X-Correlation-Id")
        );
        configuration.setExposedHeaders(
                List.of("X-Correlation-Id")
        );

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}
