import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function normalizeCnpj(value: string): string {
  return value.replace(/\D/g, '');
}

export function formatCnpj(value: string): string {
  const digits = normalizeCnpj(value).slice(0, 14);
  return digits.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5');
}

export function isValidCnpj(value: string): boolean {
  const digits = normalizeCnpj(value);
  if (digits.length !== 14 || /^(\d)\1{13}$/.test(digits)) {
    return false;
  }
  return checkDigit(digits.slice(0, 12), [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]) === digits[12]
    && checkDigit(digits.slice(0, 13), [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2]) === digits[13];
}

export function cnpjValidator(): ValidatorFn {
  return (control: AbstractControl<string>): ValidationErrors | null => {
    if (control.value.trim() === '') {
      return null;
    }
    return isValidCnpj(control.value) ? null : { invalidCnpj: true };
  };
}

function checkDigit(digits: string, weights: readonly number[]): string {
  const total = [...digits].reduce((sum, digit, index) => sum + Number(digit) * weights[index], 0);
  const remainder = total % 11;
  return String(remainder < 2 ? 0 : 11 - remainder);
}
