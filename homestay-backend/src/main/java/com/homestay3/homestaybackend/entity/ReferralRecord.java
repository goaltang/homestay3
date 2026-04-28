package com.homestay3.homestaybackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "referral_record",
        indexes = {
                @Index(name = "idx_code", columnList = "referral_code"),
                @Index(name = "idx_inviter", columnList = "inviter_id, status")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "referral_code", nullable = false, unique = true, length = 32)
    private String referralCode;

    @Column(name = "inviter_id", nullable = false)
    private Long inviterId;

    @Column(name = "template_id_for_invitee")
    private Long templateIdForInvitee;

    @Column(name = "template_id_for_inviter")
    private Long templateIdForInviter;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "max_uses", nullable = false)
    @Builder.Default
    private Integer maxUses = 1;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
