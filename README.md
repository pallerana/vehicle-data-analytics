# UK Vehicle Data Analytics - Solera Interview Implementation

This repository contains an end-to-end interview implementation for Solera's practical exercise:

> Build an interactive website that analyzes UK vehicles currently on the road, with trend charts, year-on-year changes, filtering by powertrain/manufacturer/model, and an admin area to update data.

## Interview Question (Problem Statement)

Create an interactive website that provides analysis of trends for vehicles currently on the road in the UK:

- Show both **total number of vehicles** and **year-on-year (YoY) change**
- Provide analysis mainly through **graphs**, with supporting textual highlights
- Support filtering and comparison by:
  - powertrain/fuel (Petrol, Diesel, Electric, etc.)
  - manufacturer, model, and related dimensions
- Design for a **high number of visitors** (performance/scalability awareness)
- Make data updates easy through an **admin area**
- Keep the site clean, simple, and usable

Stretch options discussed:

- Comparable alternative vehicles across powertrains
- AI-generated summaries
- Statistical prediction

## Implemented Solution

### Tech Stack

- Backend: Spring Boot 3, Java 17, JDBC/JPA, Flyway, Actuator, OpenAPI, Lombok
- Frontend: React + TypeScript + Vite
- Database: SQL Server
- Data source: UK vehicle quarterly CSV (`df_VEH0120_GB.csv`)

### Architecture

#### Backend Layers

- `controller`: API endpoints for analytics, admin, and users
- `service`: orchestration, caching, transformation, insights generation
- `repository`: SQL generation and query execution against `vehicles_data`
- `dto`: API contracts for trends, points, options, insights, admin status/import responses

#### Frontend Structure

- Single-page dashboard with two tabs:
  - `Analytics`: filters, trend line graph, YoY bars, textual highlights
  - `Admin`: CSV upload/import, cache refresh, data status summary

### Key Design Principles Used

- **Single Responsibility**: dedicated services/repositories/controllers
- **Open/Closed + Strategy**: `GroupByStrategy` for pluggable grouping dimensions
- **Separation of Concerns**: query construction isolated from API formatting
- **Defensive input handling**: validated year ranges, safe filter mapping, upload validation
- **Operational readiness**: actuator health endpoint, SQL logging for generated analytics queries

## Features Delivered

### Public Analytics

- Trend API for yearly totals with YoY change and YoY percent
- Filter options API for fuel, make, model
- Highlights API with textual summaries
- Grouping support:
  - `fuel`, `make`, `genModel`, `model`, `bodyType`, `licenceStatus`, `total`

### Admin Capabilities

- Data status API (`total rows`, `distinct makes`, year coverage)
- Cache refresh API
- CSV import API with multipart upload:
  - Validates CSV file type/content
  - Rebuilds `vehicles_data` schema from header
  - Batch inserts rows efficiently
  - Refreshes analytics metadata/cache automatically

### User CRUD (Foundational API Slice)

- User entity/repository/service/controller with:
  - list API (paging/sorting)
  - get, create, update, delete

## API Reference (Core)

### Analytics

- `GET /api/v1/analytics/trends`
- `GET /api/v1/analytics/highlights`
- `GET /api/v1/analytics/options`

### Admin

- `GET /api/v1/admin/data/status`
- `POST /api/v1/admin/data/refresh`
- `POST /api/v1/admin/data/import-csv` (multipart form-data, field: `file`)

### Health

- `GET /actuator/health`

Swagger:

- `http://localhost:8080/swagger-ui/index.html`

## Local Run

### 1) Start SQL Server (Docker)

```bash
docker compose up -d sqlserver
```

### 2) Backend

```bash
cd backend
./gradlew bootRun
```

### 3) Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend URL:

- `http://localhost:5173`

## Data Import Workflow (Quarterly Update)

1. Open Admin tab in UI
2. Choose new quarterly CSV file
3. Click **Import CSV**
4. Wait for completion message
5. Verify status panel (rows, year range)

The import process updates `vehicles_data` and refreshes analytics caches/metadata.

## Performance & Scalability Considerations

- Cached analytics responses for repeated heavy trend requests
- Metadata cache for quarter-column discovery
- Batch database inserts for CSV import
- Server-side aggregation and filtering to avoid loading raw data into client

## Notable Compromises (Time-Boxed Interview Context)

- No authentication/authorization around admin endpoints yet
- No automated tests included in this pass (intentionally deferred for speed)
- Basic SVG charts implemented without external charting library
- CSV import is synchronous in-request (could be moved to background job for larger files)

## Recommended Next Improvements

- Add role-based access control for admin routes
- Add async import job tracking with progress and retries
- Add integration tests for analytics SQL generation and CSV import
- Add API pagination/top-N endpoints for very high-cardinality grouping
- Implement stretch goals (comparable alternatives, AI insights, forecasting)
