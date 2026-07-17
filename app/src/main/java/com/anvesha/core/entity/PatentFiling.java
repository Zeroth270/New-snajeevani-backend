package com.anvesha.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "patent_filing")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatentFiling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "molecule_id", nullable = false)
    private Molecule molecule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "filed_by")
    private AppUser filedBy;

    @Column(name = "filing_date")
    private LocalDate filingDate;

    /** e.g. INDIA_IPO, USPTO */
    @Column(name = "patent_office", length = 50)
    private String patentOffice;

    @Column(name = "application_number", length = 100)
    private String applicationNumber;

    /** FILED, GRANTED, REJECTED, WITHDRAWN */
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "FILED";
}
