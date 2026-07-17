package com.anvesha.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "molecule")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Molecule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    @Column(name = "extracted_name_raw", nullable = false, length = 500)
    private String extractedNameRaw;

    @Column(name = "iupac_name", length = 500)
    private String iupacName;

    @Column(columnDefinition = "TEXT")
    private String smiles;

    @Column(name = "extraction_confidence", precision = 4, scale = 3)
    private BigDecimal extractionConfidence;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "molecule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<NoveltyScan> noveltyScans = new ArrayList<>();

    @OneToMany(mappedBy = "molecule", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PatentFiling> patentFilings = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
