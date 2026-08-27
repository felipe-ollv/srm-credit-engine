import { Pipe, PipeTransform } from '@angular/core';
import { Currency, MoneyResponse } from './pricing.models';
import { formatMoney } from './money.util';

@Pipe({ name: 'moneyDisplay' })
export class MoneyDisplayPipe implements PipeTransform {
  transform(money: MoneyResponse): string {
    return formatMoney(money.amount, money.currency);
  }
}

@Pipe({ name: 'decimalDisplay' })
export class DecimalDisplayPipe implements PipeTransform {
  transform(value: string): string {
    return value.replace('.', ',');
  }
}

@Pipe({ name: 'ratePercent' })
export class RatePercentPipe implements PipeTransform {
  transform(value: string): string {
    const [integerPart, fractionPart = ''] = value.split('.');
    const shiftedFraction = fractionPart.padEnd(2, '0');
    const digits = `${integerPart}${shiftedFraction}`.replace(/^0+(?=\d)/, '');
    const scale = Math.max(shiftedFraction.length - 2, 0);
    const padded = digits.padStart(scale + 1, '0');
    const splitAt = padded.length - scale;
    const percentageInteger = padded.slice(0, splitAt);
    const percentageFraction = padded.slice(splitAt).padEnd(2, '0').slice(0, 2);
    return `${percentageInteger},${percentageFraction}%`;
  }
}

@Pipe({ name: 'dateTimePt' })
export class DateTimePtPipe implements PipeTransform {
  transform(value: string): string {
    return new Intl.DateTimeFormat('pt-BR', {
      timeZone: 'America/Sao_Paulo',
      dateStyle: 'short',
      timeStyle: 'medium',
    }).format(new Date(value));
  }
}

export function currencyLabel(currency: Currency): string {
  return currency === 'BRL' ? 'Real brasileiro' : 'Dólar americano';
}
