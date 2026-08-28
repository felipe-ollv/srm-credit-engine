import { Currency, MoneyResponse, PageResponse, ReceivableType } from '../../core/http/api.models';

export type ReceivableStatus = 'AVAILABLE' | 'SETTLED';

export interface ReceivableResponse {
  readonly id: string;
  readonly assignorId: string;
  readonly type: ReceivableType;
  readonly faceValue: MoneyResponse;
  readonly dueDate: string;
  readonly registrationDate: string;
  readonly status: ReceivableStatus;
  readonly createdAt: string;
}

export interface CreateReceivableRequest {
  readonly assignorId: string;
  readonly type: ReceivableType;
  readonly faceValue: string;
  readonly dueDate: string;
}

export interface AssignorOption {
  readonly id: string;
  readonly document: string;
  readonly legalName: string;
}

export type ReceivablePage = PageResponse<ReceivableResponse>;
export type { Currency };
