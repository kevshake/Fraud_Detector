import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  Paper,
  Box,
  Typography,
  CircularProgress,
} from '@mui/material';

/**
 * Props for PaginatedTable component.
 *
 * @template T - row data type
 */
interface PaginatedTableProps<T> {
  columns: {
    id: string;
    label: string;
    minWidth?: number;
    maxWidth?: number;
    align?: 'left' | 'right' | 'center';
    render: (row: T) => React.ReactNode;
  }[];
  rows: T[];
  totalCount: number;
  page: number;
  rowsPerPage: number;
  onPageChange: (page: number) => void;
  onRowsPerPageChange: (rowsPerPage: number) => void;
  rowsPerPageOptions?: number[];
  loading?: boolean;
  emptyMessage?: string;
  stickyHeader?: boolean;
  maxHeight?: string | number;
}

/**
 * Reusable paginated table with responsive sizing.
 * Auto-fits content to screen and syncs with backend pagination.
 */
export function PaginatedTable<T>({
  columns,
  rows,
  totalCount,
  page,
  rowsPerPage,
  onPageChange,
  onRowsPerPageChange,
  rowsPerPageOptions = [5, 10, 15, 20, 25, 30, 40, 50, 100],
  loading = false,
  emptyMessage = 'No records found',
  stickyHeader = true,
  maxHeight,
}: PaginatedTableProps<T>) {
  return (
    <Paper
      sx={{
        width: '100%',
        overflow: 'hidden',
        borderRadius: 2,
        boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
      }}
    >
      <TableContainer
        sx={{
          maxHeight: maxHeight || `calc(100vh - 280px)`,
          '&::-webkit-scrollbar': { width: 6 },
          '&::-webkit-scrollbar-thumb': {
            backgroundColor: 'rgba(0,0,0,0.15)',
            borderRadius: 3,
          },
        }}
      >
        <Table stickyHeader={stickyHeader} size="small">
          <TableHead>
            <TableRow>
              {columns.map((col) => (
                <TableCell
                  key={col.id}
                  align={col.align || 'left'}
                  sx={{
                    minWidth: col.minWidth,
                    maxWidth: col.maxWidth,
                    fontWeight: 600,
                    fontSize: '0.8rem',
                    backgroundColor: '#f8f9fa',
                    borderBottom: '2px solid #e0e0e0',
                    whiteSpace: 'nowrap',
                    py: 1.5,
                  }}
                >
                  {col.label}
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={columns.length} align="center" sx={{ py: 8 }}>
                  <CircularProgress size={32} />
                  <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                    Loading...
                  </Typography>
                </TableCell>
              </TableRow>
            ) : rows.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length} align="center" sx={{ py: 8 }}>
                  <Typography variant="body2" color="text.secondary">
                    {emptyMessage}
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              rows.map((row, idx) => (
                <TableRow
                  hover
                  key={idx}
                  sx={{
                    '&:nth-of-type(even)': { backgroundColor: '#fafafa' },
                    '&:hover': { backgroundColor: '#f0f4ff' },
                    transition: 'background-color 0.15s',
                  }}
                >
                  {columns.map((col) => (
                    <TableCell
                      key={col.id}
                      align={col.align || 'left'}
                      sx={{
                        fontSize: '0.8rem',
                        py: 1,
                        whiteSpace: 'nowrap',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        maxWidth: col.maxWidth,
                      }}
                    >
                      {col.render(row)}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          px: 2,
          borderTop: '1px solid #e0e0e0',
          backgroundColor: '#fafafa',
        }}
      >
        <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.75rem' }}>
          {totalCount > 0
            ? `${page * rowsPerPage + 1}–${Math.min((page + 1) * rowsPerPage, totalCount)} of ${totalCount}`
            : '0 records'}
        </Typography>
        <TablePagination
          component="div"
          count={totalCount}
          page={page}
          onPageChange={(_, newPage) => onPageChange(newPage)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => onRowsPerPageChange(parseInt(e.target.value, 10))}
          rowsPerPageOptions={rowsPerPageOptions}
          labelRowsPerPage="Rows:"
          sx={{
            '.MuiTablePagination-toolbar': { minHeight: 48, pl: 0 },
            '.MuiTablePagination-selectLabel': { fontSize: '0.75rem', mb: 0 },
            '.MuiTablePagination-displayedRows': { fontSize: '0.75rem', mb: 0 },
            '.MuiTablePagination-select': { fontSize: '0.75rem' },
            '.MuiSvgIcon-root': { fontSize: '1rem' },
          }}
        />
      </Box>
    </Paper>
  );
}