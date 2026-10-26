# PostgreSQL in Docker for the shop

The tests and the normal run use the in-memory H2 database, so you need Docker **only** for the `prod` profile.

| Setting | Value |
|---|---|
| Image | `postgres:16` |
| Container name | `shop-db` |
| Port on your Mac | `5433` (inside the container Postgres uses 5432) |
| Database | `shop` |
| User | `postgres` |
| Password | your choice, kept only in `shop/.env` |

## 1. Install Docker

Install **Docker Desktop** from https://www.docker.com/products/docker-desktop/ and start it. Check that it works:

```
docker --version
docker ps
```

`docker ps` must print a table header (maybe empty) and not an error.

## 2. Create the `.env` file

```
cd shop
cp .env.example .env
```

Open `shop/.env` and choose a password:

```
DB_USER=postgres
DB_PASSWORD=my-secret-password
```

`.env` is in `.gitignore`, so it is never committed. `.env.example` (with `change-me`) is the copy that goes to git.

## 3. Create the container

Use **one** of the two ways below, not both, because they use the same container name.

### Option A: `docker compose` (recommended)

`shop/docker-compose.yml` already describes the database. In the `shop` folder:

```
docker compose up -d
```

Docker Compose reads `DB_PASSWORD` from `.env` by itself. `-d` means "run in the background".

### Option B: one `docker run` command

```
cd shop
source .env
docker run -d --name shop-db \
  -e POSTGRES_DB=shop \
  -e POSTGRES_PASSWORD="$DB_PASSWORD" \
  -p 5433:5432 \
  -v shop-data:/var/lib/postgresql/data \
  postgres:16
```

What each part means:

- `-d`: run in the background
- `--name shop-db`: the container's name, used in all later commands
- `-e POSTGRES_DB=shop`: create a database called `shop` at first start
- `-e POSTGRES_PASSWORD=...`: the password of user `postgres`
- `-p 5433:5432`: port 5433 on your Mac goes to port 5432 in the container. 5433 avoids a clash with a Postgres already installed on the Mac.
- `-v shop-data:/var/lib/postgresql/data`: keep the data in a named volume, so it survives when the container is removed
- `postgres:16`: the image (downloaded automatically the first time)

## 4. Check that it is running

```
docker ps
```

```
CONTAINER ID   IMAGE         ...   STATUS         PORTS                    NAMES
2ca7384e3c5b   postgres:16   ...   Up 19 seconds  0.0.0.0:5433->5432/tcp   shop-db
```

```
docker exec shop-db psql -U postgres -l
```

This lists the databases, and `shop` must be one of them.

## 5. Start the shop on Postgres

```
cd shop
./mvnw -q -DskipTests package
java -jar target/shop-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Run it from the `shop` folder, because that is where the app looks for `.env`. At startup Spring creates the `product` table and inserts the 5 products. Starting again doesn't duplicate them.

```
docker exec shop-db psql -U postgres -d shop -c "SELECT * FROM product ORDER BY id;"
```

## 6. Look at the data with TablePlus or pgAdmin

New connection → **PostgreSQL**:

- Host `localhost`, port `5433`
- User `postgres`, password from `.env`
- Database `shop`

## Everyday commands

| What | Command |
|---|---|
| stop the database | `docker stop shop-db` (or `docker compose stop`) |
| start it again | `docker start shop-db` (or `docker compose start`) |
| see its log | `docker logs shop-db` |
| open a SQL prompt | `docker exec -it shop-db psql -U postgres -d shop` (exit with `\q`) |
| delete the container, keep data | `docker rm -f shop-db` (or `docker compose down`) |
| delete everything incl. data | `docker compose down -v` (or `docker rm -f shop-db && docker volume rm shop-data`) |

## Problems

- **`Conflict. The container name "/shop-db" is already in use`**: a container with that name exists already. Start it with `docker start shop-db`, or remove it with `docker rm -f shop-db` and create it again.
- **`port is already allocated`**: something else uses 5433. Pick another port (e.g. `-p 5434:5432`) and change the URL in `application-prod.properties`.
- **`password authentication failed for user "postgres"`**: the password in `.env` differs from the one the container was created with. Postgres sets the password only on the **first** start with an empty volume. Either put the original password in `.env`, or delete container and data (see above) and create them again.
- **`Connection refused`**: the container is not running (`docker ps`), or Docker Desktop is not started.
