package com.mcardoso.srvcondominiopro.config;

import com.mcardoso.srvcondominiopro.shared.security.JwtFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CorsProperties corsProperties;

    public SecurityConfig(JwtFilter jwtFilter, CorsProperties corsProperties) {
        this.jwtFilter = jwtFilter;
        this.corsProperties = corsProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(corsProperties.allowedOriginPatterns());
        config.setAllowedMethods(corsProperties.allowedMethods());
        config.setAllowedHeaders(corsProperties.allowedHeaders());
        config.setAllowCredentials(corsProperties.allowCredentials());
        config.setMaxAge(corsProperties.maxAgeSeconds());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                                "/api/v1/auth/refresh-token").permitAll()
                        .requestMatchers("/api/v1/moradores/convite/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/pagamentos/webhook/**").permitAll()
                        .requestMatchers("/api/v1/moradores/me", "/api/v1/moradores/me/reservas",
                                "/api/v1/moradores/me/preferencias-notificacoes")
                            .hasAnyRole("PROPRIETARIO", "INQUILINO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/reservas", "/api/v1/reservas/validar")
                            .hasAnyRole("PROPRIETARIO", "INQUILINO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/pagamentos/criar-cobranca")
                            .hasAnyRole("PROPRIETARIO", "INQUILINO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/avisos/nao-lidos")
                            .hasAnyRole("PROPRIETARIO", "INQUILINO")
                        .requestMatchers(HttpMethod.POST, "/api/v1/avisos/*/marcar-lido")
                            .hasAnyRole("PROPRIETARIO", "INQUILINO")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/reservas/*/aprovar", "/api/v1/reservas/*/rejeitar")
                            .hasRole("SINDICO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/areas/*/reservas").hasRole("SINDICO")
                        .requestMatchers(HttpMethod.GET, "/api/v1/condominios/*/areas", "/api/v1/areas/**").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/condominios/*/avisos", "/api/v1/avisos/**",
                                "/api/v1/condominios/*/faqs", "/api/v1/condominios/*/faqs/**", "/api/v1/faqs/**")
                            .authenticated()
                        .requestMatchers("/api/v1/condominios/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/unidades/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/moradores/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/areas/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/avisos/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/faqs/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/notificacoes/**").hasRole("SINDICO")
                        .requestMatchers("/api/v1/reservas/**").authenticated()
                        .requestMatchers("/api/v1/pagamentos/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
