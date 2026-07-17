package com.anvesha.core.service;

import com.anvesha.core.client.PythonChemServiceClient;
import com.anvesha.core.client.RagServiceClient;
import com.anvesha.core.dto.paper.PaperDetailResponse;
import com.anvesha.core.dto.paper.PaperResponse;
import com.anvesha.core.dto.paper.PaperUploadRequest;
import com.anvesha.core.entity.*;
import com.anvesha.core.exception.ResourceNotFoundException;
import com.anvesha.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.anvesha.core.exception.BadRequestException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperService {

    private final PaperRepository paperRepository;
    private final AppUserRepository userRepository;
    private final MoleculeRepository moleculeRepository;
    private final NoveltyScanRepository noveltyScanRepository;
    private final PythonChemServiceClient chemClient;
    private final MoleculeService moleculeService;
    private final RagServiceClient ragServiceClient;

    // ─── Upload ─────────────────────────────────────────────────────────────

    @Transactional
    public PaperResponse upload(PaperUploadRequest req, UserDetails principal) {
        AppUser uploader = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        String text = req.rawText();
        String filePath = null;

        // If a file was provided, extract its text here.
        if (req.file() != null && !req.file().isEmpty()) {
            filePath = "uploads/" + System.currentTimeMillis() + "_" + req.file().getOriginalFilename();
            String originalFilename = req.file().getOriginalFilename();
            String lowerName = originalFilename != null ? originalFilename.toLowerCase() : "";

            if (lowerName.endsWith(".txt")) {
                log.info("Reading text from uploaded TXT file: {}", originalFilename);
                try {
                    String extractedText = new String(req.file().getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    if (extractedText != null && !extractedText.isBlank()) {
                        text = extractedText;
                    } else if (text == null || text.isBlank()) {
                        throw new BadRequestException("Uploaded text file is empty.");
                    }
                } catch (IOException e) {
                    log.error("Failed to read TXT file: {}", e.getMessage());
                    throw new BadRequestException("Failed to read text from file: " + e.getMessage());
                }
            } else if (lowerName.endsWith(".pdf")) {
                log.info("Extracting text from uploaded PDF: {}", originalFilename);
                try (PDDocument doc = Loader.loadPDF(req.file().getBytes())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String extractedText = stripper.getText(doc);
                    if (extractedText != null && !extractedText.isBlank()) {
                        text = extractedText;
                    } else if (text == null || text.isBlank()) {
                        throw new BadRequestException("PDF file is empty or contains no readable text.");
                    }
                } catch (IOException e) {
                    log.error("Failed to parse PDF file: {}", e.getMessage());
                    throw new BadRequestException("Failed to extract text from PDF: " + e.getMessage());
                }
            } else {
                throw new BadRequestException("Unsupported file type. Please upload a PDF or plain text (.txt) file.");
            }
        }

        Paper paper = Paper.builder()
                .title(req.title())
                .authors(req.authors())
                .sourceType(req.sourceType())
                .publicationDate(req.publicationDate())
                .rawText(text)
                .filePath(filePath)
                .status("PENDING")
                .institution(uploader.getInstitution())
                .uploadedBy(uploader)
                .build();

        paper = paperRepository.save(paper);
        log.info("Paper {} persisted with status PENDING", paper.getId());

        // Kick off async chemistry pipeline
        processPaperAsync(paper.getId(), text);

        return PaperResponse.from(paper);
    }

    // ─── Async chemistry pipeline ────────────────────────────────────────────

    @Async("paperProcessingExecutor")
    public void processPaperAsync(Long paperId, String text) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new ResourceNotFoundException("Paper", paperId));

        paper.setStatus("PROCESSING");
        paperRepository.save(paper);

        try {
            if (text == null || text.isBlank()) {
                log.warn("Paper {} has no text to process", paperId);
                paper.setStatus("FAILED");
                paperRepository.save(paper);
                return;
            }

            // Step 1: Extract molecules
            PythonChemServiceClient.ExtractMoleculesResponse extracted =
                    chemClient.extractMolecules(text);

            if (extracted == null || extracted.molecules().isEmpty()) {
                log.info("No molecules extracted from paper {}", paperId);
                paper.setStatus("PROCESSED");
                paperRepository.save(paper);
                return;
            }

            for (PythonChemServiceClient.MoleculeResult mr : extracted.molecules()) {
                // Step 2: Persist each molecule
                Molecule mol = Molecule.builder()
                        .paper(paper)
                        .extractedNameRaw(mr.extracted_name_raw())
                        .iupacName(mr.iupac_name())
                        .smiles(mr.smiles())
                        .extractionConfidence(mr.extraction_confidence() != null
                                ? BigDecimal.valueOf(mr.extraction_confidence()) : null)
                        .build();
                mol = moleculeRepository.save(mol);

                // Step 3: Novelty check (only if we have a SMILES)
                if (mr.smiles() != null && !mr.smiles().isBlank()) {
                    try {
                        PythonChemServiceClient.NoveltyCheckResponse nc =
                                chemClient.checkNovelty(mr.smiles());

                        NoveltyScan scan = NoveltyScan.builder()
                                .molecule(mol)
                                .noveltyScore(BigDecimal.valueOf(nc.novelty_score()))
                                .isNovel(nc.is_novel())
                                .closestMatchSource(nc.closest_match_source())
                                .closestMatchId(nc.closest_match_id())
                                .tanimotoSimilarity(nc.tanimoto_similarity() != null
                                        ? BigDecimal.valueOf(nc.tanimoto_similarity()) : null)
                                .build();
                        noveltyScanRepository.save(scan);

                        // Fire a NOVEL_MOLECULE_FOUND alert if genuinely novel
                        if (nc.is_novel()) {
                            moleculeService.createNovelMoleculeAlert(mol, paper);
                        }
                    } catch (Exception e) {
                        log.warn("Novelty check failed for molecule {}: {}", mol.getId(), e.getMessage());
                    }
                }
            }

            // Index the paper's text in the RAG service for future novelty checks
            ragServiceClient.indexPaper(text, paper.getTitle());

            paper.setStatus("PROCESSED");
            paperRepository.save(paper);
            log.info("Paper {} fully processed — {} molecules found",
                    paperId, extracted.molecules().size());

        } catch (Exception e) {
            log.error("Processing failed for paper {}: {}", paperId, e.getMessage(), e);
            paper.setStatus("FAILED");
            paperRepository.save(paper);
        }
    }

    // ─── Queries ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PaperResponse> list(UserDetails principal, String status, int page, int size) {
        AppUser caller = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String role = caller.getRole();
        Long instId = caller.getInstitution() != null ? caller.getInstitution().getId() : null;
        Long userId = caller.getId();

        Page<Paper> papers;

        if ("RESEARCHER".equals(role)) {
            papers = (status != null)
                    ? paperRepository.findByUploadedById(userId, pageable)
                    : paperRepository.findByUploadedById(userId, pageable);
        } else {
            // TTO_OFFICER, INSTITUTION_ADMIN, SUPER_ADMIN see all institution papers
            if (instId != null && status != null) {
                papers = paperRepository.findByInstitutionIdAndStatus(instId, status, pageable);
            } else if (instId != null) {
                papers = paperRepository.findByInstitutionId(instId, pageable);
            } else {
                papers = paperRepository.findAll(pageable);
            }
        }

        return papers.map(PaperResponse::from);
    }

    @Transactional(readOnly = true)
    public PaperDetailResponse getById(Long id, UserDetails principal) {
        AppUser caller = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        Paper paper = switch (caller.getRole()) {
            case "RESEARCHER" -> paperRepository.findByIdAndUploadedById(id, caller.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Paper", id));
            default -> {
                Long instId = caller.getInstitution() != null ? caller.getInstitution().getId() : null;
                yield instId != null
                        ? paperRepository.findByIdAndInstitutionId(id, instId)
                            .orElseThrow(() -> new ResourceNotFoundException("Paper", id))
                        : paperRepository.findById(id)
                            .orElseThrow(() -> new ResourceNotFoundException("Paper", id));
            }
        };

        List<com.anvesha.core.dto.molecule.MoleculeResponse> moleculeResponses =
                moleculeService.listForPaper(paper);

        return PaperDetailResponse.from(paper, moleculeResponses);
    }
}
