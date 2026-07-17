package com.anvesha.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "alert")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_user_id", nullable = false)
    private AppUser recipientUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disclosure_window_id")
    private DisclosureWindow disclosureWindow;

    /** DEADLINE_30D, DEADLINE_7D, EXPIRED, NOVEL_MOLECULE_FOUND */
    @Column(name = "alert_type", nullable = false, length = 30)
    private String alertType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
