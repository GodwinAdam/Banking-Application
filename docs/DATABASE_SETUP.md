# Database Setup

This project ships configured for **H2** (an embedded, zero-install
database) so you can clone and run it with nothing else installed. This
guide explains that default in detail, then walks through connecting the
exact same code to a **real MySQL or PostgreSQL database** — locally
installed or via Docker.

## How database selection works in this project

`src/main/resources/application.yml` has:

```yaml
spring:
  profiles:
    active: h2
```

Spring Boot loads `application.yml` first, then — based on
`spring.profiles.active` — also loads `application-<profile>.yml` **on
top of it**, overriding only the keys that file specifies. So:

- `active: h2` → only `application.yml` is used (its `spring.datasource.*`
  block already points at H2).
- `active: mysql` → `application.yml` + `application-mysql.yml` are
  merged, and the MySQL file's `spring.datasource.*` values win.
- `active: postgres` → same idea, with `application-postgres.yml`.

You can also override the active profile **without editing any file**, by
passing it on the command line:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
# or, running the built jar directly:
java -jar target/banking-application-1.0.0.jar --spring.profiles.active=mysql
```

This is the standard pattern used in real Spring Boot projects: dev
environments run one profile, CI another, production a third — all from
the identical compiled code.

## Option A: H2 (default, already working)

Nothing to install. The connection string in `application.yml` is:

```
jdbc:h2:mem:bankingdb;DB_CLOSE_DELAY=-1
```

- `mem:bankingdb` — an **in-memory** database. All data is lost when the
  app stops. This is intentional for a learning project: every run starts
  from a clean slate.
- `DB_CLOSE_DELAY=-1` — without this, H2 would destroy the in-memory
  database the instant its last open connection closes (which can happen
  transiently between requests), wiping your data mid-session. `-1` keeps
  it alive for the app's entire lifetime.

**Want your H2 data to survive an app restart?** Change the URL to a
file-based one instead of in-memory:

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/bankingdb
```

This writes actual `.mv.db` files under a `data/` folder next to your
project. Delete that folder any time to reset to a clean database.

**Browsing the H2 database in your browser:**

1. Start the app (`mvn spring-boot:run`).
2. Open http://localhost:8080/h2-console
3. Fill in:
   - **JDBC URL**: `jdbc:h2:mem:bankingdb` (must match `application.yml` exactly)
   - **User Name**: `sa`
   - **Password**: *(leave blank)*
4. Click **Connect**. You'll see `CUSTOMERS`, `ACCOUNTS`, `TRANSACTIONS`
   tables in the left sidebar and can run any SQL directly, e.g.:
   ```sql
   SELECT * FROM ACCOUNTS;
   SELECT * FROM TRANSACTIONS ORDER BY CREATED_AT DESC;
   ```

## Option B: MySQL (real, persistent database)

### B.1 — Get a MySQL server running

**Fastest: Docker (recommended if you have Docker installed)**

```bash
docker run --name banking-mysql \
  -e MYSQL_ROOT_PASSWORD=your_mysql_password_here \
  -e MYSQL_DATABASE=bankingdb \
  -p 3306:3306 \
  -d mysql:8.0
```

This single command downloads MySQL 8, starts it in the background, and
pre-creates a `bankingdb` database — no manual `CREATE DATABASE` needed.
Give it about 20-30 seconds to finish initializing before connecting.

**Alternative: install MySQL natively (macOS via Homebrew)**

```bash
brew install mysql
brew services start mysql
mysql -u root -e "CREATE DATABASE bankingdb;"
```

### B.2 — Point the app at it

Edit `src/main/resources/application-mysql.yml` and replace the
placeholder credentials with your real ones:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bankingdb?useSSL=false&serverTimezone=UTC&createDatabaseIfNotExist=true
    username: root
    password: your_mysql_password_here   # <-- change this
```

- `createDatabaseIfNotExist=true` means you technically don't even need
  step B.1's `CREATE DATABASE` — MySQL will create `bankingdb` on first
  connection if it doesn't exist (the Docker command above already
  created it via `MYSQL_DATABASE`, so this is just a safety net).
- `useSSL=false` — fine for local development; a real deployment would use
  a proper TLS connection instead.

### B.3 — Activate the profile and run

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Watch the startup logs — you should see Hibernate's generated `CREATE
TABLE` statements targeting MySQL syntax, and no H2-related log lines.
Connect with any MySQL client (or `mysql -u root -p bankingdb`) to confirm
the `customers`, `accounts`, and `transactions` tables now exist there
instead of in memory.

## Option C: PostgreSQL (real, persistent database)

### C.1 — Get a PostgreSQL server running

**Docker:**

```bash
docker run --name banking-postgres \
  -e POSTGRES_PASSWORD=your_postgres_password_here \
  -e POSTGRES_DB=bankingdb \
  -p 5432:5432 \
  -d postgres:16
```

**Native (macOS via Homebrew):**

```bash
brew install postgresql@16
brew services start postgresql@16
createdb bankingdb
```

### C.2 — Point the app at it

Edit `src/main/resources/application-postgres.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bankingdb
    username: postgres
    password: your_postgres_password_here   # <-- change this
```

### C.3 — Activate the profile and run

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Connecting to a remote/cloud database (AWS RDS, etc.)

The mechanism is identical — only the URL, username, and password change.
For example, an AWS RDS MySQL instance might look like:

```yaml
spring:
  datasource:
    url: jdbc:mysql://my-instance.abc123xyz.us-east-1.rds.amazonaws.com:3306/bankingdb?useSSL=true&serverTimezone=UTC
    username: admin
    password: ${DB_PASSWORD}   # see the note on secrets below
```

**Never commit real production credentials into `application-*.yml`.** For
anything beyond local learning, pull the password from an environment
variable instead — `${DB_PASSWORD}` above tells Spring Boot to read it
from the `DB_PASSWORD` environment variable at startup:

```bash
export DB_PASSWORD=the_real_password
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `Communications link failure` / `Connection refused` | The database server isn't running, or is listening on a different port than the URL specifies. |
| `Unknown database 'bankingdb'` | The database wasn't created — either add `createDatabaseIfNotExist=true` to the MySQL URL, or run `CREATE DATABASE bankingdb;` / `createdb bankingdb` manually. |
| `Access denied for user` | Username/password in `application-<profile>.yml` don't match the real server's credentials. |
| Tables aren't created | Confirm `spring.jpa.hibernate.ddl-auto` is `update` (or `create`) in the active profile, not `validate`/`none`. |
