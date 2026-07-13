package com.example.hotel_management_system.model.entities;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING) // Guarda el nombre (ej. "ADMIN") y no el número (0)
    @Column(nullable = false)
    private Role role;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Este metodo se ejecuta automáticamente justo antes de guardar en base de datos
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- MÉTODOS OBLIGATORIOS DE USERDETAILS ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return this.email; // 👈 2. CRUCIAL: Para tu sistema, el "username" es el email
    }

    @Override
    public String getPassword() {
        return this.passwordHash; // Retorna la contraseña encriptada de la BD
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Cuenta siempre activa
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Cuenta nunca bloqueada
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Credenciales siempre válidas
    }

    @Override
    public boolean isEnabled() {
        return true; // Usuario siempre habilitado
    }
}
