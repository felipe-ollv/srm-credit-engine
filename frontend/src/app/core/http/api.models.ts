export type Currency = 'BRL' | 'USD';
export type ReceivableType = 'DUPLICATA_MERCANTIL' | 'CHEQUE_PRE_DATADO';

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

export interface ApiProblem {
  readonly status: number;
  readonly title: string;
  readonly detail: string;
  readonly instance: string;
  readonly code: string;
  readonly correlationId: string;
  readonly fieldErrors?: Readonly<Record<string, string>>;
}

export interface ApiFailure {
  readonly code: string;
  readonly message: string;
  readonly correlationId: string | null;
  readonly fieldErrors: Readonly<Record<string, string>>;
}

export interface PageResponse<T> {
  readonly content: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
