package com.anvesha.core.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "institution")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** IIT, NIT, CSIR_LAB, PRIVATE_UNIV, OTHER */
    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "tto_contact_email")
    private String ttoContactEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "institution", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AppUser> users = new ArrayList<>();

    @OneToMany(mappedBy = "institution", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Paper> papers = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
