export type ReceivableType = 'DUPLICATA_MERCANTIL' | 'CHEQUE_PRE_DATADO';
export type Currency = 'BRL' | 'USD';

export interface PricingSimulationRequest {
  readonly receivableType: ReceivableType;
  readonly faceValue: string;
  readonly dueDate: string;
  readonly paymentCurrency: Currency;
}

export interface MoneyResponse {
  readonly amount: string;
  readonly currency: Currency;
}

export interface ExchangeRateResponse {
  readonly baseCurrency: Currency;
  readonly quoteCurrency: Currency;
  readonly rate: string;
  readonly effectiveAt: string;
  readonly capturedAt: string;
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

export interface ApiProblem {
  readonly status: number;
  readonly title: string;
  readonly detail: string;
  readonly instance: string;
  readonly code: string;
  readonly correlationId: string;
  readonly fieldErrors?: Readonly<Record<string, string>>;
}

export interface PricingFailure {
  readonly code: string;
  readonly message: string;
  readonly correlationId: string | null;
  readonly fieldErrors: Readonly<Record<string, string>>;
}
