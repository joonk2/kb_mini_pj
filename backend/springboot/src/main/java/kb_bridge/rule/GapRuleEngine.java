package kb_bridge.rule;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import kb_bridge.domain.company.entity.Company;
import kb_bridge.domain.company.entity.DisclosureEvidence;
import kb_bridge.domain.company.entity.FinancialSnapshot;

@Component
public class GapRuleEngine {

    public enum GapType {
        INVESTMENT_PLAN_GAP,
        FUNDING_PLAN_GAP,
        FX_BUSINESS_GAP
    }

    public record Finding(GapType type, String existingInfo, String latestInfo, List<DisclosureEvidence> evidence) {
    }

    public List<Finding> detect(
            Company company,
            FinancialSnapshot financials,
            List<DisclosureEvidence> disclosures
    ) {
        List<Finding> findings = new ArrayList<>();

        List<DisclosureEvidence> investmentEvidence = matching(disclosures,
                "시설투자", "신규시설", "유형자산취득", "공장신설", "설비투자", "시설증설");
        if (isNoPlan(company.investmentPlan()) && !investmentEvidence.isEmpty()) {
            findings.add(new Finding(
                    GapType.INVESTMENT_PLAN_GAP,
                    company.investmentPlan(),
                    "시설투자 관련 공시 확인",
                    investmentEvidence
            ));
        }

        List<DisclosureEvidence> fundingEvidence = matching(disclosures,
                "회사채", "사채발행", "차입", "유상증자", "전환사채", "신주인수권부사채");
        boolean debtIncreased = hasMaterialDebtIncrease(financials);
        if (isNoPlan(company.fundingPlan()) && (!fundingEvidence.isEmpty() || debtIncreased)) {
            String latestInfo = !fundingEvidence.isEmpty()
                    ? "자금조달 관련 공시 확인"
                    : "단기차입금의 전년 대비 20% 이상 증가 확인";
            if (fundingEvidence.isEmpty() && debtIncreased) {
                fundingEvidence = List.of(new DisclosureEvidence(
                        "OpenDART 재무정보",
                        "단기차입금 전년 대비 20% 이상 증가",
                        financials.period(),
                        null
                ));
            }
            findings.add(new Finding(
                    GapType.FUNDING_PLAN_GAP,
                    company.fundingPlan(),
                    latestInfo,
                    fundingEvidence
            ));
        }

        List<DisclosureEvidence> foreignEvidence = matching(disclosures,
                "해외법인", "해외사업", "해외진출", "해외투자", "해외공장", "국외사업", "외국법인");
        if (isNoPlan(company.foreignBusinessPlan()) && !foreignEvidence.isEmpty()) {
            findings.add(new Finding(
                    GapType.FX_BUSINESS_GAP,
                    company.foreignBusinessPlan(),
                    "해외법인·해외사업 관련 공시 확인",
                    foreignEvidence
            ));
        }

        return List.copyOf(findings);
    }

    public boolean needsFinancialStatements(Company company) {
        return isNoPlan(company.fundingPlan());
    }

    public boolean needsDisclosures(Company company) {
        return isNoPlan(company.investmentPlan())
                || isNoPlan(company.fundingPlan())
                || isNoPlan(company.foreignBusinessPlan());
    }

    private List<DisclosureEvidence> matching(List<DisclosureEvidence> disclosures, String... keywords) {
        return disclosures.stream()
                .filter(disclosure -> {
                    String title = disclosure.title().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
                    if (title.contains("취소")
                            || title.contains("철회")
                            || title.contains("해지")
                            || title.contains("청산")
                            || title.contains("처분")) {
                        return false;
                    }
                    for (String keyword : keywords) {
                        if (title.contains(keyword)) {
                            return true;
                        }
                    }
                    return false;
                })
                .toList();
    }

    private boolean isNoPlan(String plan) {
        if (plan == null || plan.isBlank()) {
            return false;
        }
        String normalized = plan.replaceAll("\\s+", "");
        return normalized.equals("없음")
                || normalized.contains("계획없음")
                || normalized.contains("없다")
                || normalized.contains("미계획");
    }

    private boolean hasMaterialDebtIncrease(FinancialSnapshot financials) {
        if (financials == null
                || financials.shortTermDebt() == null
                || financials.priorPeriodShortTermDebt() == null
                || financials.priorPeriodShortTermDebt().signum() <= 0) {
            return false;
        }
        BigDecimal threshold = financials.priorPeriodShortTermDebt().multiply(new BigDecimal("1.20"));
        return financials.shortTermDebt().compareTo(threshold) >= 0;
    }
}
