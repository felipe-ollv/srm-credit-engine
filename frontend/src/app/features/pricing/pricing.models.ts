import {
  ApiProblem,
  Currency,
  ExchangeRateResponse,
  MoneyResponse,
  ReceivableType,
} from '../../core/http/api.models';

export type { ApiProblem, Currency, ExchangeRateResponse, MoneyResponse, ReceivableType };

export interface PricingSimulationRequest {
  readonly receivableType: ReceivableType;
  readonly faceValue: string;
  readonly dueDate: string;
  readonly paymentCurrency: Currency;
}

export interface PricingSimulationResponse {
  readonly receivableType: ReceivableType;
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
}

export interface PricingFailure {
  readonly code: string;
  readonly message: string;
  readonly correlationId: string | null;
  readonly fieldErrors: Readonly<Record<string, string>>;
}
