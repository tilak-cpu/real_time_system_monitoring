package com.neurosys.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "lab_enrollment_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabEnrollmentCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lab_id", nullable = false)
    private Lab lab;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "max_uses")
    @Builder.Default
    private Integer maxUses = 100;

    @Column(name = "current_uses")
    @Builder.Default
    private Integer currentUses = 0;

    @Column(name = "revoked")
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
