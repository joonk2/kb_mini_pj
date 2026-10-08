package kb_bridge.domain.company.service;

import org.springframework.stereotype.Service;

import java.util.List;

import kb_bridge.agent.GapAgent;
import kb_bridge.external.dart.DartClient;
import kb_bridge.domain.company.entity.Company;
import kb_bridge.domain.company.entity.DisclosureEvidence;
import kb_bridge.domain.company.entity.FinancialSnapshot;
import kb_bridge.domain.company.entity.GapAnalysisResponse;
import kb_bridge.agent.GapAgent.ResearchPlan;
import kb_bridge.external.dart.dto.DartCompanyResponse;

@Service
public class CompanyService {

    private final DartClient dartClient;
    private final RmKnowledgeService rmKnowledgeService;
    private final GapAgent gapAgent;

    public CompanyService(
            DartClient dartClient,
            RmKnowledgeService rmKnowledgeService,
            GapAgent gapAgent
    ) {
        this.dartClient = dartClient;
        this.rmKnowledgeService = rmKnowledgeService;
        this.gapAgent = gapAgent;
    }

    public DartCompanyResponse getCompany(String corpCode) {
        return dartClient.getCompany(corpCode);
    }

    public List<Company> getRmCompanies() {
        return rmKnowledgeService.findAll();
    }

    public GapAnalysisResponse analyzeCompany(String companyId) {
        Company company = rmKnowledgeService.findById(companyId);
        String corpCode = dartClient.resolveCorpCode(company.companyName());
        DartCompanyResponse dartCompany = dartClient.getCompany(corpCode);
        if (dartCompany == null || !"000".equals(dartCompany.status())) {
            String status = dartCompany == null ? "empty response" : dartCompany.status();
            String message = dartCompany == null ? "" : dartCompany.message();
            throw new IllegalStateException("OpenDART company lookup failed (" + status + "): " + message);
        }

        ResearchPlan researchPlan = gapAgent.planResearch(company);
        FinancialSnapshot financials = researchPlan.financialStatements()
                ? dartClient.getRecentFinancials(corpCode)
                : null;
        List<DisclosureEvidence> disclosures = researchPlan.disclosures()
                ? dartClient.getRecentDisclosures(corpCode)
                : List.of();
        return gapAgent.analyze(company, corpCode, financials, disclosures);
    }
}