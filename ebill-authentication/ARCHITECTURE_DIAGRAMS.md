# Feature Permission System - Architecture Diagrams

## 📐 System Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                            Frontend Application                         │
│                         (React / Vue / Angular)                         │
└────────────────┬────────────────────────────┬─────────────────────────┘
                 │                            │
                 │ Check UI Visibility        │ Before Action Execution
                 │ (Fast, Local)              │ (Complete Validation)
                 │                            │
                 ▼                            ▼
┌────────────────────────────────────────────────────────────────────────┐
│                    ebill-authentication :8280                          │
│                    PermissionController                                │
│                                                                        │
│  ┌──────────────────────────────┐    ┌──────────────────────────────┐ │
│  │ GET /user/features           │    │ POST /validate-feature       │ │
│  │ (Get Allowed Features)       │    │ (Complete Validation)        │ │
│  │                              │    │                              │ │
│  │ 1. Extract idUser from JWT  │    │ 1. Extract idUser from JWT   │ │
│  │ 2. Extract idCompany from  │    │ 2. Extract idCompany from   │ │
│  │    JWT                       │    │    JWT                       │ │
│  │ 3. Get User Roles            │    │ 3. Get Feature by Code       │ │
│  │ 4. Get Subscription          │    │ 4. Check Super Admin         │ │
│  │ 5. Filter Features by        │    │ 5. Validate Role              │ │
│  │    Role + Plan               │    │ 6. Call ebill-billing ────┐  │ │
│  │ 6. Return Feature Codes[]   │    │ 7. Validate Subscription  │  │ │
│  │                              │    │ 8. Check Plan Includes    │  │ │
│  │ Response Time: ~100-200ms   │    │    Feature                │  │ │
│  └──────────────────────────────┘    │ 9. Check Usage Limits     │  │ │
│                                      │ 10. Return Result         │  │ │
│                                      │                           │  │ │
│                                      │ Response Time: ~100-300ms │  │ │
│                                      └───────────────────────────┼──┘ │
└──────────────────┬──────────────────────────────────────────────┼─────┘
                   │                                               │
                   │ Query                                         │ REST Call
                   ▼                                               ▼
┌────────────────────────────────────────────┐  ┌──────────────────────────┐
│         PostgreSQL Database                │  │  ebill-billing :8480     │
│         ebill-dev-auth                     │  │                          │
│                                            │  │  GET /api/subscription/  │
│  ┌──────────────────────────────────────┐ │  │      {idCompany}/current │
│  │  feature_permission                  │ │  │                          │
│  │                                      │ │  │  Returns:                │
│  │  - id_permission                     │ │  │  - idSubscription        │
│  │  - feature_code (UNIQUE)             │ │  │  - planCode              │
│  │  - feature_name                      │ │  │  - status                │
│  │  - description                       │ │  │  - startDate/endDate     │
│  │  - module                            │ │  │  - features[]            │
│  │  - allowed_roles (INTEGER[])         │ │  │  - invoiceLimit/Used     │
│  │  - required_plans (VARCHAR[])        │ │  │  - isActive              │
│  │  - is_active                         │ │  └──────────────────────────┘
│  │  - created_at / updated_at           │ │
│  │                                      │ │
│  │  Records: 15 base features           │ │
│  └──────────────────────────────────────┘ │
│                                            │
│  Other Tables Used:                        │
│  - users                                   │
│  - user_company                            │
│  - user_company_role                       │
│  - role                                    │
└────────────────────────────────────────────┘
```

---

## 🔄 Request Flow Diagrams

### Flow 1: Get User Allowed Features

```
User Action                 Frontend              Controller              Service                Repository
─────────────────────────────────────────────────────────────────────────────────────────────────────────
   │                           │                      │                      │                       │
   │ Page Load / Login         │                      │                      │                       │
   ├──────────────────────────►│                      │                      │                       │
   │                           │                      │                      │                       │
   │                           │ GET /user/features   │                      │                       │
   │                           │ (JWT in header)      │                      │                       │
   │                           ├─────────────────────►│                      │                       │
   │                           │                      │                      │                       │
   │                           │                      │ getUserAllowedFeatures()│                  │
   │                           │                      ├─────────────────────►│                       │
   │                           │                      │                      │                       │
   │                           │                      │                      │ findById(idUser)      │
   │                           │                      │                      ├──────────────────────►│
   │                           │                      │                      │◄──────────────────────┤
   │                           │                      │                      │ User entity           │
   │                           │                      │                      │                       │
   │                           │                      │                      │ findByFeatureCode()   │
   │                           │                      │                      ├──────────────────────►│
   │                           │                      │                      │◄──────────────────────┤
   │                           │                      │                      │ Feature entity        │
   │                           │                      │                      │                       │
   │                           │                      │                      │ findByUser()          │
   │                           │                      │                      ├──────────────────────►│
   │                           │                      │                      │◄──────────────────────┤
   │                           │                      │                      │ UserCompany list      │
   │                           │                      │                      │                       │
   │                           │                      │                      │ Check if Super Admin  │
   │                           │                      │                      │ OR                    │
   │                           │                      │                      │ Role in allowedRoles[]│
   │                           │                      │                      │                       │
   │                           │                      │◄─────────────────────┤                       │
   │                           │                      │ CheckRoleResponse    │                       │
   │                           │◄─────────────────────┤                      │                       │
   │                           │ { allowed: true,     │                      │                       │
   │                           │   message: "..." }   │                      │                       │
   │◄──────────────────────────┤                      │                      │                       │
   │ Show/Hide UI Element      │                      │                      │                       │
   │                           │                      │                      │                       │
   
   ⏱️  Total Time: ~10-20ms (3-4 database queries)
```

---

### Flow 2: Validate Feature Access (Layer 2 - Complete)

```
User Action          Frontend         Controller           Service              ebill-billing        Database
──────────────────────────────────────────────────────────────────────────────────────────────────────────────
   │                    │                  │                    │                       │                 │
   │ Click Submit       │                  │                    │                       │                 │
   ├───────────────────►│                  │                    │                       │                 │
   │                    │                  │                    │                       │                 │
   │                    │ POST /validate-  │                    │                       │                 │
   │                    │      feature     │                    │                       │                 │
   │                    │ { featureCode,   │                    │                       │                 │
   │                    │   idCompany }    │                    │                       │                 │
   │                    ├─────────────────►│                    │                       │                 │
   │                    │                  │                    │                       │                 │
   │                    │                  │ validateFeature()  │                       │                 │
   │                    │                  ├───────────────────►│                       │                 │
   │                    │                  │                    │                       │                 │
   │                    │                  │                    │ Get Feature           │                 │
   │                    │                  │                    ├──────────────────────►│                 │
   │                    │                  │                    │◄──────────────────────┤                 │
   │                    │                  │                    │                       │                 │
   │                    │                  │                    │ Get Company Users     │                 │
   │                    │                  │                    ├──────────────────────►│                 │
   │                    │                  │                    │◄──────────────────────┤                 │
   │                    │                  │                    │                       │                 │
   │                    │                  │                    │ Check Super Admin?    │                 │
   │                    │                  │                    │ ┌──────────────────┐  │                 │
   │                    │                  │                    │ │ IF YES: BYPASS   │  │                 │
   │                    │                  │                    │ │ RETURN ALLOWED   │  │                 │
   │                    │                  │                    │ └──────────────────┘  │                 │
   │                    │                  │                    │                       │                 │
   │                    │                  │                    │ Validate Role         │                 │
   │                    │                  │                    │ (Layer 1 Check)       │                 │
   │                    │                  │                    ├──────────────────────►│                 │
   │                    │                  │                    │◄──────────────────────┤                 │
   │                    │                  │                    │                       │                 │
   │                    │                  │                    │ ┌──────────────────┐  │                 │
   │                    │                  │                    │ │ IF NO ROLE:      │  │                 │
   │                    │                  │                    │ │ RETURN DENIED    │  │                 │
   │                    │                  │                    │ └──────────────────┘  │                 │
   │                    │                  │                    │                       │                 │
   │                    │                  │                    │ GET /subscription/    │                 │
   │                    │                  │                    │     {id}/current      │                 │
   │                    │                  │                    ├──────────────────────►│                 │
   │                    │                  │                    │◄──────────────────────┤                 │
   │                    │                  │                    │ SubscriptionResponse  │                 │
   │                    │                  │                    │                       │                 │
   │                    │                  │                    │ Validate:             │                 │
   │                    │                  │                    │ 1. Status = ACTIVE    │                 │
   │                    │                  │                    │ 2. Not expired        │                 │
   │                    │                  │                    │ 3. Plan includes      │                 │
   │                    │                  │                    │    feature            │                 │
   │                    │                  │                    │ 4. Usage limits OK    │                 │
   │                    │                  │                    │                       │                 │
   │                    │                  │◄───────────────────┤                       │                 │
   │                    │                  │ FeatureValidation  │                       │                 │
   │                    │                  │ Response           │                       │                 │
   │                    │◄─────────────────┤                    │                       │                 │
   │                    │ { allowed: bool, │                    │                       │                 │
   │                    │   reason: "...", │                    │                       │                 │
   │                    │   upgradeUrl: "" }                    │                       │                 │
   │◄───────────────────┤                  │                    │                       │                 │
   │                    │                  │                    │                       │                 │
   │ If Denied:         │                  │                    │                       │                 │
   │ Show Upgrade Modal │                  │                    │                       │                 │
   │                    │                  │                    │                       │                 │
   
   ⏱️  Total Time: ~100-300ms (Database queries + REST call + validations)
```

---

## 🔐 Permission Decision Tree

```
                                Start Validation
                                       │
                                       ▼
                            ┌──────────────────────┐
                            │  Feature Exists &    │
                            │  Active?             │
                            └──────┬───────────────┘
                                   │
                       ┌───────────┴───────────┐
                       │ NO                    │ YES
                       ▼                       ▼
              ┌────────────────┐      ┌────────────────┐
              │ DENIED         │      │ Check User     │
              │ FEATURE_NOT_   │      │ Role           │
              │ FOUND          │      └────────┬───────┘
              └────────────────┘               │
                                               │
                                   ┌───────────┴───────────┐
                                   │ Is Super Admin?       │
                                   └───────┬───────────────┘
                                           │
                               ┌───────────┴───────────┐
                               │ YES                   │ NO
                               ▼                       ▼
                      ┌────────────────┐      ┌────────────────┐
                      │ ALLOWED        │      │ Role in        │
                      │ (Bypass All)   │      │ allowedRoles[] │
                      └────────────────┘      └────────┬───────┘
                                                       │
                                           ┌───────────┴───────────┐
                                           │ NO                    │ YES
                                           ▼                       ▼
                                  ┌────────────────┐      ┌────────────────┐
                                  │ DENIED         │      │ Check          │
                                  │ NO_ROLE        │      │ Subscription   │
                                  └────────────────┘      └────────┬───────┘
                                                                   │
                                                       ┌───────────┴───────────┐
                                                       │ Has Active            │
                                                       │ Subscription?         │
                                                       └───────┬───────────────┘
                                                               │
                                                   ┌───────────┴───────────┐
                                                   │ NO                    │ YES
                                                   ▼                       ▼
                                          ┌────────────────┐      ┌────────────────┐
                                          │ DENIED         │      │ Check          │
                                          │ NO_SUBSCRIPTION│      │ Expiry Date    │
                                          │ + upgradeUrl   │      └────────┬───────┘
                                          └────────────────┘               │
                                                               ┌───────────┴───────────┐
                                                               │ Expired?              │
                                                               └───────┬───────────────┘
                                                                       │
                                                           ┌───────────┴───────────┐
                                                           │ YES                   │ NO
                                                           ▼                       ▼
                                                  ┌────────────────┐      ┌────────────────┐
                                                  │ DENIED         │      │ Plan Includes  │
                                                  │ EXPIRED        │      │ Feature?       │
                                                  │ + upgradeUrl   │      └────────┬───────┘
                                                  └────────────────┘               │
                                                                       ┌───────────┴───────────┐
                                                                       │ NO                    │ YES
                                                                       ▼                       ▼
                                                              ┌────────────────┐      ┌────────────────┐
                                                              │ DENIED         │      │ Check Usage    │
                                                              │ PLAN_NOT_      │      │ Limits         │
                                                              │ ALLOWED        │      └────────┬───────┘
                                                              │ + upgradeUrl   │               │
                                                              └────────────────┘               │
                                                                                   ┌───────────┴───────────┐
                                                                                   │ Limit Exceeded?       │
                                                                                   └───────┬───────────────┘
                                                                                           │
                                                                               ┌───────────┴───────────┐
                                                                               │ YES                   │ NO
                                                                               ▼                       ▼
                                                                      ┌────────────────┐      ┌────────────────┐
                                                                      │ DENIED         │      │ ALLOWED        │
                                                                      │ LIMIT_EXCEEDED │      │ (All checks    │
                                                                      │ + upgradeUrl   │      │  passed!)      │
                                                                      └────────────────┘      └────────────────┘
```

---

## 📊 Feature Distribution by Module

```
INVOICING        ████████ 3 features
CREDIT_NOTES     █████ 2 features
CLIENTS          █████ 2 features
REPORTS          ████████ 3 features
ADMINISTRATION   ████████ 3 features
API              █████ 2 features
────────────────────────────────────
TOTAL:           15 features
```

---

## 🎭 Role Access Matrix

```
Feature                    │ Super │ Admin │ User
                           │ (1)   │ (2)   │ (3)
───────────────────────────┼───────┼───────┼──────
INVOICE_CREATE             │  ✅   │  ✅   │  ✅
INVOICE_VOID               │  ✅   │  ✅   │  ❌
INVOICE_EXPORT             │  ✅   │  ✅   │  ✅
CREDIT_NOTE_CREATE         │  ✅   │  ✅   │  ✅
CREDIT_NOTE_VOID           │  ✅   │  ✅   │  ❌
CLIENT_CREATE              │  ✅   │  ✅   │  ✅
CLIENT_IMPORT              │  ✅   │  ✅   │  ❌
REPORT_OPERATIONAL         │  ✅   │  ✅   │  ✅
REPORT_SALES               │  ✅   │  ✅   │  ❌
REPORT_ADVANCED            │  ✅   │  ✅   │  ❌
USER_MANAGEMENT            │  ✅   │  ✅   │  ❌
COMPANY_SETTINGS           │  ✅   │  ✅   │  ❌
ESTABLISHMENT_MANAGEMENT   │  ✅   │  ✅   │  ❌
API_ACCESS                 │  ✅   │  ✅   │  ❌
WEBHOOK_CONFIG             │  ✅   │  ❌   │  ❌
```

---

## 💎 Plan Feature Matrix

```
Feature                    │ FREE │ BASIC │ PRO │ ENT
───────────────────────────┼──────┼───────┼─────┼─────
INVOICE_CREATE             │  ✅  │  ✅   │ ✅  │ ✅
INVOICE_VOID               │  ❌  │  ✅   │ ✅  │ ✅
INVOICE_EXPORT             │  ❌  │  ❌   │ ✅  │ ✅
CREDIT_NOTE_CREATE         │  ❌  │  ✅   │ ✅  │ ✅
CREDIT_NOTE_VOID           │  ❌  │  ✅   │ ✅  │ ✅
CLIENT_CREATE              │  ✅  │  ✅   │ ✅  │ ✅
CLIENT_IMPORT              │  ❌  │  ❌   │ ✅  │ ✅
REPORT_OPERATIONAL         │  ❌  │  ✅   │ ✅  │ ✅
REPORT_SALES               │  ❌  │  ❌   │ ✅  │ ✅
REPORT_ADVANCED            │  ❌  │  ❌   │ ❌  │ ✅
USER_MANAGEMENT            │  ❌  │  ✅   │ ✅  │ ✅
COMPANY_SETTINGS           │  ✅  │  ✅   │ ✅  │ ✅
ESTABLISHMENT_MANAGEMENT   │  ❌  │  ✅   │ ✅  │ ✅
API_ACCESS                 │  ❌  │  ❌   │ ✅  │ ✅
WEBHOOK_CONFIG             │  ❌  │  ❌   │ ❌  │ ✅
```

---

## 🗂️ Database Schema

```sql
┌────────────────────────────────────────────────────┐
│             feature_permission                     │
├────────────────────────────────────────────────────┤
│ PK │ id_permission      │ BIGINT               │   │
│    │ feature_code       │ VARCHAR(50) UNIQUE   │ 🔑│
│    │ feature_name       │ VARCHAR(100)         │   │
│    │ description        │ TEXT                 │   │
│    │ module             │ VARCHAR(50)          │ 📇│
│    │ allowed_roles      │ INTEGER[]            │ 🗂️│
│    │ required_plans     │ VARCHAR(50)[]        │ 🗂️│
│    │ is_active          │ BOOLEAN              │ 📇│
│    │ created_at         │ TIMESTAMP            │   │
│    │ updated_at         │ TIMESTAMP            │   │
└────────────────────────────────────────────────────┘

Indexes:
🔑 idx_feature_permission_code (feature_code)
📇 idx_feature_permission_active (is_active)
📇 idx_feature_permission_module (module)
🗂️ idx_feature_permission_allowed_roles (allowed_roles) GIN
```

---

*Diagrams created for ebill-authentication Feature Permission System*
