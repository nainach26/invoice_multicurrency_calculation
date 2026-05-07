'use client';

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
import { InvoiceLine, FormErrors } from '../types/invoice';
import { CURRENCIES } from '../constants/invoice';

interface Props {
  date: Dayjs | null;
  baseCurrency: string;
  lines: InvoiceLine[];
  fieldErrors: FormErrors | null;
  total: string | null;
  error: string | null;
  loading: boolean;
  onDateChange: (val: Dayjs | null) => void;
  onBaseCurrencyChange: (val: string) => void;
  onUpdateLine: (index: number, field: keyof Omit<InvoiceLine, 'id'>, value: string) => void;
  onAddLine: () => void;
  onRemoveLine: (index: number) => void;
  onSubmit: () => void;
}

export default function InvoiceFormView({
  date, baseCurrency, lines, fieldErrors, total, error, loading,
  onDateChange, onBaseCurrencyChange, onUpdateLine, onAddLine, onRemoveLine, onSubmit,
}: Props) {
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
          onChange={onDateChange}
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
          onChange={e => onBaseCurrencyChange(e.target.value)}
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
        <Button startIcon={<AddIcon />} size="small" onClick={onAddLine} variant="outlined">
          Add Line
        </Button>
      </Box>

      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mb: 3 }}>
        {lines.map((line, i) => (
          <Box key={line.id} sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start', flexWrap: 'wrap' }}>
            <TextField
              label="Description"
              value={line.description}
              onChange={e => onUpdateLine(i, 'description', e.target.value)}
              size="small"
              sx={{ flex: 3, minWidth: 160 }}
            />
            <TextField
              label="Amount"
              type="number"
              slotProps={{ htmlInput: { min: 0, step: '0.01' } }}
              value={line.amount}
              onChange={e => onUpdateLine(i, 'amount', e.target.value)}
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
              onChange={e => onUpdateLine(i, 'currency', e.target.value)}
              size="small"
              sx={{ flex: 1, minWidth: 100 }}
            >
              {CURRENCIES.map(c => (
                <MenuItem key={c} value={c}>{c}</MenuItem>
              ))}
            </TextField>
            <IconButton
              aria-label="remove line"
              onClick={() => onRemoveLine(i)}
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
        onClick={onSubmit}
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
