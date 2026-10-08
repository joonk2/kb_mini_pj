package kb_bridge.domain.company.entity;

import java.time.LocalDate;

public record Company(
        String companyId,
        String companyName,
        LocalDate consultationDate,
        String investmentPlan,
        String fundingPlan,
        String foreignBusinessPlan,
        String rmMemo
) {
}
