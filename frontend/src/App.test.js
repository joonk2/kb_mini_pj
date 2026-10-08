import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import App from './App';
import { getCompanies, getCompanyAnalysis } from './api';

const companies = [{
  companyId: '1',
  companyName: '한빛전자',
  consultationDate: '2026-01-08',
  investmentPlan: '없음',
  fundingPlan: '없음',
  foreignBusinessPlan: '없음',
  rmMemo: '현재 신규 설비투자 계획 없음',
}];

beforeEach(() => {
  global.fetch = jest.fn();
});

test('loads RM companies and shows analyzed gap evidence and questions', async () => {
  global.fetch
    .mockResolvedValueOnce({ ok: true, json: async () => companies })
    .mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        company: companies[0],
        corpCode: '00123456',
        gaps: [{
          gapType: 'INVESTMENT_PLAN_GAP',
          existingInfo: '없음',
          latestInfo: '시설투자 관련 공시 확인',
          reason: '기존 정보와 최신 정보가 일치하지 않습니다.',
          explanationSource: 'Gemini',
          evidence: [{
            source: 'OpenDART',
            title: '신규시설투자 결정',
            date: '2026-06-01',
            url: 'https://dart.fss.or.kr/example',
          }],
          questions: ['투자 집행 일정은 어떻게 됩니까?'],
        }],
        financials: {
          period: '2025',
          revenue: 1000000,
          operatingProfit: 100000,
          shortTermDebt: 50000,
          priorPeriodShortTermDebt: 40000,
          operatingCashFlow: 20000,
        },
        message: '1개의 고객정보 업데이트 항목을 확인했습니다.',
      }),
    });

  render(<App />);
  expect((await screen.findAllByText('한빛전자')).length).toBeGreaterThan(0);
  fireEvent.click(screen.getByRole('button', { name: '최신정보 분석' }));

  expect(await screen.findByText('INVESTMENT_PLAN_GAP')).toBeInTheDocument();
  expect(screen.getByText('OpenDART 기업코드 00123456')).toBeInTheDocument();
  expect(screen.getByText('전기 단기차입금')).toBeInTheDocument();
  expect(screen.getByText('40,000 원')).toBeInTheDocument();
  expect(screen.getAllByText('Gemini')).toHaveLength(2);
  expect(screen.getByText('신규시설투자 결정')).toBeInTheDocument();
  expect(screen.getByText('투자 집행 일정은 어떻게 됩니까?')).toBeInTheDocument();
  await waitFor(() => expect(global.fetch).toHaveBeenCalledTimes(2));
  expect(global.fetch).toHaveBeenNthCalledWith(1, '/api/companies');
  expect(global.fetch).toHaveBeenNthCalledWith(2, '/api/companies/1/analysis');
});

test('shows an explicit message when RM companies cannot be loaded', async () => {
  global.fetch.mockResolvedValueOnce({
    ok: false,
    status: 502,
    json: async () => ({ message: 'RM 사전정보 CSV를 읽을 수 없습니다.' }),
  });
  render(<App />);
  expect(await screen.findByRole('alert')).toHaveTextContent('RM 사전정보 CSV를 읽을 수 없습니다.');
});

test('requests the backend analysis route and reports backend API errors', async () => {
  global.fetch.mockResolvedValueOnce({
    ok: false,
    status: 502,
    json: async () => ({ message: 'GEMINI_API_KEY must be configured.' }),
  });

  await expect(getCompanyAnalysis('company/1')).rejects.toThrow(
    'GEMINI_API_KEY must be configured.'
  );
  expect(global.fetch).toHaveBeenCalledWith(
    '/api/companies/company%2F1/analysis'
  );
});

test('rejects a company-list payload that does not match the backend DTO', async () => {
  global.fetch.mockResolvedValueOnce({
    ok: true,
    json: async () => ({ companies: [] }),
  });

  await expect(getCompanies()).rejects.toThrow(
    '기업 목록 응답 형식이 백엔드 API 계약과 다릅니다.'
  );
});
