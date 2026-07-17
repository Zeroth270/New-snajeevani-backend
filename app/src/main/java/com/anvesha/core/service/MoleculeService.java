package com.anvesha.core.service;

import com.anvesha.core.dto.molecule.MoleculeResponse;
import com.anvesha.core.dto.scan.NoveltyScanResponse;
import com.anvesha.core.entity.*;
import com.anvesha.core.exception.ResourceNotFoundException;
import com.anvesha.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoleculeService {

    private final MoleculeRepository moleculeRepository;
    private final NoveltyScanRepository noveltyScanRepository;
    private final AlertRepository alertRepository;
    private final AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public MoleculeResponse getById(Long id) {
        Molecule mol = moleculeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Molecule", id));
        NoveltyScanResponse latestScan = noveltyScanRepository
                .findTopByMoleculeIdOrderByScannedAtDesc(id)
                .map(NoveltyScanResponse::from)
                .orElse(null);
        return MoleculeResponse.from(mol, latestScan);
    }

    @Transactional(readOnly = true)
    public List<MoleculeResponse> listForPaper(Paper paper) {
        return moleculeRepository.findByPaperId(paper.getId()).stream()
                .map(mol -> {
                    NoveltyScanResponse scan = noveltyScanRepository
                            .findTopByMoleculeIdOrderByScannedAtDesc(mol.getId())
                            .map(NoveltyScanResponse::from)
                            .orElse(null);
                    return MoleculeResponse.from(mol, scan);
                })
                .toList();
    }

    /**
     * Create a NOVEL_MOLECULE_FOUND alert for all TTO officers of the institution
     * that owns the paper where this molecule was extracted.
     */
    @Transactional
    public void createNovelMoleculeAlert(Molecule mol, Paper paper) {
        if (paper.getInstitution() == null) return;

        List<AppUser> ttoUsers = userRepository
                .findTtoOfficersByInstitution(paper.getInstitution().getId());

        for (AppUser officer : ttoUsers) {
            Alert alert = Alert.builder()
                    .recipientUser(officer)
                    .alertType("NOVEL_MOLECULE_FOUND")
                    .message(String.format(
                            "Novel molecule detected: '%s' (SMILES: %s) in paper '%s'",
                            mol.getExtractedNameRaw(), mol.getSmiles(), paper.getTitle()))
                    .isRead(false)
                    .build();
            alertRepository.save(alert);
            log.info("Novel molecule alert created for user {}", officer.getEmail());
        }
    }
}
