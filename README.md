# LIaRCAS

LIaRCAS stands for Log Ingestion and Root Cause Analysis System.

It is a backend platform for collecting structured logs from distributed services, enriching them with tenant context, streaming them through Kafka, and storing them in Elasticsearch so they can later be queried and analyzed.

The long-term goal is to add an AI-assisted Root Cause Analysis layer using LangGraph. That RCA layer is planned, but not implemented yet. The current repository already provides the ingestion and processing pipeline that RCA will build on.

## What This Project Is

LIaRCAS is not a website or an end-user product UI.

It is an infrastructure-style backend system that other services talk to over HTTP.

Typical usage looks like this:

1. A microservice sends a structured error log to LIaRCAS.
2. LIaRCAS authenticates the caller with an API key.
3. LIaRCAS resolves the tenant from that API key.
4. The log is published to Kafka.
5. A processing service consumes the log and stores it in Elasticsearch.
6. A future RCA service will analyze stored logs and return likely root causes.

## Current Status

Implemented today:

- Multi-module Maven project with shared models
- Log ingestion service built with Spring Boot
- API-key based authentication on the ingestion endpoint
- Tenant resolution from API key
- Kafka-based decoupling between ingestion and processing
- Log processing service that persists events to Elasticsearch
- Docker Compose setup for local bring-up
- End-to-end smoke test script with Docker
- Unit and integration tests run in CI

Planned next:

- Request validation hardening
- Tenant-scoped storage and query APIs
- Actuator and metrics
- Environment profiles and stronger production configuration
- Error handling and resilience improvements
- Security hardening for production deployments
- LangGraph-based RCA engine
- Dashboard UI for incident and RCA viewing

## Architecture

```text
Client Service / Log Appender
				|
				v
log-ingestion-service
	- REST API
	- API key authentication
	- tenant resolution
	- message enrichment
				|
				v
Kafka topic: raw-logs
				|
				v
log-processing-service
	- Kafka consumer
	- transforms LogEvent -> LogEventDocument
	- persists to Elasticsearch
				|
				v
Elasticsearch
				|
				v
root-cause-service
	- currently scaffolded only
	- planned RCA API layer
	- planned LangGraph integration
```

## Repository Structure

```text
LIaRCAS/
├── common-models/
├── log-ingestion-service/
├── log-processing-service/
├── root-cause-service/
├── .github/workflows/
├── docker-compose.yaml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── runThis.ps1
```

## Modules

### common-models

Shared Java classes used across services.

Current key model:

- `LogEvent`: the structured log payload that moves from ingestion to Kafka to processing

### log-ingestion-service

Responsibilities:

- Exposes `POST /logs`
- Requires an API key header
- Resolves the tenant from the configured API key mapping
- Prevents clients from spoofing tenantId
- Generates `id` and `timestamp` if the client does not provide them
- Publishes the final log event to Kafka topic `raw-logs`

### log-processing-service

Responsibilities:

- Consumes messages from Kafka topic `raw-logs`
- Maps incoming `LogEvent` objects into Elasticsearch documents
- Persists them to Elasticsearch

Current note:

- Logs are currently stored in a shared `logs` index
- Tenant-scoped indices are planned as the next hardening step

### root-cause-service

Responsibilities today:

- Minimal Spring Boot service scaffold
- Health endpoint only

Planned responsibilities:

- Query stored logs from Elasticsearch
- Run RCA workflows
- Expose RCA APIs such as analysis trigger and report retrieval
- Integrate with a LangGraph-based AI agent workflow

## Technology Stack

- Java 21
- Spring Boot 3.4.1
- Maven multi-module build
- Spring Web
- Spring Security
- Spring for Apache Kafka
- Spring Data Elasticsearch
- Kafka
- Elasticsearch
- Docker and Docker Compose
- GitHub Actions for CI

Planned for RCA:

- Python service for LangGraph workflows
- LLM-backed hypothesis generation
- anomaly detection and temporal correlation steps
- optional dashboard frontend

## Authentication Model

The ingestion endpoint is protected with an API key.

Current behavior:

- Client sends `X-API-Key`
- LIaRCAS matches the key against configured clients
- A tenant is resolved from that client configuration
- The server overwrites any tenantId sent by the caller

This ensures the caller cannot submit logs on behalf of another tenant simply by changing the request body.

Example auth configuration:

```yaml
liarcas:
	auth:
		header-name: X-API-Key
		clients:
			- client-id: local-dev-client
				api-key: local-dev-api-key
				tenant-id: tenant-001
```

## Log Event Shape

Current fields supported by `LogEvent`:

- `id`
- `tenantId`
- `serviceName`
- `component`
- `environment`
- `serviceVersion`
- `instanceId`
- `traceId`
- `level`
- `message`
- `exceptionType`
- `stackTraceHash`
- `timestamp`

Example request:

```json
{
	"serviceName": "payment-service",
	"component": "db-client",
	"environment": "prod",
	"serviceVersion": "1.4.2",
	"instanceId": "payment-pod-7",
	"traceId": "trace-abc-123",
	"level": "ERROR",
	"message": "Database timeout",
	"exceptionType": "SQLTransientConnectionException",
	"stackTraceHash": "sth-9f8c2d"
}
```

Server-managed behavior:

- `tenantId` is resolved from the API key
- `id` is generated if omitted
- `timestamp` is generated if omitted

## Local Development

### Prerequisites

For the easiest local experience:

- Git
- Docker Desktop or another Docker runtime with Compose support

If you want to build and test outside Docker as well:

- Java 21

## Running the Stack

### Option 1: Recommended local bring-up

On Windows PowerShell:

```powershell
.\runThis.ps1
```

What this script does:

- builds Docker images
- starts Kafka, Elasticsearch, and the application services
- waits for the services to become reachable
- posts a sample log through the ingestion API
- verifies the log was stored in Elasticsearch

### Option 2: Start with Docker Compose directly

```bash
docker compose up -d --build
```

Current containers included in Compose:

- Zookeeper
- Kafka
- Elasticsearch
- Kafka topic initialization helper
- log-ingestion-service
- log-processing-service

## Local Endpoints

When the Docker Compose stack is running locally:

- Ingestion API: `http://localhost:8081/logs`
- Ingestion health: `http://localhost:8081/health`
- Processing health: `http://localhost:8082/health`
- Elasticsearch: `http://localhost:9200`
- Kafka external listener: `localhost:9092`

Note:

- The root-cause service is not currently part of the Docker Compose stack
- The current compose setup focuses on the ingestion and processing path

If you start the root-cause service separately, its default health endpoint is:

- Root cause service health: `http://localhost:8083/health`

## Example API Call

```bash
curl -X POST http://localhost:8081/logs \
	-H "X-API-Key: local-dev-api-key" \
	-H "Content-Type: application/json" \
	-d '{
		"serviceName": "payment-service",
		"component": "db-client",
		"environment": "prod",
		"serviceVersion": "1.4.2",
		"instanceId": "payment-pod-7",
		"traceId": "trace-abc-123",
		"level": "ERROR",
		"message": "Database timeout",
		"exceptionType": "SQLTransientConnectionException",
		"stackTraceHash": "sth-9f8c2d"
	}'
```

Expected behavior:

- request is authenticated using the API key
- tenantId is set by the server
- event is published to Kafka
- processing service stores the document in Elasticsearch

## Build and Test

Run the full Maven verification build:

On Linux or macOS:

```bash
./mvnw -B verify
```

On Windows PowerShell:

```powershell
.\mvnw.cmd -B verify
```

Current CI behavior:

- GitHub Actions runs the Maven wrapper
- build includes unit tests and integration tests

## Current Limitations

These are known and expected at the current stage of the project:

- no dashboard UI yet
- root-cause-service is scaffolded but not functionally implemented
- request validation still needs hardening
- logs are stored in a shared Elasticsearch index
- local Docker setup is development-oriented, not production-secure
- health endpoints are custom and will later move to actuator-based health checks

## RCA Vision

The long-term direction is for LIaRCAS to evolve from a logging pipeline into an AI-assisted RCA platform.

Planned RCA capabilities:

- retrieve incident logs for a time window and tenant
- cluster repeated errors and extract patterns
- detect anomalies in error rates and service behavior
- correlate failures across services over time
- generate ranked root-cause hypotheses
- expose RCA reports through an API
- support a future dashboard for incidents and RCA visualization

Recommended implementation direction:

- keep ingestion and processing in Java/Spring Boot
- build the RCA engine as a dedicated Python service using LangGraph
- use the existing root-cause-service as the API-facing boundary or orchestration layer

## Who This Project Is For

LIaRCAS is useful as:

- a portfolio project showing distributed systems design
- a practical example of Kafka plus Elasticsearch integration
- a base platform for multi-tenant log ingestion
- a future showcase for AI-agent driven root cause analysis
- a starting point for an internal engineering observability product

## Roadmap Summary

Short-term priorities:

- request validation and input safety
- tenant-scoped storage
- actuator and metrics
- configuration profiles
- resilience and DLQ handling
- production security hardening

Medium-term priorities:

- RCA request and report model
- Elasticsearch-backed RCA query flow
- LangGraph orchestration
- anomaly detection and evidence ranking

Long-term priorities:

- dashboard UI
- feedback loop for RCA quality improvement
- production deployment patterns such as Kubernetes and ingress

## Project Direction

The current repository already demonstrates the critical distributed pipeline:

- authenticated log ingestion
- tenant-aware enrichment
- asynchronous event streaming with Kafka
- persistence in Elasticsearch

The next stage is to build the RCA capability on top of this stable foundation.
