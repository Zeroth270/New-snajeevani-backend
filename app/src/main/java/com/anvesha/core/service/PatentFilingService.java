package com.anvesha.core.service;

import com.anvesha.core.dto.filing.PatentFilingRequest;
import com.anvesha.core.dto.filing.PatentFilingResponse;
import com.anvesha.core.entity.*;
import com.anvesha.core.exception.ResourceNotFoundException;
import com.anvesha.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatentFilingService {

    private final PatentFilingRepository filingRepository;
    private final MoleculeRepository moleculeRepository;
    private final AppUserRepository userRepository;
    private final DisclosureWindowRepository windowRepository;

    @Transactional
    public PatentFilingResponse record(Long moleculeId, PatentFilingRequest req, UserDetails principal) {
        Molecule molecule = moleculeRepository.findById(moleculeId)
                .orElseThrow(() -> new ResourceNotFoundException("Molecule", moleculeId));

        AppUser filer = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        PatentFiling filing = PatentFiling.builder()
                .molecule(molecule)
                .filedBy(filer)
                .filingDate(req.filingDate())
                .patentOffice(req.patentOffice())
                .applicationNumber(req.applicationNumber())
                .status("FILED")
                .build();

        filing = filingRepository.save(filing);

        // Mark any OPEN/CLOSING_SOON DisclosureWindow for this paper as FILED
        molecule.getPaper().getDisclosureWindows().stream()
                .filter(dw -> "OPEN".equals(dw.getStatus()) || "CLOSING_SOON".equals(dw.getStatus()))
                .forEach(dw -> {
                    dw.setStatus("FILED");
                    windowRepository.save(dw);
                });

        return PatentFilingResponse.from(filing);
    }
}
