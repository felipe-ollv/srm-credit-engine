import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export interface DateBounds {
  readonly minimum: string;
  readonly maximum: string;
}

export function pricingDateBounds(now: Date = new Date()): DateBounds {
  const current = saoPauloDate(now);
  return {
    minimum: addCalendarDays(current, 1),
    maximum: addCalendarMonths(current, 360),
  };
}

export function dueDateBoundsValidator(bounds: DateBounds): ValidatorFn {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    const value = control.value;
    if (value === '') {
      return null;
    }
    if (value < bounds.minimum) {
      return { dueDatePast: true };
    }
    if (value > bounds.maximum) {
      return { dueDateTooFar: true };
    }
    return null;
  };
}

export function addCalendarMonths(isoDate: string, months: number): string {
  const { year, month, day } = parseIsoDate(isoDate);
  const zeroBasedTarget = month - 1 + months;
  const targetYear = year + Math.floor(zeroBasedTarget / 12);
  const targetMonth = modulo(zeroBasedTarget, 12) + 1;
  const targetDay = Math.min(day, daysInMonth(targetYear, targetMonth));
  return isoDateOf(targetYear, targetMonth, targetDay);
}

export function addCalendarDays(isoDate: string, days: number): string {
  const { year, month, day } = parseIsoDate(isoDate);
  const value = new Date(Date.UTC(year, month - 1, day + days));
  return isoDateOf(value.getUTCFullYear(), value.getUTCMonth() + 1, value.getUTCDate());
}

function saoPauloDate(now: Date): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Sao_Paulo',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(now);
  const values = Object.fromEntries(parts.map((part) => [part.type, part.value]));
  return `${values['year']}-${values['month']}-${values['day']}`;
}

function parseIsoDate(value: string): { year: number; month: number; day: number } {
  const [year, month, day] = value.split('-').map((part) => Number.parseInt(part, 10));
  return { year, month, day };
}

function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

function isoDateOf(year: number, month: number, day: number): string {
  return `${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
}

function modulo(value: number, divisor: number): number {
  return ((value % divisor) + divisor) % divisor;
}
