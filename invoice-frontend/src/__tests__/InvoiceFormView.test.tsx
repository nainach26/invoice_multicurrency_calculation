import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import InvoiceFormView from '../components/InvoiceFormView';
import { MAX_LINES } from '../constants/invoice';
import { InvoiceLine, FormErrors } from '../types/invoice';

jest.mock('@mui/x-date-pickers/DatePicker', () => ({
  DatePicker: ({ label, onChange, slotProps }: any) => (
    <input
      aria-label={label}
      data-testid="date-picker"
      onChange={e => onChange(e.target.value)}
      className={slotProps?.textField?.error ? 'error' : ''}
    />
  ),
}));

const makeLine = (id: string, amount = '100', currency = 'NZD'): InvoiceLine => ({
  id,
  description: 'Item',
  currency,
  amount,
});

const defaultProps = {
  date: null,
  baseCurrency: 'NZD',
  lines: [makeLine('1')],
  fieldErrors: null,
  total: null,
  error: null,
  loading: false,
  onDateChange: jest.fn(),
  onBaseCurrencyChange: jest.fn(),
  onUpdateLine: jest.fn(),
  onAddLine: jest.fn(),
  onRemoveLine: jest.fn(),
  onSubmit: jest.fn(),
};

const renderView = (overrides = {}) =>
  render(<InvoiceFormView {...defaultProps} {...overrides} />);

// ── Rendering ──────────────────────────────────────────────────────────────

describe('InvoiceFormView — rendering', () => {
  test('renders the Invoice Calculator heading', () => {
    renderView();
    expect(screen.getByText('Invoice Calculator')).toBeInTheDocument();
  });

  test('renders the Calculate Total button', () => {
    renderView();
    expect(screen.getByRole('button', { name: /calculate total/i })).toBeInTheDocument();
  });

  test('renders the Add Line button', () => {
    renderView();
    expect(screen.getByRole('button', { name: /add line/i })).toBeInTheDocument();
  });

  test('renders the Invoice Lines section', () => {
    renderView();
    expect(screen.getByText('Invoice Lines')).toBeInTheDocument();
  });

  test('renders one line row by default', () => {
    renderView();
    expect(screen.getAllByLabelText('Description')).toHaveLength(1);
  });

  test('renders multiple line rows', () => {
    renderView({ lines: [makeLine('1'), makeLine('2'), makeLine('3')] });
    expect(screen.getAllByLabelText('Description')).toHaveLength(3);
  });
});

// ── Button States ──────────────────────────────────────────────────────────

describe('InvoiceFormView — button states', () => {
  test('Add Line button is enabled below MAX_LINES', () => {
    renderView({ lines: [makeLine('1')] });
    expect(screen.getByRole('button', { name: /add line/i })).not.toBeDisabled();
  });

  test('Add Line button is disabled at MAX_LINES', () => {
    const lines = Array.from({ length: MAX_LINES }, (_, i) => makeLine(String(i)));
    renderView({ lines });
    expect(screen.getByRole('button', { name: /add line/i })).toBeDisabled();
  });

  test('Delete button is disabled when only one line exists', () => {
    renderView({ lines: [makeLine('1')] });
    expect(screen.getByRole('button', { name: /remove line/i })).toBeDisabled();
  });

  test('Delete button is enabled when multiple lines exist', () => {
    renderView({ lines: [makeLine('1'), makeLine('2')] });
    const deleteButtons = screen.getAllByRole('button', { name: /remove line/i });
    deleteButtons.forEach(btn => expect(btn).not.toBeDisabled());
  });

  test('Calculate Total button is disabled while loading', () => {
    renderView({ loading: true });
    expect(screen.getByRole('button', { name: /calculating/i })).toBeDisabled();
  });

  test('Calculate Total button shows Calculating… text when loading', () => {
    renderView({ loading: true });
    expect(screen.getByText('Calculating…')).toBeInTheDocument();
  });
});

// ── Alerts ─────────────────────────────────────────────────────────────────

describe('InvoiceFormView — alerts', () => {
  test('shows success alert with total when total is provided', () => {
    renderView({ total: '123.45', baseCurrency: 'NZD' });
    expect(screen.getByText(/123\.45/)).toBeInTheDocument();
    expect(screen.getByText(/Total \(NZD\)/i)).toBeInTheDocument();
  });

  test('shows error alert when error is provided', () => {
    renderView({ error: 'Something went wrong' });
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  test('does not show success alert when total is null', () => {
    renderView({ total: null });
    expect(screen.queryByText(/Total \(/i)).not.toBeInTheDocument();
  });

  test('does not show error alert when error is null', () => {
    renderView({ error: null });
    expect(screen.queryByText('Something went wrong')).not.toBeInTheDocument();
  });

  test('shows field error on amount input', () => {
    const fieldErrors: FormErrors = { lines: [{ amount: 'Enter amount' }] };
    renderView({ fieldErrors });
    expect(screen.getByText('Enter amount')).toBeInTheDocument();
  });
});

// ── Interactions ───────────────────────────────────────────────────────────

describe('InvoiceFormView — interactions', () => {
  test('calls onAddLine when Add Line is clicked', () => {
    const onAddLine = jest.fn();
    renderView({ onAddLine });
    fireEvent.click(screen.getByRole('button', { name: /add line/i }));
    expect(onAddLine).toHaveBeenCalledTimes(1);
  });

  test('calls onSubmit when Calculate Total is clicked', () => {
    const onSubmit = jest.fn();
    renderView({ onSubmit });
    fireEvent.click(screen.getByRole('button', { name: /calculate total/i }));
    expect(onSubmit).toHaveBeenCalledTimes(1);
  });

  test('calls onRemoveLine with correct index when delete is clicked', () => {
    const onRemoveLine = jest.fn();
    renderView({
      lines: [makeLine('1'), makeLine('2')],
      onRemoveLine,
    });
    const deleteButtons = screen.getAllByRole('button', { name: /remove line/i });
    fireEvent.click(deleteButtons[0]);
    expect(onRemoveLine).toHaveBeenCalledWith(0);
  });

  test('renders base currency selector', () => {
    renderView();
    expect(screen.getByRole('combobox', { name: /base currency/i })).toBeInTheDocument();
  });
});
