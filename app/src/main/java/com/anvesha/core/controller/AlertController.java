package com.anvesha.core.controller;

import com.anvesha.core.dto.alert.AlertResponse;
import com.anvesha.core.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alerts", description = "User notifications")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Get alerts for the current user. Use ?unread=true to filter.")
    public Page<AlertResponse> list(
            @RequestParam(defaultValue = "false") boolean unread,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails principal) {
        return alertService.listForCurrentUser(principal, unread, page, size);
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark an alert as read")
    public AlertResponse markRead(@PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails principal) {
        return alertService.markRead(id, principal);
    }
}
