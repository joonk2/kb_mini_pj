package kb_bridge.domain.company.entity;

import java.util.List;

public record GapAnalysisResponse(
        Company company,
        String corpCode,
        FinancialSnapshot financials,
        List<GapResult> gaps,
        String message
) {
}
