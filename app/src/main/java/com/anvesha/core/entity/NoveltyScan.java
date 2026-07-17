package com.anvesha.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "novelty_scan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NoveltyScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "molecule_id", nullable = false)
    private Molecule molecule;

    /** 0.0 = identical to known compound, 1.0 = fully novel */
    @Column(name = "novelty_score", nullable = false, precision = 4, scale = 3)
    private BigDecimal noveltyScore;

    @Column(name = "is_novel", nullable = false)
    private boolean isNovel;

    /** PUBCHEM, CHEMBL, SURECHEMBL */
    @Column(name = "closest_match_source", length = 30)
    private String closestMatchSource;

    @Column(name = "closest_match_id", length = 100)
    private String closestMatchId;

    @Column(name = "tanimoto_similarity", precision = 4, scale = 3)
    private BigDecimal tanimotoSimilarity;

    @Column(name = "scanned_at", nullable = false, updatable = false)
    private OffsetDateTime scannedAt;

    @PrePersist
    protected void onScan() {
        this.scannedAt = OffsetDateTime.now();
    }
}
