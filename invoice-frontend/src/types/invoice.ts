export interface InvoiceLine {
  id: string;
  description: string;
  currency: string;
  amount: string;
}

export interface LineErrors {
  amount?: string;
}

export interface FormErrors {
  date?: string;
  lines: LineErrors[];
}
