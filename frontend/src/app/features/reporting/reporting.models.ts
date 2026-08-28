import { Currency, ExchangeRateResponse, MoneyResponse, PageResponse } from '../../core/http/api.models';

export interface SettlementStatement {
  readonly settlementId: string;
  readonly batchId: string;
  readonly receivableId: string;
  readonly assignorId: string;
  readonly assignorDocument: string;
  readonly assignorLegalName: string;
  readonly receivableType: string;
  readonly dueDate: string;
  readonly faceValue: MoneyResponse;
  readonly presentValue: MoneyResponse;
  readonly discount: MoneyResponse;
  readonly payment: MoneyResponse;
  readonly termMonths: number;
  readonly baseRate: string;
  readonly spread: string;
  readonly exchangeRate: ExchangeRateResponse | null;
  readonly pricingDate: string;
  readonly calculatedAt: string;
  readonly settledAt: string;
}

export interface ReportingFilters {
  readonly from: string;
  readonly to: string;
  readonly assignorId: string;
  readonly paymentCurrency: Currency | '';
  readonly sort: 'settledAt,desc' | 'settledAt,asc' | 'assignorLegalName,asc' | 'paymentAmount,desc';
}

export type SettlementPage = PageResponse<SettlementStatement>;
