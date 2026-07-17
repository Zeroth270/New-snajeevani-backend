package com.anvesha.core.service;

import com.anvesha.core.dto.dashboard.DashboardSummaryResponse;
import com.anvesha.core.entity.AppUser;
import com.anvesha.core.exception.ResourceNotFoundException;
import com.anvesha.core.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AppUserRepository userRepository;
    private final PaperRepository paperRepository;
    private final MoleculeRepository moleculeRepository;
    private final DisclosureWindowRepository windowRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(UserDetails principal) {
        AppUser caller = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Long instId = caller.getInstitution() != null ? caller.getInstitution().getId() : null;

        OffsetDateTime startOfMonth = OffsetDateTime.now()
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        long papersThisMonth = instId != null
                ? paperRepository.countProcessedThisMonth(instId, startOfMonth) : 0L;

        long novelMolecules = instId != null
                ? moleculeRepository.countNovelMoleculesThisMonth(instId, startOfMonth) : 0L;

        long closingSoon = instId != null
                ? windowRepository.countClosingSoonByInstitution(instId) : 0L;

        long expired = instId != null
                ? windowRepository.countExpiredByInstitution(instId) : 0L;

        return new DashboardSummaryResponse(papersThisMonth, novelMolecules, closingSoon, expired);
    }
}
