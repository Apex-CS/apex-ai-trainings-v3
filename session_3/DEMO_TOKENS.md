# Demo JWT Bearer Tokens

These prefabricated HS256 JWT tokens are signed with the demo secret configured in
`application.yml` (`app.jwt.secret`). Pass any token in the `Authorization` header:

```
Authorization: Bearer <token>
```

Tokens expire in 2031. Each token includes `sub`, `name`, and `roles` claims.

| User | Username (`sub`) | Roles | Financial API access | IT API access | Sales API access |
|------|------------------|-------|----------------------|---------------|------------------|
| fulano smith | `fulano.smith` | financial-admin, it-user, marketing-user, sales-user | GET + UPDATE | list servers | products + sales (PII redacted) |
| sutano doe | `sutano.doe` | sales-admin, financial-user, it-user, marketing-user | GET only | list servers | full sales access (full PII) |
| mengana davidson | `mengana.davidson` | marketing-admin, financial-user, it-user, sales-user | GET only | list servers | products + sales (PII redacted) |
| bart perez | `bart.perez` | it-admin, financial-user, marketing-user, sales-user | GET only | full IT access | products + sales (PII redacted) |

## Tokens

### fulano smith (financial-admin)

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJmdWxhbm8uc21pdGgiLCJuYW1lIjoiZnVsYW5vIHNtaXRoIiwicm9sZXMiOlsiZmluYW5jaWFsLWFkbWluIiwiaXQtdXNlciIsIm1hcmtldGluZy11c2VyIiwic2FsZXMtdXNlciJdLCJpYXQiOjE3ODYzODE2MjEsImV4cCI6MTk0NDA2MTYyMX0.ZHmS9_kmfuv-E1xiQRrFBzs7kYJ_K2vS40ySYEVI8mk
```

### sutano doe (financial-user)

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzdXRhbm8uZG9lIiwibmFtZSI6InN1dGFubyBkb2UiLCJyb2xlcyI6WyJzYWxlcy1hZG1pbiIsImZpbmFuY2lhbC11c2VyIiwiaXQtdXNlciIsIm1hcmtldGluZy11c2VyIl0sImlhdCI6MTc4NjM4MTYyMSwiZXhwIjoxOTQ0MDYxNjIxfQ.lugwdYSFvfihB9Cw1b6XIDJp5W2TwGS0edkORhw2IDQ
```

### mengana davidson (financial-user)

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJtZW5nYW5hLmRhdmlkc29uIiwibmFtZSI6Im1lbmdhbmEgZGF2aWRzb24iLCJyb2xlcyI6WyJtYXJrZXRpbmctYWRtaW4iLCJmaW5hbmNpYWwtdXNlciIsIml0LXVzZXIiLCJzYWxlcy11c2VyIl0sImlhdCI6MTc4NjM4MTYyMSwiZXhwIjoxOTQ0MDYxNjIxfQ.k-merM-rzHfBpwN-XSAfs5_HSxiMI6QHZCLSyM6o54Y
```

### bart perez (financial-user)

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJiYXJ0LnBlcmV6IiwibmFtZSI6ImJhcnQgcGVyZXoiLCJyb2xlcyI6WyJpdC1hZG1pbiIsImZpbmFuY2lhbC11c2VyIiwibWFya2V0aW5nLXVzZXIiLCJzYWxlcy11c2VyIl0sImlhdCI6MTc4NjM4MTYyMSwiZXhwIjoxOTQ0MDYxNjIxfQ.Ft8SjODLwAAZt-XprCeT-rl-Ng6LMLl4MRZvP3ni6Nw
```

## Example requests

```bash
# GET budgets for IT (financial-user or financial-admin)
curl -s "http://localhost:8091/api/get-budget-by-area?area=IT&fiscalYear=2026" \
  -H "Authorization: Bearer <token>"

# UPDATE budget (financial-admin only)
curl -s -X PUT "http://localhost:8091/api/update-budget-by-area" \
  -H "Authorization: Bearer <fulano-token>" \
  -H "Content-Type: application/json" \
  -d '{"area":"IT","fiscalQuarter":1,"fiscalYear":2026,"budget":375000.00}'
```

## Error scenarios

| Scenario | HTTP status | Example |
|----------|-------------|---------|
| Missing token | 401 | No `Authorization` header |
| Invalid token | 401 | `Authorization: Bearer invalid-token` |
| Wrong role for UPDATE | 403 | sutano doe token on PUT endpoint |
