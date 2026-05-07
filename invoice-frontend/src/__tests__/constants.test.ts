import { CURRENCIES, MAX_LINES, MAX_AMOUNT, emptyLine } from '../constants/invoice';

describe('CURRENCIES', () => {
  test('contains NZD', () => expect(CURRENCIES).toContain('NZD'));
  test('contains USD', () => expect(CURRENCIES).toContain('USD'));
  test('contains EUR', () => expect(CURRENCIES).toContain('EUR'));
  test('has no duplicates', () => expect(new Set(CURRENCIES).size).toBe(CURRENCIES.length));
  test('all entries are 3-letter uppercase strings', () => {
    CURRENCIES.forEach(c => expect(c).toMatch(/^[A-Z]{3}$/));
  });
});

describe('MAX_LINES', () => {
  test('is a positive integer', () => {
    expect(Number.isInteger(MAX_LINES)).toBe(true);
    expect(MAX_LINES).toBeGreaterThan(0);
  });
});

describe('MAX_AMOUNT', () => {
  test('is 1 billion', () => {
    expect(MAX_AMOUNT).toBe(1_000_000_000);
  });

  test('is a positive number', () => {
    expect(MAX_AMOUNT).toBeGreaterThan(0);
  });

  test('is a finite number', () => {
    expect(Number.isFinite(MAX_AMOUNT)).toBe(true);
  });
});

describe('emptyLine', () => {
  test('returns a line with empty amount and description', () => {
    const line = emptyLine();
    expect(line.amount).toBe('');
    expect(line.description).toBe('');
  });

  test('defaults currency to USD', () => {
    expect(emptyLine().currency).toBe('USD');
  });

  test('each call returns a unique id', () => {
    const a = emptyLine();
    const b = emptyLine();
    expect(a.id).not.toBe(b.id);
  });
});
