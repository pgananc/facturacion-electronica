import { SubscriptionPlan } from './subscription-plan';

export interface CompanySubscription {
  idSubscription: number;
  idCompany: number;
  idPlan: number;
  plan: SubscriptionPlan;
  status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'SUSPENDED' | 'TRIAL';
  startDate: string;
  expiresAt: string;
  currentYearUsage: number;
  percentUsed: number;
  documentsRemaining: number;
  lastResetDate?: string;
  paymentMethod?: string;
  lastPaymentDate?: string;
  lastPaymentAmount?: number;
  paymentReference?: string;
  notes?: string;
  createdAt?: string;
  updatedAt?: string;
}
