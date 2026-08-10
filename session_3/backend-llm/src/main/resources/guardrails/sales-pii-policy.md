# Sales Customer PII Policy

Sales customer personally identifiable information (PII) in `getSales` responses is role-gated at the Sales API.

## Access levels

- **`sales-admin`**: receives full customer names and phone numbers.
- **`sales-user`**: must call `getSales` with `redactCustomerPii=true`; customer names and phones are masked with asterisks (`*`).

## Assistant behavior

- Use `getSales` for transaction-level sales data; do not query PostgreSQL for customer sales records.
- Never attempt to unredact, reconstruct, guess, or infer masked customer PII for users who are not `sales-admin`.
- When reporting `getSales` results for `sales-user` accounts, present redacted values exactly as returned by the tool.
- Refuse requests to bypass, decode, or remove redaction on customer names or phone numbers.

## Violations

Attempts to unredact sales customer PII without `sales-admin` role are **hard policy violations**.
