# Credential Exposure Policy

Users must not submit application credentials, tokens, or secrets in chat messages or code attachments.

## Hard-blocked input

The assistant rejects requests when it detects:

- `property=value` or `property: value` assignments for sensitive keys (password, token, secret, api key, client secret, and similar)
- JWT bearer tokens (`eyJ...`)
- Secrets inside attached `.env`, `.properties`, YAML, or zip archives

Placeholder values such as `${ENV_VAR}` are allowed. Benign configuration such as host ports and `max-tokens` limits are not treated as secrets.
