export interface SubscriptionUsageHistory {
  idHistory: number;
  idCompany: number;
  idSubscription: number;
  monthYear: string;
  invoicesCount: number;
  creditNotesCount: number;
  debitNotesCount: number;
  retentionsCount: number;
  guidesCount: number;
  totalDocuments: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface MonthlyUsage {
  month: string;
  invoices: number;
  creditNotes: number;
  debitNotes: number;
  retentions: number;
  guides: number;
  total: number;
}

export interface YearlyUsage {
  year: number;
  total: number;
  limit: number | null;
  percentUsed: number;
  remaining: number;
}

export interface SubscriptionUsageResponse {
  currentMonth: MonthlyUsage;
  currentYear: YearlyUsage;
  lastMonths: MonthlyUsage[];
}
