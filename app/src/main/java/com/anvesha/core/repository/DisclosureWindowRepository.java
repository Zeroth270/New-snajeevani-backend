package com.anvesha.core.repository;

import com.anvesha.core.entity.DisclosureWindow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DisclosureWindowRepository extends JpaRepository<DisclosureWindow, Long> {

    Page<DisclosureWindow> findByStatus(String status, Pageable pageable);

    Page<DisclosureWindow> findByPaperInstitutionIdAndStatus(Long institutionId, String status, Pageable pageable);

    /** All OPEN windows whose deadline has not been filed — used by the daily scheduler */
    @Query("SELECT dw FROM DisclosureWindow dw WHERE dw.status IN ('OPEN', 'CLOSING_SOON')")
    List<DisclosureWindow> findAllActiveWindows();

    /** Dashboard: windows closing within X days for an institution */
    @Query("SELECT COUNT(dw) FROM DisclosureWindow dw " +
           "WHERE dw.paper.institution.id = :instId " +
           "AND dw.status = 'CLOSING_SOON'")
    long countClosingSoonByInstitution(Long instId);

    /** Dashboard: expired and not filed */
    @Query("SELECT COUNT(dw) FROM DisclosureWindow dw " +
           "WHERE dw.paper.institution.id = :instId " +
           "AND dw.status = 'EXPIRED'")
    long countExpiredByInstitution(Long instId);

    /** Check if an alert of a given type was already sent for this window to avoid duplicates */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END " +
           "FROM Alert a WHERE a.disclosureWindow.id = :windowId AND a.alertType = :alertType")
    boolean alertAlreadySent(Long windowId, String alertType);
}
