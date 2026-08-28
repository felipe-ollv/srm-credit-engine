import { describe, expect, it } from 'vitest';
import { formatCnpj, isValidCnpj, normalizeCnpj } from './cnpj.util';

describe('CNPJ utilities', () => {
  it('normalizes and formats a valid document', () => {
    expect(normalizeCnpj('11.222.333/0001-81')).toBe('11222333000181');
    expect(formatCnpj('11222333000181')).toBe('11.222.333/0001-81');
    expect(isValidCnpj('11.222.333/0001-81')).toBe(true);
  });

  it('rejects invalid check digits and repeated digits', () => {
    expect(isValidCnpj('11.222.333/0001-82')).toBe(false);
    expect(isValidCnpj('00.000.000/0000-00')).toBe(false);
  });
});
