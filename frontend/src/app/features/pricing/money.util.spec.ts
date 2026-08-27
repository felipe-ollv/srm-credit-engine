import { describe, expect, it } from 'vitest';
import {
  formatBrlMoney,
  formatMoney,
  normalizeBrlMoney,
} from './money.util';

describe('money utilities', () => {
  it('normalizes localized BRL without converting through number', () => {
    expect(normalizeBrlMoney('100.000,00')).toBe('100000.00');
    expect(normalizeBrlMoney('1,5')).toBe('1.50');
    expect(normalizeBrlMoney('99999999999999999,99')).toBe('99999999999999999.99');
  });

  it('rejects zero, excessive scale, malformed grouping and excessive precision', () => {
    expect(normalizeBrlMoney('0,00')).toBeNull();
    expect(normalizeBrlMoney('1,001')).toBeNull();
    expect(normalizeBrlMoney('10.00,00')).toBeNull();
    expect(normalizeBrlMoney('100000000000000000,00')).toBeNull();
    expect(normalizeBrlMoney('1e3')).toBeNull();
  });

  it('formats input and API money with Brazilian separators', () => {
    expect(formatBrlMoney('100000,5')).toBe('100.000,50');
    expect(formatMoney('92859.94', 'BRL')).toBe('R$ 92.859,94');
    expect(formatMoney('17094.67', 'USD')).toBe('US$ 17.094,67');
  });
});
