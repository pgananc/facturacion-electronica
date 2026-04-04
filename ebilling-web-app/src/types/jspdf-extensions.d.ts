import 'jspdf';

declare module 'jspdf' {
  interface jsPDF {
    setLineWidth(width: number): jsPDF;
    line(x1: number, y1: number, x2: number, y2: number): jsPDF;
    getNumberOfPages(): number;
    setPage(page: number): jsPDF;
  }
}
