import { useEffect, useState } from 'react';
import { getCompanies, getCompanyAnalysis } from './api';
import './App.css';

function App() {
  const [companies, setCompanies] = useState([]);
  const [selectedId, setSelectedId] = useState('');
  const [analysis, setAnalysis] = useState(null);
  const [loadingCompanies, setLoadingCompanies] = useState(true);
  const [loadingAnalysis, setLoadingAnalysis] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    getCompanies()
      .then((items) => {
        setCompanies(items);
        if (items.length > 0) {
          setSelectedId(items[0].companyId);
        }
      })
      .catch((loadError) => setError(loadError.message))
      .finally(() => setLoadingCompanies(false));
  }, []);

  async function analyzeSelectedCompany(event) {
    event.preventDefault();
    if (!selectedId) return;

    setLoadingAnalysis(true);
    setError('');
    setAnalysis(null);
    try {
      setAnalysis(await getCompanyAnalysis(selectedId));
    } catch (analysisError) {
      setError(analysisError.message);
    } finally {
      setLoadingAnalysis(false);
    }
  }

  const selectedCompany = companies.find((company) => company.companyId === selectedId);

  return (
    <main className="page-shell">
      <header className="page-header">
        <div className="brand-mark" aria-hidden="true">CG</div>
        <div>
          <p className="eyebrow">CORPORATE CLIENT INTELLIGENCE</p>
          <h1>기업 고객정보 Gap Agent</h1>
          <p className="subtitle">RM 사전정보와 최신 OpenDART 공개정보를 비교합니다.</p>
        </div>
      </header>

      <section className="selection-panel" aria-labelledby="company-selection-title">
        <div>
          <p className="eyebrow">CLIENT REVIEW</p>
          <h2 id="company-selection-title">분석할 기업을 선택하세요</h2>
        </div>
        <form onSubmit={analyzeSelectedCompany} className="company-form">
          <label className="visually-hidden" htmlFor="company-select">기업 선택</label>
          <select
            id="company-select"
            value={selectedId}
            onChange={(event) => {
              setSelectedId(event.target.value);
              setAnalysis(null);
              setError('');
            }}
            disabled={loadingCompanies || companies.length === 0}
          >
            {companies.map((company) => (
              <option key={company.companyId} value={company.companyId}>
                {company.companyName}
              </option>
            ))}
          </select>
          <button type="submit" disabled={!selectedId || loadingAnalysis}>
            {loadingAnalysis ? '최신정보 조회 중...' : '최신정보 분석'}
          </button>
        </form>
      </section>

      {error && <p className="error-message" role="alert">{error}</p>}
      {loadingCompanies && <p className="empty-state">RM 기업정보를 불러오는 중입니다...</p>}

      {selectedCompany && (
        <section className="rm-panel" aria-labelledby="rm-info-title">
          <div className="section-heading">
            <div>
              <p className="eyebrow">EXISTING RM KNOWLEDGE</p>
              <h2 id="rm-info-title">{selectedCompany.companyName} <span>RM 사전정보</span></h2>
            </div>
            <p className="consultation-date">
              최근 상담일 <strong>{selectedCompany.consultationDate}</strong>
            </p>
          </div>
          <dl className="rm-grid">
            <div><dt>투자계획</dt><dd>{selectedCompany.investmentPlan || '확인할 수 없음'}</dd></div>
            <div><dt>자금조달계획</dt><dd>{selectedCompany.fundingPlan || '확인할 수 없음'}</dd></div>
            <div><dt>해외사업계획</dt><dd>{selectedCompany.foreignBusinessPlan || '확인할 수 없음'}</dd></div>
            <div className="memo"><dt>RM 메모</dt><dd>{selectedCompany.rmMemo || '확인할 수 없음'}</dd></div>
          </dl>
        </section>
      )}

      {analysis && (
        <section className="analysis-section" aria-labelledby="analysis-title">
          <div className="analysis-heading">
            <div>
              <p className="eyebrow">LATEST PUBLIC INFORMATION</p>
              <h2 id="analysis-title">분석 결과</h2>
              {analysis.corpCode && (
                <p className="corp-code">OpenDART 기업코드 {analysis.corpCode}</p>
              )}
            </div>
            <span className={analysis.gaps.length > 0 ? 'status-badge warning' : 'status-badge'}>
              {analysis.gaps.length > 0 ? '고객정보 업데이트 필요' : '확인된 Gap 없음'}
            </span>
          </div>
          <p className="analysis-message">{analysis.message}</p>

          {analysis.financials ? (
            <div className="financial-panel">
              <h3>최근 재무정보 <span>{analysis.financials.period} 사업연도</span></h3>
              <dl className="financial-grid">
                <div><dt>매출액</dt><dd>{formatAmount(analysis.financials.revenue)}</dd></div>
                <div><dt>영업이익</dt><dd>{formatAmount(analysis.financials.operatingProfit)}</dd></div>
                <div><dt>단기차입금</dt><dd>{formatAmount(analysis.financials.shortTermDebt)}</dd></div>
                <div><dt>전기 단기차입금</dt><dd>{formatAmount(analysis.financials.priorPeriodShortTermDebt)}</dd></div>
                <div><dt>영업활동 현금흐름</dt><dd>{formatAmount(analysis.financials.operatingCashFlow)}</dd></div>
              </dl>
            </div>
          ) : (
            <p className="data-notice">최근 재무정보를 확인할 수 없습니다.</p>
          )}

          {analysis.gaps.length > 0 ? analysis.gaps.map((gap) => (
            <article className="gap-card" key={gap.gapType}>
              <div className="gap-title-row">
                <span className="gap-icon" aria-hidden="true">!</span>
                <h3>{gap.gapType}</h3>
              </div>
              <div className="comparison-grid">
                <div><h4>기존 RM 정보</h4><p>{gap.existingInfo || '확인할 수 없음'}</p></div>
                <div><h4>최신 정보</h4><p>{gap.latestInfo}</p></div>
              </div>
              <div className="reason-panel">
                <h4>Gap 발생 이유 <span className="generation-source">{gap.explanationSource}</span></h4>
                <p>{gap.reason}</p>
              </div>
              <div className="questions-panel">
                <h4>상담 시 확인할 질문 <span className="generation-source">{gap.explanationSource}</span></h4>
                <ol>{gap.questions.map((question) => <li key={question}>{question}</li>)}</ol>
              </div>
              <div className="evidence-panel">
                <h4>근거</h4>
                {gap.evidence.length > 0 ? (
                  <ul>
                    {gap.evidence.map((evidence) => (
                      <li key={`${evidence.date}-${evidence.title}`}>
                        <span>{evidence.source} · {evidence.date || '날짜 확인 필요'}</span>
                        {evidence.url
                          ? <a href={evidence.url} target="_blank" rel="noreferrer">{evidence.title}</a>
                          : <span>{evidence.title}</span>}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p>OpenDART 재무정보에서 단기차입금 증가를 확인했습니다.</p>
                )}
              </div>
            </article>
          )) : (
            <p className="empty-state no-gap">규칙을 충족하는 업데이트 항목이 발견되지 않았습니다.</p>
          )}
        </section>
      )}
    </main>
  );
}

function formatAmount(amount) {
  if (amount === null || amount === undefined) return '확인할 수 없음';
  return `${new Intl.NumberFormat('ko-KR').format(amount)} 원`;
}

export default App;
