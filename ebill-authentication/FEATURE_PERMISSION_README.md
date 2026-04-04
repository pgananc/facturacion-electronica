# 🎯 Feature Permission System

Complete implementation of a two-layer permission validation system for ebill-authentication.

## 📚 Table of Contents
- [Overview](#overview)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [API Endpoints](#api-endpoints)
- [Features List](#features-list)
- [Testing](#testing)
- [Documentation](#documentation)

---

## Overview

This system provides **role-based** and **subscription-based** access control for features in the ebill platform.

### Two Validation Layers:

1. **Layer 1: Role Validation (Local)** 
   - Validates if user's role has permission to access a feature
   - Fast, local database query
   - Use for UI element visibility

2. **Layer 2: Complete Validation (Role + Plan)**
   - Validates role permission (Layer 1)
   - Calls ebill-billing to verify subscription status
   - Checks if subscription plan includes the feature
   - Validates usage limits
   - Use before executing actual actions

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     PermissionController                     │
│                   /api/permission/*                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
         ┌─────────────┴─────────────┐
         │                           │
         ▼                           ▼
┌──────────────────┐      ┌──────────────────────┐
│ user/features    │      │  validate-feature    │
│ (Get Allowed)    │      │  (Complete)          │
│                  │      │                      │
│  1. Extract JWT │      │  1. Extract JWT      │
│  2. Get Roles    │      │  2. Check Role       │
│  3. Get Plan     │      │  3. Call Billing     │
│  4. Filter       │      │  4. Check Plan       │
│  5. Return []    │      │  5. Check Limits     │
│                  │      │  6. Return           │
└──────────────────┘      └────────┬─────────────┘
         │                         │
         │                         │ REST Call
         │                         ▼
         │              ┌────────────────────┐
         │              │   ebill-billing    │
         │              │   :8480            │
         │              │ /api/subscription/ │
         │              │   {id}/current     │
         │              └────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│              feature_permission table (PostgreSQL)           │
│  - 15 base features across 5 modules                         │
│  - allowed_roles: INTEGER[]                                  │
│  - required_plans: VARCHAR[]                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## Quick Start

### 1. Start the Service

```bash
cd /Users/jguerreroh/Development/wallparisoft/facturacion-electronica/ebill-authentication
./scripts/start-local.sh
```

The Flyway migration will automatically create the `feature_permission` table and insert 15 base features.

### 2. Verify Database

```sql
-- Check table creation
SELECT COUNT(*) FROM feature_permission; -- Should return 15

-- View all features
SELECT feature_code, feature_name, module 
FROM feature_permission 
ORDER BY module, feature_code;
```

### 3. Test Endpoints

```bash
# Test complete validation endpoint
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Content-Type: application/json" \
  -d '{"featureCode": "INVOICE_CREATE", "idCompany": 1}'
```

---

## API Endpoints

### Main Endpoint

#### Complete Feature Validation (Role + Plan)
**POST** `/api/permission/validate-feature`

Performs complete validation: role + subscription + plan + limits.

**Request:**
```json
{
  "featureCode": "INVOICE_CREATE",
  "idCompany": 456
}
```

**Response (Success):**
```json
{
  "allowed": true,
  "message": "Access granted to feature 'Crear Facturas' under plan 'PROFESSIONAL'"
}
```

**Response (Denied - Needs Upgrade):**
```json
{
  "allowed": false,
  "reason": "PLAN_NOT_ALLOWED",
  "message": "Your current plan (BASIC) does not include this feature. Upgrade to access it.",
  "upgradeUrl": "/subscriptions/upgrade"
}
```

**Response (Denied - Limit Reached):**
```json
{
  "allowed": false,
  "reason": "LIMIT_EXCEEDED",
  "message": "Invoice limit reached (100/100). Upgrade your plan for more invoices.",
  "upgradeUrl": "/subscriptions/upgrade"
}
```

**Denial Reasons:**
- `NO_ROLE` - User's role doesn't have permission
- `NO_SUBSCRIPTION` - No active subscription
- `EXPIRED` - Subscription expired
- `PLAN_NOT_ALLOWED` - Plan doesn't include feature
- `LIMIT_EXCEEDED` - Usage limit reached

**Status Codes:**
- `200 OK` - Access granted
- `403 FORBIDDEN` - Access denied

---

### Management Endpoints (Super Admin Only)

#### Get All Features for Management
**GET** `/api/permission/permissions/features`

Returns all features with their permission details (only features associated with leaf menus).

**Example:** `/api/permission/permissions/features`

---

#### Get Role Permissions
**GET** `/api/permission/permissions/role/{idRole}`

Returns all features accessible by a specific role.

**Example:** `/api/permission/permissions/role/3`

---

#### Get User Allowed Features
**GET** `/api/permission/user/features`

Returns all feature codes that the authenticated user can access in their current company.
Extracts `idUser` and `idCompany` from JWT token.

**Example:** `/api/permission/user/features`

**Response:**
```json
[
  "INVOICE_CREATE",
  "CLIENT_CREATE",
  "DRAFT_MANAGEMENT"
]
```

---

## Features List

### All 15 Base Features

| Code | Name | Module | Roles | Plans |
|------|------|--------|-------|-------|
| INVOICE_CREATE | Crear Facturas | INVOICING | 1,2,3 | FREE,BASIC,PRO,ENT |
| INVOICE_VOID | Anular Facturas | INVOICING | 1,2 | BASIC,PRO,ENT |
| INVOICE_EXPORT | Exportar Facturas | INVOICING | 1,2,3 | PRO,ENT |
| CREDIT_NOTE_CREATE | Crear Notas de Crédito | CREDIT_NOTES | 1,2,3 | BASIC,PRO,ENT |
| CREDIT_NOTE_VOID | Anular Notas de Crédito | CREDIT_NOTES | 1,2 | BASIC,PRO,ENT |
| CLIENT_CREATE | Crear Clientes | CLIENTS | 1,2,3 | FREE,BASIC,PRO,ENT |
| CLIENT_IMPORT | Importar Clientes | CLIENTS | 1,2 | PRO,ENT |
| REPORT_OPERATIONAL | Reportes Operativos | REPORTS | 1,2,3 | BASIC,PRO,ENT |
| REPORT_SALES | Reportes Comerciales | REPORTS | 1,2 | PRO,ENT |
| REPORT_ADVANCED | Reportes Avanzados | REPORTS | 1,2 | ENT |
| USER_MANAGEMENT | Gestión de Usuarios | ADMINISTRATION | 1,2 | BASIC,PRO,ENT |
| COMPANY_SETTINGS | Configuración de Empresa | ADMINISTRATION | 1,2 | FREE,BASIC,PRO,ENT |
| ESTABLISHMENT_MANAGEMENT | Gestión de Establecimientos | ADMINISTRATION | 1,2 | BASIC,PRO,ENT |
| API_ACCESS | Acceso API | API | 1,2 | PRO,ENT |
| WEBHOOK_CONFIG | Configuración de Webhooks | API | 1 | ENT |

**Role Legend:**
- 1 = ROLE_ADMIN_GLOBAL (Super Admin)
- 2 = ROLE_ADMIN_COMPANY (Company Admin)
- 3 = ROLE_USER (Regular User)

**Plan Legend:**
- FREE = Free Plan
- BASIC = Basic Plan
- PRO = Professional Plan
- ENT = Enterprise Plan

---

## Testing

### Manual Testing

#### Test Complete Validation:
```bash
curl -X POST http://localhost:8280/api/permission/validate-feature \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"featureCode": "INVOICE_CREATE", "idCompany": 1}'
```

#### Get User Allowed Features:
```bash
curl -X GET http://localhost:8280/api/permission/user/features \
  -H "Authorization: Bearer YOUR_TOKEN"
```

#### Get All Features for Management:
```bash
curl -X GET http://localhost:8280/api/permission/permissions/features \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## Documentation

Comprehensive documentation available:

1. **FEATURE_PERMISSION_IMPLEMENTATION.md** - Complete implementation details
2. **FEATURE_PERMISSION_QUICK_REF.md** - Quick reference guide

### Key Files Created:

#### Database:
- `V2025121400__create_feature_permission_table.sql` - Migration script

#### Backend:
- `FeaturePermission.java` - Entity
- `IFeaturePermissionRepository.java` - Repository
- `IFeaturePermissionService.java` - Service interface
- `FeaturePermissionServiceImpl.java` - Service implementation
- `PermissionController.java` - REST controller
- `IBillingClient.java` - FeignClient for ebill-billing integration

#### DTOs:
- `CheckRoleRequestDto.java`
- `CheckRoleResponseDto.java`
- `FeatureValidationRequestDto.java`
- `FeatureValidationResponseDto.java`
- `SubscriptionResponseDto.java`

---

## Super Admin Bypass

**Super Admin** (Role ID: 1, Name: ROLE_ADMIN_GLOBAL) automatically has access to ALL features, bypassing:
- Role validation
- Plan validation
- Usage limits

---

## Dependencies

### Required Services:
1. **PostgreSQL** - For feature_permission table
2. **ebill-billing** (Port 8480) - For subscription validation (Layer 2 only)

### Configuration:
```properties
# application-localhost.properties
ebill.billing.url=http://localhost:8480
```

---

## Support

For issues:
1. Check logs: `logs/ebill-authentication.log`
2. Verify migration: `SELECT * FROM flyway WHERE script LIKE '%feature_permission%'`
3. Test service health: `http://localhost:8280/actuator/health`

---

## License

© 2025 Wallparisoft - ebill-authentication
