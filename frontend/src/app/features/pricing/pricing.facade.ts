import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  catchError,
  defer,
  distinctUntilChanged,
  EMPTY,
  finalize,
  map,
  merge,
  Observable,
  of,
  Subject,
  switchMap,
  tap,
  timer,
} from 'rxjs';
import { PricingApi } from './pricing.api';
import {
  ApiProblem,
  PricingFailure,
  PricingSimulationRequest,
  PricingSimulationResponse,
} from './pricing.models';

@Injectable()
export class PricingFacade {
  private readonly api = inject(PricingApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly retryRequested = new Subject<void>();
  private readonly lastCommand = signal<PricingSimulationRequest | null>(null);
  private connected = false;

  readonly loading = signal(false);
  readonly result = signal<PricingSimulationResponse | null>(null);
  readonly failure = signal<PricingFailure | null>(null);

  connect(commands: Observable<PricingSimulationRequest | null>): void {
    if (this.connected) {
      throw new Error('PricingFacade can only be connected once');
    }
    this.connected = true;

    const debouncedCommands = commands.pipe(
      tap(() => {
        this.result.set(null);
        this.failure.set(null);
      }),
      switchMap((command) => command === null
        ? of(null)
        : timer(400).pipe(map(() => command))),
      distinctUntilChanged(commandsEqual),
    );
    const retries = this.retryRequested.pipe(map(() => this.lastCommand()));

    merge(debouncedCommands, retries).pipe(
      switchMap((command) => {
        if (command === null) {
          this.loading.set(false);
          return EMPTY;
        }
        this.lastCommand.set(command);
        return this.execute(command);
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  retry(): void {
    if (this.lastCommand() !== null) {
      this.retryRequested.next();
    }
  }

  private execute(command: PricingSimulationRequest): Observable<void> {
    return defer(() => {
      this.loading.set(true);
      this.failure.set(null);
      return this.api.simulate(command).pipe(
        tap((result) => this.result.set(result)),
        map(() => undefined),
        catchError((error: unknown) => {
          this.failure.set(toPricingFailure(error));
          return EMPTY;
        }),
        finalize(() => this.loading.set(false)),
      );
    });
  }
}

function commandsEqual(
  first: PricingSimulationRequest | null,
  second: PricingSimulationRequest | null,
): boolean {
  if (first === null || second === null) {
    return first === second;
  }
  return first.receivableType === second.receivableType
    && first.faceValue === second.faceValue
    && first.dueDate === second.dueDate
    && first.paymentCurrency === second.paymentCurrency;
}

export function toPricingFailure(error: unknown): PricingFailure {
  if (error instanceof HttpErrorResponse && isApiProblem(error.error)) {
    return {
      code: error.error.code,
      message: messageFor(error.error),
      correlationId: error.error.correlationId || null,
      fieldErrors: error.error.fieldErrors ?? {},
    };
  }
  return {
    code: 'NETWORK_ERROR',
    message: 'Não foi possível acessar o motor de crédito. Verifique os serviços e tente novamente.',
    correlationId: null,
    fieldErrors: {},
  };
}

function messageFor(problem: ApiProblem): string {
  switch (problem.code) {
    case 'FX_RATE_UNAVAILABLE':
      return 'A cotação USD/BRL está indisponível ou expirada. Tente novamente mais tarde.';
    case 'PRICING_RULE_VIOLATION':
      return problem.detail;
    case 'ACCESS_DENIED':
      return 'Seu perfil não possui permissão para realizar esta simulação.';
    case 'REQUEST_INVALID':
      return 'Revise os campos destacados e tente novamente.';
    default:
      return problem.detail || 'A simulação não pôde ser concluída.';
  }
}

function isApiProblem(value: unknown): value is ApiProblem {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Partial<ApiProblem>;
  return typeof candidate.code === 'string'
    && typeof candidate.detail === 'string'
    && typeof candidate.correlationId === 'string';
}
