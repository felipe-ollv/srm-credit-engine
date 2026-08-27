import { describe, expect, it } from 'vitest';
import { RatePercentPipe } from './pricing-display.pipe';

describe('RatePercentPipe', () => {
  const pipe = new RatePercentPipe();

  it('moves the decimal point without floating-point arithmetic', () => {
    expect(pipe.transform('0.01')).toBe('1,00%');
    expect(pipe.transform('0.015')).toBe('1,50%');
    expect(pipe.transform('0.1')).toBe('10,00%');
    expect(pipe.transform('1')).toBe('100,00%');
  });
});
