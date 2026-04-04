declare module 'xlsx' {
  interface WorkBook {
    Sheets: { [sheet: string]: WorkSheet };
    SheetNames: string[];
  }

  interface WorkSheet {
    [cell: string]: CellObject | any;
    '!cols'?: Array<{ wch?: number; width?: number }>;
    '!rows'?: Array<{ hpt?: number; hpx?: number }>;
    '!merges'?: any[];
    '!ref'?: string;
  }

  interface CellObject {
    v?: any;
    t?: string;
    s?: any;
  }

  namespace utils {
    function book_new(): WorkBook;
    function aoa_to_sheet(data: any[][]): WorkSheet;
    function json_to_sheet(data: any[]): WorkSheet;
    function sheet_add_json(ws: WorkSheet, data: any[], opts?: any): WorkSheet;
    function book_append_sheet(wb: WorkBook, ws: WorkSheet, name: string): void;
  }

  function writeFile(workbook: WorkBook, filename: string, opts?: any): void;
  function write(workbook: WorkBook, opts?: any): any;
}
