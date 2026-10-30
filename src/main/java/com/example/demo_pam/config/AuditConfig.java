package com.example.demo_pam.config;

import com.example.demo_pam.entity.User;
import com.example.demo_pam.security.AppUserPrincipal;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Configuration
// Activa la auditoría JPA para poblar @CreatedBy/@CreatedDate/@LastModifiedBy/@LastModifiedDate
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class AuditConfig {

    // Provee el usuario autenticado como auditor a partir del AppUserPrincipal del contexto de seguridad
    @Bean
    public AuditorAware<User> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal principal) {
                return Optional.of(principal.getUser());
            }
            return Optional.empty();
        };
    }
}
