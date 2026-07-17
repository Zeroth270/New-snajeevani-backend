package com.anvesha.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "disclosure_window")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DisclosureWindow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    /** Date the paper/thesis was first publicly disclosed */
    @Column(name = "disclosure_date", nullable = false)
    private LocalDate disclosureDate;

    /** Grace period in days — 365 by default (Indian Patents Act §31) */
    @Column(name = "grace_period_days", nullable = false)
    @Builder.Default
    private int gracePeriodDays = 365;

    /** disclosureDate + gracePeriodDays */
    @Column(name = "deadline_date", nullable = false)
    private LocalDate deadlineDate;

    /** Whether the disclosure qualifies for §31 grace — needs legal judgment, TTO must confirm */
    @Column(name = "qualifies_for_section_31", nullable = false)
    @Builder.Default
    private boolean qualifiesForSection31 = false;

    /** OPEN, CLOSING_SOON, EXPIRED, FILED */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "disclosureWindow", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Alert> alerts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
