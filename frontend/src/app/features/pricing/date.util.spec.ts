import { describe, expect, it } from 'vitest';
import {
  addCalendarMonths,
  pricingDateBounds,
} from './date.util';

describe('pricing date utilities', () => {
  it('derives bounds using the business timezone', () => {
    const bounds = pricingDateBounds(new Date('2026-08-26T02:00:00Z'));

    expect(bounds.minimum).toBe('2026-08-26');
    expect(bounds.maximum).toBe('2056-08-25');
  });

  it('uses calendar months and clamps to the target month end', () => {
    expect(addCalendarMonths('2026-01-31', 1)).toBe('2026-02-28');
    expect(addCalendarMonths('2024-01-31', 1)).toBe('2024-02-29');
    expect(addCalendarMonths('2026-08-26', 360)).toBe('2056-08-26');
  });
});
