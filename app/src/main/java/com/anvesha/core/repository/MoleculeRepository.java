package com.anvesha.core.repository;

import com.anvesha.core.entity.Molecule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoleculeRepository extends JpaRepository<Molecule, Long> {

    List<Molecule> findByPaperId(Long paperId);

    /** Count novel molecules found for an institution this month */
    @Query("SELECT COUNT(m) FROM Molecule m " +
           "JOIN m.noveltyScans ns " +
           "WHERE m.paper.institution.id = :instId AND ns.isNovel = true " +
           "AND m.createdAt >= :startOfMonth")
    long countNovelMoleculesThisMonth(Long instId, java.time.OffsetDateTime startOfMonth);
}
