package com.example.swd392_se1907_aives.domain.entity;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
@Entity
@DynamicInsert
@Table(name="users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    @Column(name="id", nullable=false)
    private UUID id;
    @Column(name="full_name", nullable=false, length=150)
    private String fullName;
    @Column(name="email", nullable=false, length=255)
    private String email;
    @Column(name="password_hash", nullable=false)
    private String passwordHash;
    @Column(name="role", nullable=false, length=20)
    private String role;
    @Column(name="is_active", nullable=false)
    private Boolean isActive;
    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false)
    private OffsetDateTime updatedAt;
}
