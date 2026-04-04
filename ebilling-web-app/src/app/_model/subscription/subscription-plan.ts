export interface SubscriptionPlan {
  idPlan: number;
  code: string;
  name: string;
  description: string;
  priceAnnual: number;
  priceMonthly: number;
  documentLimitYearly: number | null;
  userLimit: number | null;
  establishmentLimit: number | null;
  issuancePointLimit: number | null;
  hasDashboard: boolean;
  hasAdvancedReports: boolean;
  hasDrafts: boolean;
  hasApiAccess: boolean;
  hasWebhooks: boolean;
  hasErpIntegration: boolean;
  hasInventory: boolean;
  hasScheduledReports: boolean;
  hasCsvImport: boolean;
  hasGuides: boolean;
  hasWhatsappNotifications: boolean;
  isActive: boolean;
  displayOrder: number;
  createdAt?: string;
  updatedAt?: string;
}
