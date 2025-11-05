package com.example.shoppingcart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    InMemoryUserDetailsManager userDetailsService() {
        return new InMemoryUserDetailsManager(
            User.withUsername("admin")
                .password("{noop}admin123") // solo para desarrollo
                .roles("ADMIN")
                .build()
        );
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
          .csrf(csrf -> csrf.disable()) // luego ajustamos según frontend
          .authorizeHttpRequests(auth -> auth
              .requestMatchers("/api/health", "/actuator/health").permitAll()
              .anyRequest().authenticated()
          )
          .httpBasic(Customizer.withDefaults())
          .formLogin(Customizer.withDefaults()); // para probar sesiones rápidamente
        return http.build();
    }
}
