package com.anvesha.core.repository;

import com.anvesha.core.entity.PatentFiling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatentFilingRepository extends JpaRepository<PatentFiling, Long> {

    List<PatentFiling> findByMoleculeId(Long moleculeId);

    List<PatentFiling> findByFiledById(Long userId);
}
