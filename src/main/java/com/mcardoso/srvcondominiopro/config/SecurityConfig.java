package com.mcardoso.srvcondominiopro.config;

import com.mcardoso.srvcondominiopro.shared.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/moradores/convite/**").permitAll()
                        .requestMatchers("/api/v1/moradores/me", "/api/v1/moradores/me/reservas")
                            .hasAnyRole("PROPRIETARIO", "INQUILINO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservas", "/api/v1/reservas/validar")
                            .hasAnyRole("PROPRIETARIO", "INQUILINO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservas/*/aprovar", "/api/v1/reservas/*/rejeitar")
                            .hasRole("SINDICO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/areas/*/reservas").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/condominios/*/areas", "/api/v1/areas/**").authenticated()
                        .requestMatchers("/api/v1/condominios/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/unidades/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/moradores/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/areas/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/reservas/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
