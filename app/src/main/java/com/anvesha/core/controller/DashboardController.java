package com.anvesha.core.controller;

import com.anvesha.core.dto.dashboard.DashboardSummaryResponse;
import com.anvesha.core.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "TTO aggregate statistics")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get aggregate TTO dashboard counts for the caller's institution")
    public DashboardSummaryResponse summary(@AuthenticationPrincipal UserDetails principal) {
        return dashboardService.getSummary(principal);
    }
}
