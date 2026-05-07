'use client';

import { useState } from 'react';
import { Dayjs } from 'dayjs';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import IconButton from '@mui/material/IconButton';
import MenuItem from '@mui/material/MenuItem';
import Paper from '@mui/material/Paper';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import Alert from '@mui/material/Alert';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import AddIcon from '@mui/icons-material/Add';
import DeleteIcon from '@mui/icons-material/Delete';

const CURRENCIES = [
  'AUD', 'BGN', 'BRL', 'CAD', 'CHF', 'CNY', 'CZK', 'DKK', 'EUR',
  'GBP', 'HKD', 'HUF', 'IDR', 'ILS', 'INR', 'ISK', 'JPY', 'KRW',
  'MXN', 'MYR', 'NOK', 'NZD', 'PHP', 'PLN', 'RON', 'SEK', 'SGD',
  'THB', 'TRY', 'USD', 'ZAR',
];

interface InvoiceLine {
  id: string;
  description: string;
  currency: string;
  amount: string;
}

interface LineErrors {
  amount?: string;
}

interface FormErrors {
  date?: string;
  lines: LineErrors[];
}

const emptyLine = (): InvoiceLine => ({
  id: crypto.randomUUID(),
  description: '',
  currency: 'USD',
  amount: '',
});

function validate(date: Dayjs | null, lines: InvoiceLine[]): FormErrors | null {
  const errors: FormErrors = { lines: lines.map(() => ({})) };
  let hasError = false;

  if (!date || !date.isValid()) {
    errors.date = 'Invoice date is required';
    hasError = true;
  }

  lines.forEach((line, i) => {
    const amount = parseFloat(line.amount);
    if (!line.amount || isNaN(amount) || amount <= 0) {
      errors.lines[i].amount = 'Must be a positive number';
      hasError = true;
    }
  });

  return hasError ? errors : null;
}

export default function InvoiceForm() {
  const [date, setDate] = useState<Dayjs | null>(null);
  const [baseCurrency, setBaseCurrency] = useState('NZD');
  const [lines, setLines] = useState<InvoiceLine[]>([emptyLine()]);
  const [fieldErrors, setFieldErrors] = useState<FormErrors | null>(null);
  const [total, setTotal] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

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

  const addLine = () => setLines(prev => [...prev, emptyLine()]);

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
    <Paper elevation={3} sx={{ maxWidth: 720, mx: 'auto', mt: 6, p: 4 }}>
      <Typography variant="h5" sx={{ fontWeight: 600 }} gutterBottom>
        Invoice Calculator
      </Typography>
      <Typography variant="body2" sx={{ color: 'text.secondary', mb: 3 }}>
        Enter invoice details to calculate the total in your base currency.
      </Typography>

      <Box sx={{ display: 'flex', gap: 2, mb: 3, flexWrap: 'wrap' }}>
        <DatePicker
          label="Invoice Date"
          value={date}
          onChange={val => {
            setDate(val);
            if (fieldErrors?.date) setFieldErrors(prev => prev ? { ...prev, date: undefined } : null);
          }}
          slotProps={{
            textField: {
              required: true,
              error: !!fieldErrors?.date,
              helperText: fieldErrors?.date,
              sx: { flex: 1, minWidth: 180 },
            },
          }}
        />
        <TextField
          select
          label="Base Currency"
          value={baseCurrency}
          onChange={e => setBaseCurrency(e.target.value)}
          required
          sx={{ flex: 1, minWidth: 140 }}
        >
          {CURRENCIES.map(c => (
            <MenuItem key={c} value={c}>{c}</MenuItem>
          ))}
        </TextField>
      </Box>

      <Divider sx={{ mb: 2 }} />

      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
        <Typography variant="subtitle1" sx={{ fontWeight: 500 }}>Invoice Lines</Typography>
        <Button startIcon={<AddIcon />} size="small" onClick={addLine} variant="outlined">
          Add Line
        </Button>
      </Box>

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 3 }}>
        {lines.map((line, i) => (
          <Box key={line.id} sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start', flexWrap: 'wrap' }}>
            <TextField
              label="Description"
              value={line.description}
              onChange={e => updateLine(i, 'description', e.target.value)}
              size="small"
              sx={{ flex: 3, minWidth: 160 }}
            />
            <TextField
              label="Amount"
              type="number"
              slotProps={{ htmlInput: { min: 0, step: '0.01' } }}
              value={line.amount}
              onChange={e => updateLine(i, 'amount', e.target.value)}
              size="small"
              required
              error={!!fieldErrors?.lines[i]?.amount}
              helperText={fieldErrors?.lines[i]?.amount}
              sx={{ flex: 1, minWidth: 100 }}
            />
            <TextField
              select
              label="Currency"
              value={line.currency}
              onChange={e => updateLine(i, 'currency', e.target.value)}
              size="small"
              sx={{ flex: 1, minWidth: 100 }}
            >
              {CURRENCIES.map(c => (
                <MenuItem key={c} value={c}>{c}</MenuItem>
              ))}
            </TextField>
            <IconButton
              aria-label="remove line"
              onClick={() => removeLine(i)}
              disabled={lines.length === 1}
              color="error"
              size="small"
              sx={{ mt: 0.5 }}
            >
              <DeleteIcon />
            </IconButton>
          </Box>
        ))}
      </Box>

      <Button
        variant="contained"
        size="large"
        fullWidth
        onClick={handleSubmit}
        disabled={loading}
        startIcon={loading ? <CircularProgress size={18} color="inherit" /> : null}
      >
        {loading ? 'Calculating…' : 'Calculate Total'}
      </Button>

      {total !== null && (
        <Alert severity="success" sx={{ mt: 3 }} icon={false}>
          <Typography variant="subtitle2">Total ({baseCurrency})</Typography>
          <Typography variant="h5" sx={{ fontWeight: 700 }}>
            {parseFloat(total).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })} {baseCurrency}
          </Typography>
        </Alert>
      )}

      {error !== null && (
        <Alert severity="error" sx={{ mt: 3 }}>
          {error}
        </Alert>
      )}
    </Paper>
  );
}
