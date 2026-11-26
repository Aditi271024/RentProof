# Security operations

## Landlord and demo account provisioning

Create landlord accounts with the registration page or `POST /api/auth/register`. Choose a unique password locally; account passwords are stored using PBKDF2 and are not included in frontend files or SQL seeds.

Start `RentProofServer` to run `DatabaseInitializer`, which creates missing tables and applies additive tenant rent/address migrations while preserving existing rows. The `database/rentproof.sql` schema script no longer drops tables, but it is not a substitute for application migrations. `database/rentproof-sample-data.sql` uses fixed IDs and should only be used with a verified disposable database; never run it against a database containing user records.

## Database credentials

Use a dedicated least-privilege MySQL account, not `root`. Set `RENTPROOF_DB_USER` and `RENTPROOF_DB_PASSWORD` in the process environment or provide a protected, untracked `db.properties` file. Do not put live credentials in Git, source files, command history, or frontend assets.

Rotate any credential that was previously exposed using an authorized MySQL administrator account. For example, in an interactive MySQL session:

```sql
ALTER USER 'rentproof_app'@'localhost' IDENTIFIED BY '<new-unique-secret>';
FLUSH PRIVILEGES;
```

Update the protected runtime secret at the same time and restart the backend. Do not reuse the previous password. The application cannot rotate the MySQL account itself.

For deployments behind a TLS-terminating reverse proxy, set `RENTPROOF_HTTPS=true` so session and CSRF cookies include `Secure`, and set `RENTPROOF_ALLOWED_ORIGINS` to a comma-separated list of exact trusted origins. Do not enable the HTTPS flag unless the browser-facing connection is HTTPS.

## Sessions, CSRF, and static files

Sessions are held in memory and expire after 30 minutes of inactivity; restarting the server invalidates active sessions. The login page's “Remember me” control does not extend the session. State-changing API calls require the CSRF cookie/header pair; the frontend refreshes its token before expiry.

The HTTP server serves only `index.html` and files under `frontend/`. Keep credentials and backend configuration outside the frontend tree; the backend source, database scripts, and runtime configuration must not be exposed as static assets.

## Database backup

The tenant fix adds nullable `tenants.monthly_rent` and `tenants.permanent_address` columns and preserves existing rows. Before any future schema migration, create and verify a backup using the deployment's protected MySQL credentials. Example from PowerShell:

```powershell
New-Item -ItemType Directory -Force database\backups
mysqldump --single-transaction --routines --triggers -u $env:RENTPROOF_DB_USER -p rentproof > database\backups\rentproof-pre-migration.sql
```

`mysqldump` prompts for the password; do not put it on the command line. Verify the backup file is non-empty and restorable before applying a migration. Backups under `database/backups/` are excluded from Git.
