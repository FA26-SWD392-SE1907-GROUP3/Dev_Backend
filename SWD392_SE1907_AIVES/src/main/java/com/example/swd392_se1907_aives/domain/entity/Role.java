package com.example.swd392_se1907_aives.domain.entity;

import com.example.swd392_se1907_aives.domain.enums.RoleName;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "RoleID")
    private Integer roleId;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    @Column(name = "RoleName", nullable = false, unique = true, length = 50)
    private RoleName roleName;

    @Column(name = "Description", length = 255)
    private String description;
}
