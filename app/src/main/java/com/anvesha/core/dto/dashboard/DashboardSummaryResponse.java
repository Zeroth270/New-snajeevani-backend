package com.anvesha.core.dto.dashboard;

public record DashboardSummaryResponse(
        long papersProcessedThisMonth,
        long novelMoleculesFoundThisMonth,
        long windowsClosingSoon,
        long windowsExpiredUnfiled
) {}
