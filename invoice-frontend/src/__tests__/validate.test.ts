import dayjs from 'dayjs';
import { validate } from '../components/InvoiceForm';
import { InvoiceLine, } from '../types/invoice';
import { MAX_AMOUNT } from '../constants/invoice';

const line = (amount: string, currency = 'NZD'): InvoiceLine => ({
  id: 'test-id',
  description: 'Item',
  currency,
  amount,
});

const pastDate = dayjs('2024-01-15');
const futureDate = dayjs('2099-12-31');
const invalidDate = dayjs('not-a-date');

// ── Date Validation ────────────────────────────────────────────────────────

describe('validate — date', () => {
  test('null date returns "Invoice date is required"', () => {
    const result = validate(null, [line('100')]);
    expect(result?.date).toBe('Invoice date is required');
  });

  test('invalid date returns "Invalid date"', () => {
    const result = validate(invalidDate, [line('100')]);
    expect(result?.date).toBe('Invalid date');
  });

  test('future date returns "Invoice date cannot be in the future"', () => {
    const result = validate(futureDate, [line('100')]);
    expect(result?.date).toBe('Invoice date cannot be in the future');
  });

  test('valid past date returns no date error', () => {
    const result = validate(pastDate, [line('100')]);
    expect(result?.date).toBeUndefined();
  });

  test('today is accepted', () => {
    const result = validate(dayjs(), [line('100')]);
    expect(result?.date).toBeUndefined();
  });
});

// ── Amount Validation ──────────────────────────────────────────────────────

describe('validate — amount', () => {
  test('blank amount returns "Enter amount"', () => {
    const result = validate(pastDate, [line('')]);
    expect(result?.lines[0].amount).toBe('Enter amount');
  });

  test('negative amount returns "Must be a positive number"', () => {
    const result = validate(pastDate, [line('-50')]);
    expect(result?.lines[0].amount).toBe('Must be a positive number');
  });

  test('zero amount returns "Must be a positive number"', () => {
    const result = validate(pastDate, [line('0')]);
    expect(result?.lines[0].amount).toBe('Must be a positive number');
  });

  test('non-numeric amount returns "Must be a positive number"', () => {
    const result = validate(pastDate, [line('abc')]);
    expect(result?.lines[0].amount).toBe('Must be a positive number');
  });

  test('valid positive amount returns no error', () => {
    const result = validate(pastDate, [line('100')]);
    expect(result?.lines[0].amount).toBeUndefined();
  });

  test('decimal amount is accepted', () => {
    const result = validate(pastDate, [line('99.99')]);
    expect(result?.lines[0].amount).toBeUndefined();
  });

  test('amount exceeding MAX_AMOUNT returns error', () => {
    const result = validate(pastDate, [line(String(MAX_AMOUNT + 1))]);
    expect(result?.lines[0].amount).toContain('cannot exceed');
    expect(result?.lines[0].amount).toContain('1,000,000,000');
  });

  test('amount exactly at MAX_AMOUNT is accepted', () => {
    const result = validate(pastDate, [line(String(MAX_AMOUNT))]);
    expect(result?.lines[0].amount).toBeUndefined();
  });

  test('amount just below MAX_AMOUNT is accepted', () => {
    const result = validate(pastDate, [line(String(MAX_AMOUNT - 1))]);
    expect(result?.lines[0].amount).toBeUndefined();
  });
});

// ── Multiple Lines ─────────────────────────────────────────────────────────

describe('validate — multiple lines', () => {
  test('errors array length matches lines length', () => {
    const result = validate(pastDate, [line('100'), line('200'), line('300')]);
    expect(result).toBeNull();
  });

  test('only the invalid line has an error', () => {
    const result = validate(pastDate, [line('100'), line(''), line('300')]);
    expect(result?.lines[0].amount).toBeUndefined();
    expect(result?.lines[1].amount).toBe('Enter amount');
    expect(result?.lines[2].amount).toBeUndefined();
  });

  test('all lines invalid returns errors for each', () => {
    const result = validate(pastDate, [line(''), line('-10')]);
    expect(result?.lines[0].amount).toBe('Enter amount');
    expect(result?.lines[1].amount).toBe('Must be a positive number');
  });
});

// ── Combined ───────────────────────────────────────────────────────────────

describe('validate — combined', () => {
  test('returns null when everything is valid', () => {
    const result = validate(pastDate, [line('100'), line('200')]);
    expect(result).toBeNull();
  });

  test('returns both date and amount errors together', () => {
    const result = validate(null, [line('')]);
    expect(result?.date).toBe('Invoice date is required');
    expect(result?.lines[0].amount).toBe('Enter amount');
  });
});
