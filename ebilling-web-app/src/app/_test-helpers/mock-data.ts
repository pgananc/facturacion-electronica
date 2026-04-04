/**
 * Helper functions to create mock data for tests
 * Centralized to ensure consistency across all test files
 */

import { CompanySubscription } from '../_model/subscription/company-subscription';
import { SubscriptionPlan } from '../_model/subscription/subscription-plan';
import { SubscriptionUsageResponse } from '../_model/subscription/subscription-usage';
import { CanEmitDocumentResponse } from '../_model/subscription/feature-validation-response';

/**
 * Creates a mock SubscriptionPlan
 */
export function createMockSubscriptionPlan(
  overrides?: Partial<SubscriptionPlan>
): SubscriptionPlan {
  return {
    idPlan: 1,
    code: 'PROFESSIONAL',
    name: 'Professional Plan',
    description: 'Professional Plan Description',
    priceAnnual: 900.0,
    priceMonthly: 75.0,
    documentLimitYearly: 2000,
    userLimit: null,
    establishmentLimit: null,
    issuancePointLimit: null,
    hasDashboard: true,
    hasAdvancedReports: true,
    hasDrafts: true,
    hasApiAccess: false,
    hasWebhooks: false,
    hasErpIntegration: false,
    hasInventory: false,
    hasScheduledReports: false,
    hasCsvImport: false,
    hasGuides: false,
    hasWhatsappNotifications: false,
    isActive: true,
    displayOrder: 3,
    ...overrides,
  };
}

/**
 * Creates a mock CompanySubscription
 */
export function createMockCompanySubscription(
  overrides?: Partial<CompanySubscription>
): CompanySubscription {
  return {
    idSubscription: 1,
    idCompany: 1,
    idPlan: 1,
    status: 'ACTIVE',
    startDate: '2025-01-01',
    expiresAt: '2025-12-31',
    currentYearUsage: 0,
    percentUsed: 0,
    documentsRemaining: 2000,
    plan: createMockSubscriptionPlan(),
    ...overrides,
  };
}

/**
 * Creates a mock SubscriptionUsageResponse
 */
export function createMockSubscriptionUsageResponse(
  overrides?: Partial<SubscriptionUsageResponse>
): SubscriptionUsageResponse {
  return {
    currentMonth: {
      month: '2025-01',
      invoices: 50,
      creditNotes: 5,
      debitNotes: 2,
      retentions: 0,
      guides: 0,
      total: 57,
    },
    currentYear: {
      year: 2025,
      total: 150,
      limit: 2000,
      percentUsed: 7.5,
      remaining: 1850,
    },
    lastMonths: [],
    ...overrides,
  };
}

/**
 * Creates a mock CanEmitDocumentResponse
 */
export function createMockCanEmitDocumentResponse(
  overrides?: Partial<CanEmitDocumentResponse>
): CanEmitDocumentResponse {
  return {
    allowed: true,
    currentUsage: 150,
    limit: 2000,
    remaining: 1850,
    message: 'Can emit document',
    ...overrides,
  };
}
