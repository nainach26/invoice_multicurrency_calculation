import Box from '@mui/material/Box';
import InvoiceForm from '@/components/InvoiceForm';

export default function Home() {
  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'grey.100', py: 4, px: 2 }}>
      <InvoiceForm />
    </Box>
  );
}
