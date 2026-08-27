import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const BRL_INPUT = /^(?:0|[1-9]\d*|[1-9]\d{0,2}(?:\.\d{3})+)(?:,\d{1,2})?$/;

export function normalizeBrlMoney(input: string): string | null {
  const trimmed = input.trim();
  if (!BRL_INPUT.test(trimmed)) {
    return null;
  }

  const [integerPart, fractionPart = ''] = trimmed.split(',');
  const integerDigits = integerPart.replaceAll('.', '');
  if (integerDigits.length > 17) {
    return null;
  }
  const fraction = fractionPart.padEnd(2, '0');
  if (/^0+$/.test(integerDigits) && /^0+$/.test(fraction)) {
    return null;
  }
  return `${integerDigits}.${fraction}`;
}

export function formatBrlMoney(input: string): string | null {
  const normalized = normalizeBrlMoney(input);
  if (normalized === null) {
    return null;
  }
  const [integerPart, fractionPart] = normalized.split('.');
  return `${groupThousands(integerPart)},${fractionPart}`;
}

export function formatMoney(amount: string, currency: 'BRL' | 'USD'): string {
  const negative = amount.startsWith('-');
  const unsigned = negative ? amount.slice(1) : amount;
  const [integerPart, fractionPart = '00'] = unsigned.split('.');
  const symbol = currency === 'BRL' ? 'R$' : 'US$';
  return `${negative ? '-' : ''}${symbol} ${groupThousands(integerPart)},${fractionPart.padEnd(2, '0')}`;
}

export function brlMoneyValidator(): ValidatorFn {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    if (control.value.trim() === '') {
      return null;
    }
    return normalizeBrlMoney(control.value) === null ? { invalidMoney: true } : null;
  };
}

function groupThousands(integerPart: string): string {
  return integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}
