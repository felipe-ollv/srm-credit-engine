import {
  Currency,
  ExchangeRateResponse,
  MoneyResponse,
} from '../../core/http/api.models';
import { ReceivableResponse } from '../receivables/receivables.models';

export type SettlementItemStatus =
  | 'SUCCESS'
  | 'NOT_FOUND'
  | 'CONFLICT'
  | 'RULE_VIOLATION'
  | 'FX_RATE_UNAVAILABLE'
  | 'TECHNICAL_ERROR';

export interface SettlementBatchRequest {
  readonly items: readonly {
    readonly receivableId: string;
    readonly paymentCurrency: Currency;
  }[];
}

export interface SettlementSnapshot {
  readonly settlementId: string;
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

export interface SettlementBatchItemResponse {
  readonly receivableId: string;
  readonly paymentCurrency: Currency;
  readonly status: SettlementItemStatus;
  readonly code: string | null;
  readonly detail: string | null;
  readonly settlement: SettlementSnapshot | null;
}

export interface SettlementBatchResponse {
  readonly batchId: string;
  readonly status: string;
  readonly requestedAt: string;
  readonly completedAt: string;
  readonly items: readonly SettlementBatchItemResponse[];
}

export interface SettlementSelection {
  readonly receivable: ReceivableResponse;
  readonly paymentCurrency: Currency;
}
