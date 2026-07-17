package com.anvesha.core.repository;

import com.anvesha.core.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Used by the scheduler to find TTO officers for an institution */
    @Query("SELECT u FROM AppUser u WHERE u.institution.id = :institutionId AND u.role IN ('TTO_OFFICER', 'INSTITUTION_ADMIN')")
    List<AppUser> findTtoOfficersByInstitution(Long institutionId);

    List<AppUser> findByInstitutionId(Long institutionId);
}
