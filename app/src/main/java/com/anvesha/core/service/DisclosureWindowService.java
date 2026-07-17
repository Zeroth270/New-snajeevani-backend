package com.anvesha.core.service;

import com.anvesha.core.dto.window.CreateDisclosureWindowRequest;
import com.anvesha.core.dto.window.DisclosureWindowResponse;
import com.anvesha.core.entity.DisclosureWindow;
import com.anvesha.core.entity.Paper;
import com.anvesha.core.exception.ResourceNotFoundException;
import com.anvesha.core.repository.DisclosureWindowRepository;
import com.anvesha.core.repository.PaperRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisclosureWindowService {

    private final DisclosureWindowRepository windowRepository;
    private final PaperRepository paperRepository;

    /**
     * Configurable flag — whether to automatically mark windows as qualifying
     * for Section 31. Real eligibility requires legal review; this is a
     * convenience default surfaced to the TTO for their records.
     */
    @Value("${anvesha.section31.qualifies-by-default:false}")
    private boolean section31QualifiesByDefault;

    @Transactional
    public DisclosureWindowResponse createForMolecule(Long paperId, CreateDisclosureWindowRequest req) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper", paperId));

        int graceDays = (req.gracePeriodDays() != null) ? req.gracePeriodDays() : 365;

        DisclosureWindow window = DisclosureWindow.builder()
                .paper(paper)
                .disclosureDate(req.disclosureDate())
                .gracePeriodDays(graceDays)
                .deadlineDate(req.disclosureDate().plusDays(graceDays))
                .qualifiesForSection31(section31QualifiesByDefault)
                .status("OPEN")
                .build();

        window = windowRepository.save(window);
        log.info("DisclosureWindow {} created for paper {} — deadline {}",
                window.getId(), paperId, window.getDeadlineDate());

        return DisclosureWindowResponse.from(window);
    }

    @Transactional(readOnly = true)
    public Page<DisclosureWindowResponse> list(String status, Long institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("deadlineDate").ascending());
        Page<DisclosureWindow> results;

        if (institutionId != null && status != null) {
            results = windowRepository.findByPaperInstitutionIdAndStatus(institutionId, status, pageable);
        } else if (status != null) {
            results = windowRepository.findByStatus(status, pageable);
        } else {
            results = windowRepository.findAll(pageable);
        }

        return results.map(DisclosureWindowResponse::from);
    }
}
