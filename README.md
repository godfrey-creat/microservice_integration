# Country Info Service

A Spring Boot microservice that receives a country name over REST, looks it up in the public
**CountryInfoService SOAP API**, stores the result in **MySQL**, and provides REST endpoints to
manage the stored data. It is packaged with Docker and deployed to **Kubernetes**.

- **Deployment guide:** [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)
- **Troubleshooting guide:** [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)
- **SoapUI project and screenshots:** [soapui/](soapui/) and [docs/screenshots/](docs/screenshots/)

**Tech stack:** Java 17, Spring Boot 4.1.1 (Web, Data JPA, Validation, Actuator), MySQL 8, Flyway,
Resilience4j, Caffeine, Docker, Kubernetes (minikube).

---

## How it works

```
POST { "name": "kenya" }
        │
        ▼
1. Convert to sentence case            "kenya" → "Kenya"
2. SOAP CountryISOCode("Kenya")        → "KE"
3. SOAP FullCountryInfo("KE")          → capital, phone code, currency, flag, languages
4. Save to MySQL                       → country_info + country_language tables
5. Return the stored country           → 201 Created
```

## Assessment steps

| Step | What was done | Where |
|---|---|---|
| 1 | Spring Boot project with Spring Web, Spring Data JPA, MySQL Driver | `pom.xml` |
| 2 | SoapUI project importing the WSDL; tested CountryISOCode and FullCountryInfo | `soapui/`, `docs/screenshots/` |
| 3 | POST endpoint receiving `{ "name": "..." }`, converted to sentence case | `controller/CountryInfoController`, `util/TextUtils` |
| 4 | SOAP call to `CountryISOCode` to get the ISO code | `soap/CountryInfoSoapClient` |
| 5 | SOAP call to `FullCountryInfo` with the ISO code | `soap/CountryInfoSoapClient` |
| 6 | `CountryInfo` and `Language` models, saved to MySQL | `model/`, `db/migration/V1__create_country_tables.sql` |
| 7 | Fetch all, fetch by id, update and delete endpoints | `controller/CountryInfoController` |
| 8 | Dockerfile and Kubernetes deployment scripts | `Dockerfile`, `k8s/`, `scripts/` |
| 9 | Deployment guide | `docs/DEPLOYMENT.md` |
| 10 | Troubleshooting guide | `docs/TROUBLESHOOTING.md` |

## API

Base path: `/api/v1/countries`

| Method | Path | Description | Success | Errors |
|---|---|---|---|---|
| POST | `/` | Register a country by name | 201 new / 200 updated | 400, 404, 502, 503 |
| GET | `/?page=0&size=20` | List all countries (paged, max 100 per page) | 200 | 400 |
| GET | `/{id}` | Get one country | 200 | 400, 404 |
| PUT | `/{id}` | Update a country | 200 | 400, 404, 409 |
| DELETE | `/{id}` | Delete a country | 204 | 404 |

Example:

```bash
curl -X POST localhost:8080/api/v1/countries \
  -H 'Content-Type: application/json' -d '{"name":"kenya"}'
```

```json
{
  "id": 1,
  "isoCode": "KE",
  "name": "Kenya",
  "capitalCity": "Nairobi",
  "phoneCode": "254",
  "continentCode": "AF",
  "currencyIsoCode": "KES",
  "countryFlag": "http://www.oorsprong.org/WebSamples.CountryInfo/Flags/Kenya.jpg",
  "languages": [{ "isoCode": "swa", "name": "Swahili" }],
  "createdAt": "2026-10-08T07:30:00Z",
  "updatedAt": "2026-10-08T07:30:00Z"
}
```

All errors return the same format, including a `correlationId` that matches the application logs:

```json
{
  "timestamp": "2026-10-08T07:31:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "No country found with the name 'Atlantis'",
  "path": "/api/v1/countries",
  "correlationId": "5b0c1f2e-...",
  "fieldErrors": null
}
```

## Design decisions

**Layered structure.** The controller handles HTTP only, the service holds the business logic,
the SOAP client handles the integration, and the repository handles the database. API responses
use DTOs, so database entities are never exposed directly.

**Case-sensitive SOAP lookup.** Testing showed the SOAP service only matches exact names
(`South Africa` works, `South africa` does not). The app uses sentence case first, as required,
then retries once in title case. This fixes multi-word countries without breaking single-word ones.

**Handling SOAP failures.**
- Timeouts: 3 s to connect, 5 s to read.
- Retry: up to 3 attempts with increasing wait times.
- Circuit breaker: after repeated failures, calls stop for 30 s instead of piling up.
- Fallback: if the SOAP service is down, a previously stored copy of the country is returned
  (`X-Data-Source: DATABASE_FALLBACK`). If there is none, the API returns `503` with `Retry-After`.
- "Country not found" is treated as a normal answer: it is not retried and does not trip the breaker.

**Short database transactions.** SOAP calls happen outside the database transaction. Only the
final save runs in a transaction, so a slow SOAP service never holds a database connection.

**No duplicates.** The ISO code is unique. Registering the same country twice updates the existing
row (`200`) instead of creating a new one (`201`).

**Scalability.** The service keeps no state, so Kubernetes can run 2 to 10 copies behind a load
balancer (HPA). SOAP results are cached for 6 hours. Lists are paginated.

**Observability.**
- Health checks for Kubernetes: `/actuator/health/liveness` and `/actuator/health/readiness`.
- Metrics for Prometheus: `/actuator/prometheus`, including SOAP call latency.
- Every request gets a correlation ID that appears in every log line and error response.
- Logs are JSON in production (`prod` profile), readable text locally.

**Database schema.** Managed by Flyway migrations, so schema changes are versioned and repeatable.

## Run locally

Requirements: Java 17 and Docker.

```bash
# 1. Start MySQL
docker run -d --name country-mysql -p 3306:3306 \
  -e MYSQL_DATABASE=countrydb -e MYSQL_USER=country_user \
  -e MYSQL_PASSWORD=country_pass -e MYSQL_ROOT_PASSWORD=root_pass \
  mysql:8.0

# 2. Run the app
./mvnw spring-boot:run
```

The app runs on http://localhost:8080. Flyway creates the tables on first start.

Alternatively, run the packaged image together with its own MySQL:

```bash
docker compose up --build
```

## Run the tests

```bash
./mvnw clean test
```

The tests do not need MySQL or internet access. They use an in-memory H2 database and a mocked SOAP client.

- **Unit tests:** sentence and title case conversion, SOAP XML parsing (including SOAP faults and XXE protection).
- **Integration tests:** the full create, read, update, delete flow over HTTP, plus 400, 404, 503, the database fallback and correlation IDs.

## Deploy to Kubernetes

```bash
minikube start --driver=docker --cpus=2 --memory=4g
minikube addons enable metrics-server
minikube addons enable ingress
./scripts/deploy.sh minikube
```

Full instructions: [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md)

## Project structure

```
src/main/java/com/ncba/countryinfo
├── config/        HTTP client, cache, correlation ID filter
├── controller/    REST endpoints
├── dto/           Request and response objects
├── exception/     Exceptions and global error handler
├── model/         CountryInfo and Language entities
├── repository/    Database access (Spring Data JPA)
├── service/       Business logic
├── soap/          SOAP client, request builder, response parser
└── util/          Sentence case and title case conversion
src/main/resources
├── application.properties
├── application-prod.properties
└── db/migration/  Flyway SQL migrations
k8s/               Kubernetes manifests
scripts/           deploy.sh, teardown.sh
docs/              Deployment and troubleshooting guides, screenshots
soapui/            SoapUI project
Dockerfile
docker-compose.yml
```