package com.anvesha.core.repository;

import com.anvesha.core.entity.NoveltyScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NoveltyScanRepository extends JpaRepository<NoveltyScan, Long> {

    /** Retrieve the most recent scan for a molecule */
    Optional<NoveltyScan> findTopByMoleculeIdOrderByScannedAtDesc(Long moleculeId);
}
