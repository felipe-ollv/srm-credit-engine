import { PageResponse } from '../../core/http/api.models';

export interface CreateAssignorRequest {
  readonly document: string;
  readonly legalName: string;
}

export interface AssignorResponse {
  readonly id: string;
  readonly document: string;
  readonly legalName: string;
  readonly createdAt: string;
}

export type AssignorPage = PageResponse<AssignorResponse>;
