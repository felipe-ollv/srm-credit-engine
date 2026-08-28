import { SettlementBatchRequest } from './settlements.models';

export class IdempotencyAttempt {
  private fingerprint: string | null = null;
  private key: string | null = null;

  keyFor(request: SettlementBatchRequest): string {
    const fingerprint = JSON.stringify(request);
    if (this.fingerprint !== fingerprint || this.key === null) {
      this.fingerprint = fingerprint;
      this.key = crypto.randomUUID();
    }
    return this.key;
  }

  complete(): void {
    this.fingerprint = null;
    this.key = null;
  }
}
