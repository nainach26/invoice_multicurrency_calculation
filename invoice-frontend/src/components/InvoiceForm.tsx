'use client';

import { useState } from 'react';
import { Dayjs } from 'dayjs';
import { InvoiceLine, FormErrors } from '../types/invoice';
import { CURRENCIES, emptyLine, MAX_AMOUNT } from '../constants/invoice';
import InvoiceFormView from './InvoiceFormView';

export type { InvoiceLine, FormErrors };
export { CURRENCIES };

export function validate(date: Dayjs | null, lines: InvoiceLine[]): FormErrors | null {
  const errors: FormErrors = { lines: lines.map(() => ({})) };
  let hasError = false;

  if (!date) {
    errors.date = 'Invoice date is required';
    hasError = true;
  } else if (!date.isValid()) {
    errors.date = 'Invalid date';
    hasError = true;
  } else if (date.isAfter(new Date())) {
    errors.date = 'Invoice date cannot be in the future';
    hasError = true;
  }

  lines.forEach((line, i) => {
    if (!line.amount) {
      errors.lines[i].amount = 'Enter amount';
      hasError = true;
    } else {
      const amount = parseFloat(line.amount);
      if (isNaN(amount) || amount <= 0) {
        errors.lines[i].amount = 'Must be a positive number';
        hasError = true;
      } else if (amount > MAX_AMOUNT) {
        errors.lines[i].amount = `Amount cannot exceed ${MAX_AMOUNT.toLocaleString()}`;
        hasError = true;
      }
    }
  });

  return hasError ? errors : null;
}

export default function InvoiceForm() {
  const [date, setDate] = useState<Dayjs | null>(null);
  const [baseCurrency, setBaseCurrency] = useState('AUD');
  const [lines, setLines] = useState<InvoiceLine[]>([emptyLine()]);
  const [fieldErrors, setFieldErrors] = useState<FormErrors | null>(null);
  const [total, setTotal] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleDateChange = (val: Dayjs | null) => {
    setDate(val);
    if (fieldErrors?.date) setFieldErrors(prev => prev ? { ...prev, date: undefined } : null);
  };

  const updateLine = (index: number, field: keyof Omit<InvoiceLine, 'id'>, value: string) => {
    setLines(prev => prev.map((line, i) => i === index ? { ...line, [field]: value } : line));
    if (fieldErrors?.lines[index]?.amount && field === 'amount') {
      setFieldErrors(prev => {
        if (!prev) return null;
        const lines = [...prev.lines];
        lines[index] = { ...lines[index], amount: undefined };
        return { ...prev, lines };
      });
    }
  };

  const addLine = () => setLines(prev => [emptyLine(), ...prev]);

  const removeLine = (index: number) => {
    setLines(prev => prev.filter((_, i) => i !== index));
    setFieldErrors(prev => {
      if (!prev) return null;
      return { ...prev, lines: prev.lines.filter((_, i) => i !== index) };
    });
  };

  const handleSubmit = async () => {
    const errors = validate(date, lines);
    if (errors) {
      setFieldErrors(errors);
      return;
    }

    setFieldErrors(null);
    setTotal(null);
    setError(null);
    setLoading(true);

    try {
      const payload = {
        invoice: {
          currency: baseCurrency,
          date: date!.format('YYYY-MM-DD'),
          lines: lines.map(({ description, currency, amount }) => ({
            description,
            currency,
            amount: parseFloat(amount),
          })),
        },
      };

      const res = await fetch('/api/invoice/total', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const text = await res.text();

      if (!res.ok) {
        setError(text || `Request failed with status ${res.status}`);
      } else {
        setTotal(text);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to reach the invoice service.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <InvoiceFormView
      date={date}
      baseCurrency={baseCurrency}
      lines={lines}
      fieldErrors={fieldErrors}
      total={total}
      error={error}
      loading={loading}
      onDateChange={handleDateChange}
      onBaseCurrencyChange={setBaseCurrency}
      onUpdateLine={updateLine}
      onAddLine={addLine}
      onRemoveLine={removeLine}
      onSubmit={handleSubmit}
    />
  );
}
