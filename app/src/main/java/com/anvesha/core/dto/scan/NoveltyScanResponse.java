package com.anvesha.core.dto.scan;

import com.anvesha.core.entity.NoveltyScan;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record NoveltyScanResponse(
        Long id,
        BigDecimal noveltyScore,
        boolean isNovel,
        String closestMatchSource,
        String closestMatchId,
        BigDecimal tanimotoSimilarity,
        OffsetDateTime scannedAt
) {
    public static NoveltyScanResponse from(NoveltyScan s) {
        return new NoveltyScanResponse(
                s.getId(), s.getNoveltyScore(), s.isNovel(),
                s.getClosestMatchSource(), s.getClosestMatchId(),
                s.getTanimotoSimilarity(), s.getScannedAt()
        );
    }
}
