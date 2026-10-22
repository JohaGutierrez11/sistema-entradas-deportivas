package ec.edu.uce.tickets.reservation.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String CLIENTE = "CLIENTE";
    private static final String ADMIN = "ADMIN";

    @Bean
    @SuppressWarnings("java:S4502")
    public SecurityFilterChain securityFilterChain(HttpSecurity http){
        http
                // CSRF desactivado A PROPÓSITO (revisado, java:S4502): API sin estado con JWT en el header
                // Authorization. El navegador no envía ese header por su cuenta, así que un sitio ajeno
                // no puede falsificar peticiones. Si algún día el token viaja en una cookie, hay que reactivarlo.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/reservation-api/v1/reservations").hasRole(CLIENTE)
                        .requestMatchers(HttpMethod.POST, "/reservation-api/v1/reservations/*/confirm").hasRole(CLIENTE)
                        .requestMatchers(HttpMethod.POST, "/reservation-api/v1/reservations/*/cancel").hasAnyRole(CLIENTE, ADMIN)
                        .requestMatchers("/reservation-api/v1/reservations/me", "/reservation-api/v1/reservations/me/**").hasRole(CLIENTE)
                        .requestMatchers("/reservation-api/v1/reports/**").hasRole(ADMIN)
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}