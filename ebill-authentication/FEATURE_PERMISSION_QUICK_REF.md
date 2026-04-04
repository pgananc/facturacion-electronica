# Feature Permission System - Quick Reference

## 🎯 Main Validation Endpoint

### Complete Validation (Role + Plan) 🚀
**Use when**: You want full validation including subscription and plan checks

```bash
POST /api/permission/validate-feature
{
  "featureCode": "INVOICE_CREATE",
  "idCompany": 456
}
```

**Response Success**:
```json
{
  "allowed": true,
  "message": "Access granted to feature 'Crear Facturas' under plan 'PROFESSIONAL'"
}
```

**Response Denied**:
```json
{
  "allowed": false,
  "reason": "PLAN_NOT_ALLOWED",
  "message": "Your current plan (BASIC) does not include this feature. Upgrade to access it.",
  "upgradeUrl": "/subscriptions/upgrade"
}
```

---

## 📋 Available Features (15 total)

| Feature Code | Feature Name | Module | Allowed Roles | Required Plans |
|--------------|--------------|--------|---------------|----------------|
| INVOICE_CREATE | Crear Facturas | INVOICING | [1,2,3] | [FREE,BASIC,PROFESSIONAL,ENTERPRISE] |
| INVOICE_VOID | Anular Facturas | INVOICING | [1,2] | [BASIC,PROFESSIONAL,ENTERPRISE] |
| INVOICE_EXPORT | Exportar Facturas | INVOICING | [1,2,3] | [PROFESSIONAL,ENTERPRISE] |
| CREDIT_NOTE_CREATE | Crear Notas de Crédito | CREDIT_NOTES | [1,2,3] | [BASIC,PROFESSIONAL,ENTERPRISE] |
| CREDIT_NOTE_VOID | Anular Notas de Crédito | CREDIT_NOTES | [1,2] | [BASIC,PROFESSIONAL,ENTERPRISE] |
| CLIENT_CREATE | Crear Clientes | CLIENTS | [1,2,3] | [FREE,BASIC,PROFESSIONAL,ENTERPRISE] |
| CLIENT_IMPORT | Importar Clientes | CLIENTS | [1,2] | [PROFESSIONAL,ENTERPRISE] |
| REPORT_OPERATIONAL | Reportes Operativos | REPORTS | [1,2,3] | [BASIC,PROFESSIONAL,ENTERPRISE] |
| REPORT_SALES | Reportes Comerciales | REPORTS | [1,2] | [PROFESSIONAL,ENTERPRISE] |
| REPORT_ADVANCED | Reportes Avanzados | REPORTS | [1,2] | [ENTERPRISE] |
| USER_MANAGEMENT | Gestión de Usuarios | ADMINISTRATION | [1,2] | [BASIC,PROFESSIONAL,ENTERPRISE] |
| COMPANY_SETTINGS | Configuración de Empresa | ADMINISTRATION | [1,2] | [FREE,BASIC,PROFESSIONAL,ENTERPRISE] |
| ESTABLISHMENT_MANAGEMENT | Gestión de Establecimientos | ADMINISTRATION | [1,2] | [BASIC,PROFESSIONAL,ENTERPRISE] |
| API_ACCESS | Acceso API | API | [1,2] | [PROFESSIONAL,ENTERPRISE] |
| WEBHOOK_CONFIG | Configuración de Webhooks | API | [1] | [ENTERPRISE] |

**Role IDs**:
- 1 = ROLE_ADMIN_GLOBAL (Super Admin - bypasses all checks)
- 2 = ROLE_ADMIN_COMPANY (Company Admin)
- 3 = ROLE_USER (Regular User)

---

## 🔍 Denial Reasons

When access is denied, the response includes a `reason` field:

| Reason | Description | Upgrade Needed |
|--------|-------------|----------------|
| `NO_ROLE` | User's role doesn't have permission | Contact admin to change role |
| `NO_SUBSCRIPTION` | Company has no active subscription | Yes - Purchase plan |
| `EXPIRED` | Subscription has expired | Yes - Renew subscription |
| `PLAN_NOT_ALLOWED` | Current plan doesn't include feature | Yes - Upgrade plan |
| `LIMIT_EXCEEDED` | Usage limit reached (e.g., invoice limit) | Yes - Upgrade plan |
| `FEATURE_NOT_FOUND` | Feature code doesn't exist | N/A |
| `INACTIVE_FEATURE` | Feature is disabled | N/A |

---

## 🛠️ Management Endpoints (Super Admin Only)

### Get All Features for Management
```bash
GET /api/permission/permissions/features
# Returns features with permission details (only leaf menu features)
```

### Get Role Permissions
```bash
GET /api/permission/permissions/role/3
# Returns all features accessible by role 3
```

### Get User Allowed Features
```bash
GET /api/permission/user/features
# Returns feature codes that authenticated user can access
# Extracts idUser and idCompany from JWT token
```

---

## 🎨 Frontend Integration Example

### Get User Allowed Features (Recommended)
```typescript
async function getUserAllowedFeatures(): Promise<string[]> {
  const response = await fetch('http://localhost:8280/api/permission/user/features', {
    method: 'GET',
    headers: { 
      'Authorization': `Bearer ${getToken()}`,
      'Content-Type': 'application/json' 
    }
  });
  
  return await response.json();
}

// Usage in React component
const allowedFeatures = await getUserAllowedFeatures();
const canCreateInvoice = allowedFeatures.includes('INVOICE_CREATE');
if (canCreateInvoice) {
  // Show "Create Invoice" button
}
```

### Validate Before Action (Full Validation)
```typescript
async function validateFeatureAccess(
  featureCode: string, 
  companyId: number
): Promise<{ allowed: boolean, message: string, upgradeUrl?: string }> {
  const response = await fetch('http://localhost:8280/api/permission/validate-feature', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ featureCode, idCompany: companyId })
  });
  
  return await response.json();
}

// Usage before creating invoice
const validation = await validateFeatureAccess('INVOICE_CREATE', currentCompany.id);
if (!validation.allowed) {
  if (validation.upgradeUrl) {
    // Show upgrade modal with validation.message and link to validation.upgradeUrl
    showUpgradeModal(validation.message, validation.upgradeUrl);
  } else {
    // Show error message
    showError(validation.message);
  }
  return;
}

// Proceed with invoice creation
createInvoice();
```

---

## 🔐 Super Admin Bypass

**Super Admin (Role ID: 1)** automatically has access to ALL features, bypassing:
- Role validation
- Plan validation  
- Usage limits

---

## 📊 Database Queries

### Check Feature Configuration
```sql
SELECT * FROM feature_permission 
WHERE feature_code = 'INVOICE_CREATE';
```

### See All Features for a Role
```sql
SELECT feature_code, feature_name, module 
FROM feature_permission 
WHERE 3 = ANY(allowed_roles) AND is_active = true;
```

### See All Features for a Plan
```sql
SELECT feature_code, feature_name, module 
FROM feature_permission 
WHERE 'BASIC' = ANY(required_plans) AND is_active = true;
```

### Count Features by Module
```sql
SELECT module, COUNT(*) as feature_count 
FROM feature_permission 
WHERE is_active = true
GROUP BY module 
ORDER BY module;
```

---

## 🚨 Common Issues & Solutions

### Issue: "Feature not found"
**Solution**: Check that feature code exists in database and `is_active = true`

### Issue: "User has no roles"
**Solution**: Ensure user is assigned to a company with active roles in `user_company_role` table

### Issue: "Unable to verify subscription"
**Solution**: 
1. Check ebill-billing service is running on port 8480
2. Check network connectivity between services
3. Verify endpoint `/api/subscription/{idCompany}/current` exists

### Issue: Super admin still gets denied
**Solution**: Verify role ID is 1 or role name is exactly "ROLE_ADMIN_GLOBAL"

---

## 📞 Support

For issues or questions, check:
1. Application logs at `logs/ebill-authentication.log`
2. Database migration status: `SELECT * FROM flyway WHERE script = 'V2025121400__create_feature_permission_table.sql'`
3. Service health: `http://localhost:8280/actuator/health`
