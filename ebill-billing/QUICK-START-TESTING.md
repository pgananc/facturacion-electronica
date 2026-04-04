# 🚀 Quick Start - Testing Subscription System

## 📋 Prerequisites

1. PostgreSQL running
2. ebill-billing application running on port 8480
3. Database migrations executed (Flyway)
4. At least one company with data

---

## ⚡ Quick Test Steps

### Step 1: Verify Migration

Check if tables were created and plans inserted:

```sql
-- Check plans
SELECT * FROM subscription_plan ORDER BY display_order;

-- Check subscriptions (should have FREE plans for all companies)
SELECT cs.*, sp.name
FROM company_subscription cs
JOIN subscription_plan sp ON cs.id_plan = sp.id_plan
WHERE cs.status = 'ACTIVE';

-- Check usage history (should be empty initially)
SELECT * FROM subscription_usage_history;
```

**Expected Result:**

- 5 plans (FREE, BASIC, PRO, BUSINESS, ENTERPRISE)
- All existing companies have ACTIVE FREE subscriptions

---

### Step 2: Test API - Get Current Subscription

```bash
curl -X GET http://localhost:8480/api/subscription/1/current
```

**Expected Response:**

```json
{
  "idSubscription": 1,
  "idCompany": 1,
  "plan": {
    "code": "FREE",
    "name": "Plan Gratuito",
    "documentLimitYearly": 50,
    ...
  },
  "status": "ACTIVE",
  "currentYearUsage": 0,
  "percentUsed": 0.00,
  "documentsRemaining": 50,
  "isExpired": false,
  "isLimitReached": false
}
```

✅ **SUCCESS:** Company has FREE plan with 50 documents limit

---

### Step 3: Test API - Check Can Emit

```bash
curl -X POST http://localhost:8480/api/subscription/1/can-emit \
  -H "Content-Type: application/json" \
  -d '{"documentType": "INVOICE"}'
```

**Expected Response:**

```json
{
  "allowed": true,
  "currentUsage": 0,
  "limit": 50,
  "remaining": 50,
  "message": "Puede emitir el documento",
  "reason": "ALLOWED"
}
```

✅ **SUCCESS:** Can emit invoice

---

### Step 4: Create Test Invoice

Create an invoice using your existing endpoint:

```bash
curl -X POST http://localhost:8480/api/invoice \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "idCompany": 1,
    "idClient": 1,
    "idIssuancePoint": 1,
    "environment": "PRODUCCION",
    "subtotalZero": 0,
    "subtotalIva": 100,
    "subtotalIce": 0,
    "discount": 0,
    "tip": 0,
    "totalAmount": 112,
    "invoiceDetails": [
      {
        "idProduct": 1,
        "quantity": 1,
        "unitPrice": 100,
        "subtotal": 100,
        "ivaPercentage": 12
      }
    ],
    "invoicePayments": [
      {
        "idPaymentMethod": 1,
        "amount": 112
      }
    ]
  }'
```

**Expected Result:**

- Invoice created successfully
- Invoice authorized by SRI (if all config is correct)

---

### Step 5: Verify Usage Counter Increased

Check if the trigger updated the usage:

```sql
-- Check monthly usage
SELECT * FROM subscription_usage_history
WHERE id_company = 1
  AND month_year = TO_CHAR(CURRENT_DATE, 'YYYY-MM');

-- Check yearly usage
SELECT current_year_usage
FROM company_subscription
WHERE id_company = 1 AND status = 'ACTIVE';
```

**Expected Result:**

```
invoices_count: 1
total_documents: 1
current_year_usage: 1
```

✅ **SUCCESS:** Usage counters updated automatically!

---

### Step 6: Verify Usage via API

```bash
curl -X GET http://localhost:8480/api/subscription/1/usage
```

**Expected Response:**

```json
{
  "currentMonth": {
    "month": "2025-12",
    "invoices": 1,
    "creditNotes": 0,
    "total": 1
  },
  "currentYear": {
    "year": 2025,
    "total": 1,
    "limit": 50,
    "percentUsed": 2.00,
    "remaining": 49
  },
  "lastMonths": [...]
}
```

✅ **SUCCESS:** API shows correct usage statistics

---

### Step 7: Test Upgrade Plan (Admin Only)

```bash
curl -X POST http://localhost:8480/api/subscription/1/upgrade \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{
    "newPlanCode": "PRO",
    "paymentAmount": 300.00,
    "paymentDate": "2025-12-14T10:00:00",
    "paymentReference": "TEST-UPGRADE-001",
    "notes": "Test upgrade to PRO plan"
  }'
```

**Expected Response:**

```json
{
  "success": true,
  "message": "Plan actualizado exitosamente",
  "subscription": {
    "plan": {
      "code": "PRO",
      "name": "Plan Profesional",
      "documentLimitYearly": 2000
    },
    "currentYearUsage": 1,
    "documentsRemaining": 1999,
    "expiresAt": "2026-12-14T10:00:00"
  }
}
```

✅ **SUCCESS:** Plan upgraded to PRO

---

### Step 8: Test Subscription Validation

Try to create 50+ invoices on a FREE plan:

**Scenario A: Before reaching limit**

```bash
# Create invoice #49
POST /api/invoice
# Response: 200 OK - Invoice created
```

**Scenario B: At limit**

```bash
# Create invoice #50
POST /api/invoice
# Response: 200 OK - Invoice created (last one!)
```

**Scenario C: Over limit**

```bash
# Try to create invoice #51
POST /api/invoice
# Response: 403 FORBIDDEN
{
  "code": 403,
  "status": "FORBIDDEN",
  "message": "Ha alcanzado el límite de documentos de su plan"
}
```

✅ **SUCCESS:** Validation working correctly!

---

### Step 9: Test Credit Note Validation

Create a credit note:

```bash
curl -X POST http://localhost:8480/api/credit-note \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "idCompany": 1,
    "idClient": 1,
    "originalInvoiceId": 1,
    ...
  }'
```

**Expected Result:**

- Credit note validated before creation
- If allowed, credit note created successfully
- Usage counter increased

✅ **SUCCESS:** Credit note validation working

---

### Step 10: Test Super Admin Bypass

Create invoices as SUPER_ADMIN user (even if limit reached):

```bash
# User with SUPER_ADMIN role
curl -X POST http://localhost:8480/api/invoice \
  -H "Authorization: Bearer SUPER_ADMIN_TOKEN" \
  -d '{...}'
```

**Expected Result:**

- Invoice created successfully
- **No validation** of subscription limits

✅ **SUCCESS:** Super admin bypass working

---

## 🔍 Verification Checklist

- [ ] ✅ Database tables created
- [ ] ✅ 5 plans inserted
- [ ] ✅ Existing companies migrated to FREE
- [ ] ✅ GET /current endpoint works
- [ ] ✅ GET /usage endpoint works
- [ ] ✅ POST /can-emit endpoint works
- [ ] ✅ POST /upgrade endpoint works (admin)
- [ ] ✅ Invoice validation works
- [ ] ✅ Credit note validation works
- [ ] ✅ Usage counter increases automatically
- [ ] ✅ Trigger works on authorized documents
- [ ] ✅ Super admin bypass works
- [ ] ✅ Limit reached blocks document creation

---

## 🧪 Advanced Testing Scenarios

### Test 1: Feature Not Available

Try to create a debit note on FREE plan:

```bash
curl -X POST http://localhost:8480/api/subscription/1/can-emit \
  -H "Content-Type: application/json" \
  -d '{"documentType": "DEBIT_NOTE"}'
```

**Expected Response:**

```json
{
  "allowed": false,
  "message": "El tipo de documento DEBIT_NOTE no está disponible en su plan actual",
  "reason": "FEATURE_NOT_AVAILABLE"
}
```

---

### Test 2: Expired Subscription

Manually expire a subscription:

```sql
UPDATE company_subscription
SET expires_at = '2024-01-01'::timestamp
WHERE id_company = 1;
```

Try to create invoice:

```bash
POST /api/invoice
# Response: 403 FORBIDDEN
{
  "message": "La suscripción ha expirado"
}
```

---

### Test 3: Multiple Companies

Test with different companies on different plans:

```sql
-- Company 1: FREE (50 docs)
-- Company 2: BASIC (500 docs)
-- Company 3: PRO (2000 docs)

-- Each should have independent limits and counters
```

---

### Test 4: Monthly History

Create invoices across multiple months:

```sql
-- Insert historical data
INSERT INTO subscription_usage_history (
  id_company, id_subscription, month_year,
  invoices_count, credit_notes_count, total_documents
) VALUES
(1, 1, '2025-11', 45, 5, 50),
(1, 1, '2025-10', 40, 8, 48),
(1, 1, '2025-09', 38, 7, 45);
```

Get usage:

```bash
GET /api/subscription/1/usage
# Should show last 12 months in lastMonths array
```

---

## 📊 Database Queries for Verification

### Check all subscriptions by plan

```sql
SELECT
  sp.code,
  sp.name,
  COUNT(cs.id_subscription) as total_subscriptions,
  SUM(cs.current_year_usage) as total_documents_used
FROM subscription_plan sp
LEFT JOIN company_subscription cs ON sp.id_plan = cs.id_plan AND cs.status = 'ACTIVE'
GROUP BY sp.code, sp.name
ORDER BY sp.display_order;
```

### Check top users by document count

```sql
SELECT
  cs.id_company,
  sp.name as plan_name,
  cs.current_year_usage,
  sp.document_limit_yearly as limit,
  ROUND((cs.current_year_usage::numeric / sp.document_limit_yearly * 100), 2) as percent_used
FROM company_subscription cs
JOIN subscription_plan sp ON cs.id_plan = sp.id_plan
WHERE cs.status = 'ACTIVE'
ORDER BY cs.current_year_usage DESC
LIMIT 10;
```

### Check companies near limit

```sql
SELECT
  cs.id_company,
  cs.current_year_usage,
  sp.document_limit_yearly,
  sp.document_limit_yearly - cs.current_year_usage as remaining
FROM company_subscription cs
JOIN subscription_plan sp ON cs.id_plan = sp.id_plan
WHERE cs.status = 'ACTIVE'
  AND cs.current_year_usage >= (sp.document_limit_yearly * 0.8)
ORDER BY remaining ASC;
```

---

## 🐛 Troubleshooting

### Issue: Trigger not firing

**Check:**

```sql
-- Verify trigger exists
SELECT * FROM pg_trigger WHERE tgname LIKE '%usage%';

-- Test manually
SELECT increment_document_usage();
```

### Issue: Usage not increasing

**Verify:**

1. Invoice has `id_authorization_sri` set (not NULL)
2. Trigger is enabled
3. Company has active subscription

### Issue: Validation not working

**Check:**

1. `ISubscriptionService` is injected in service
2. `validateCanEmit()` is called before saving
3. No exceptions are being swallowed

---

## ✅ Success Criteria

Your implementation is working if:

1. ✅ All endpoints return expected responses
2. ✅ Invoices/credit notes validate subscription before creation
3. ✅ Usage counters increase automatically when document is authorized
4. ✅ Super admin can bypass all limits
5. ✅ Regular users are blocked at limit
6. ✅ Plan upgrades work correctly
7. ✅ Historical usage is tracked accurately

---

## 🎉 Next Steps

Once all tests pass:

1. ✅ Test in staging environment
2. ✅ Load test with multiple concurrent requests
3. ✅ Test subscription expiration workflow
4. ✅ Test year-end reset (manual simulation)
5. ✅ Document integration with ebill-customer
6. ✅ Deploy to production

---

**Happy Testing! 🚀**

If any test fails, check:

1. Database migrations executed successfully
2. Application restarted after code changes
3. Correct JWT token with proper roles
4. Company exists and has active subscription
5. Logs for detailed error messages
