package com.anvesha.core.scheduler;

import com.anvesha.core.entity.*;
import com.anvesha.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily scheduler that scans all OPEN/CLOSING_SOON DisclosureWindows,
 * updates their status, and fires Alert rows at the 30-day, 7-day,
 * and expiry thresholds for the institution's TTO officer(s).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadlineSchedulerService {

    private final DisclosureWindowRepository windowRepository;
    private final AlertRepository alertRepository;
    private final AppUserRepository userRepository;

    @Scheduled(cron = "${scheduler.deadline-check.cron:0 0 2 * * *}")
    @Transactional
    public void runDeadlineCheck() {
        LocalDate today = LocalDate.now();
        List<DisclosureWindow> activeWindows = windowRepository.findAllActiveWindows();

        log.info("Deadline check started — {} active windows", activeWindows.size());

        for (DisclosureWindow window : activeWindows) {
            long daysRemaining = today.until(window.getDeadlineDate(),
                    java.time.temporal.ChronoUnit.DAYS);

            if (daysRemaining < 0) {
                // Deadline passed
                if (!"EXPIRED".equals(window.getStatus())) {
                    window.setStatus("EXPIRED");
                    windowRepository.save(window);
                    fireAlert(window, "EXPIRED",
                            buildMessage(window, "EXPIRED", daysRemaining));
                }
            } else if (daysRemaining <= 7) {
                // 7-day warning
                if (!"CLOSING_SOON".equals(window.getStatus())) {
                    window.setStatus("CLOSING_SOON");
                    windowRepository.save(window);
                }
                fireAlertIfNotSent(window, "DEADLINE_7D",
                        buildMessage(window, "DEADLINE_7D", daysRemaining));
            } else if (daysRemaining <= 30) {
                // 30-day warning
                if (!"CLOSING_SOON".equals(window.getStatus())) {
                    window.setStatus("CLOSING_SOON");
                    windowRepository.save(window);
                }
                fireAlertIfNotSent(window, "DEADLINE_30D",
                        buildMessage(window, "DEADLINE_30D", daysRemaining));
            }
        }

        log.info("Deadline check complete");
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    private void fireAlert(DisclosureWindow window, String alertType, String message) {
        List<AppUser> officers = getTtoOfficers(window);
        for (AppUser officer : officers) {
            Alert alert = Alert.builder()
                    .recipientUser(officer)
                    .disclosureWindow(window)
                    .alertType(alertType)
                    .message(message)
                    .isRead(false)
                    .build();
            alertRepository.save(alert);
            log.info("Alert {} created for user {} (window {})",
                    alertType, officer.getEmail(), window.getId());
        }
    }

    /** Only fires if no alert of this type has been sent for this window yet */
    private void fireAlertIfNotSent(DisclosureWindow window, String alertType, String message) {
        if (!windowRepository.alertAlreadySent(window.getId(), alertType)) {
            fireAlert(window, alertType, message);
        }
    }

    private List<AppUser> getTtoOfficers(DisclosureWindow window) {
        if (window.getPaper() == null || window.getPaper().getInstitution() == null) {
            return List.of();
        }
        return userRepository.findTtoOfficersByInstitution(
                window.getPaper().getInstitution().getId());
    }

    private String buildMessage(DisclosureWindow window, String type, long daysRemaining) {
        String paperTitle = window.getPaper() != null ? window.getPaper().getTitle() : "Unknown";
        return switch (type) {
            case "DEADLINE_30D" -> String.format(
                    "⚠️ Patent filing deadline approaching: Paper '%s' disclosed on %s. " +
                    "Deadline: %s (%d days remaining). File a provisional application soon.",
                    paperTitle, window.getDisclosureDate(), window.getDeadlineDate(), daysRemaining);
            case "DEADLINE_7D" -> String.format(
                    "🚨 URGENT — Only %d days left to file! Paper '%s' grace period expires on %s. " +
                    "Immediate action required.",
                    daysRemaining, paperTitle, window.getDeadlineDate());
            case "EXPIRED" -> String.format(
                    "❌ Grace period EXPIRED for paper '%s'. Disclosure date: %s, Deadline was: %s. " +
                    "Patent rights may have been forfeited under Indian Patents Act.",
                    paperTitle, window.getDisclosureDate(), window.getDeadlineDate());
            default -> "Disclosure window status update for paper: " + paperTitle;
        };
    }
}
