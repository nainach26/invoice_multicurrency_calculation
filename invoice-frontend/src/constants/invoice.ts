import { InvoiceLine } from '../types/invoice';

export const MAX_LINES = 25;

export const CURRENCIES = [
  'AUD', 'BGN', 'BRL', 'CAD', 'CHF', 'CNY', 'CZK', 'DKK', 'EUR',
  'GBP', 'HKD', 'HUF', 'IDR', 'ILS', 'INR', 'ISK', 'JPY', 'KRW',
  'MXN', 'MYR', 'NOK', 'NZD', 'PHP', 'PLN', 'RON', 'SEK', 'SGD',
  'THB', 'TRY', 'USD', 'ZAR',
];

export const emptyLine = (): InvoiceLine => ({
  id: crypto.randomUUID(),
  description: '',
  currency: 'USD',
  amount: '',
});
