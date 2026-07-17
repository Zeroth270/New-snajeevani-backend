package com.anvesha.core.repository;

import com.anvesha.core.entity.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaperRepository extends JpaRepository<Paper, Long> {

    Page<Paper> findByInstitutionId(Long institutionId, Pageable pageable);

    Page<Paper> findByUploadedById(Long userId, Pageable pageable);

    Page<Paper> findByInstitutionIdAndStatus(Long institutionId, String status, Pageable pageable);

    /** Dashboard: count papers processed this month for an institution */
    @Query("SELECT COUNT(p) FROM Paper p WHERE p.institution.id = :instId " +
           "AND p.status = 'PROCESSED' " +
           "AND p.createdAt >= :startOfMonth")
    long countProcessedThisMonth(Long instId, java.time.OffsetDateTime startOfMonth);

    Optional<Paper> findByIdAndInstitutionId(Long id, Long institutionId);

    Optional<Paper> findByIdAndUploadedById(Long id, Long userId);
}
