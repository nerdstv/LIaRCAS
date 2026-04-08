# LIaRCAS

Distributed Log Ingestion and Root Cause Analysis System

## Goal

Build a basic but complete distributed logging product that covers these points without unnecessary complexity:

- Java with Spring Boot for backend services
- Kafka for distributed log ingestion and asynchronous communication
- Producer-consumer architecture for decoupled services
- Elasticsearch for real-time indexing and search
- Docker for containerization
- Kubernetes with Minikube for local deployment

This repository will be built step by step. Each step should be small, clear, and ready to push independently.

## MVP Scope

The first version will focus on a minimal working system:

- A service receives logs over HTTP
- The service publishes logs to Kafka
- A consumer service reads logs from Kafka
- The consumer indexes logs into Elasticsearch
- A simple analysis API provides basic root cause hints by grouping frequent errors
- All services run in Docker
- The stack is deployable on Minikube

This keeps the system simple while still demonstrating the full distributed pipeline.

## Minimal Architecture

```text
Client / Sample Log Generator
	|
	v
log-ingestion-service (Spring Boot REST API)
	|
	v
Kafka topic: raw-logs
	|
	v
log-processing-service (Spring Boot Kafka consumer)
	|
	v
Elasticsearch
	|
	v
root-cause-service (Spring Boot REST API)
```

## Planned Services

### 1. log-ingestion-service

- Accepts logs from clients using a REST endpoint
- Validates the request
- Publishes messages to Kafka

### 2. log-processing-service

- Consumes logs from Kafka
- Performs basic normalization
- Stores logs in Elasticsearch

### 3. root-cause-service

- Queries Elasticsearch
- Groups repeated failures by service, level, and message pattern
- Returns a simple root cause summary

## Suggested Tech Choices

- Java 21
- Spring Boot 3.x
- Maven
- Spring Web
- Spring for Apache Kafka
- Spring Data Elasticsearch or Elasticsearch Java Client
- Docker and Docker Compose for local bring-up
- Kubernetes manifests for Minikube deployment

## Delivery Plan

### Phase 1: Repository setup

- Define project scope
- Define minimal architecture
- Prepare README and initial structure

### Phase 2: Spring Boot project scaffold

- Create the parent Maven project
- Create the three services
- Add common configuration and shared model package if needed

### Phase 3: Kafka pipeline

- Add Kafka producer to ingestion service
- Add Kafka consumer to processing service
- Verify end-to-end message flow

### Phase 4: Elasticsearch integration

- Start Elasticsearch locally
- Index processed logs
- Add simple query support

### Phase 5: Root cause analysis API

- Implement simple aggregation-based analysis
- Return top error groups and likely root cause candidates

### Phase 6: Containerization

- Dockerize all services
- Add local orchestration for development

### Phase 7: Kubernetes deployment

- Add Minikube-ready manifests
- Deploy services and infrastructure locally

### Phase 8: Validation and documentation

- Add sample requests
- Add test flow
- Finalize setup and run instructions

## Expected Repository Structure

```text
LIaRCAS/
├── README.md
├── .gitignore
├── pom.xml
├── common-models/
├── log-ingestion-service/
├── log-processing-service/
├── root-cause-service/
├── infrastructure/
│   ├── docker/
│   └── k8s/
└── docs/
```

## First Milestone

The first code milestone after this setup step will be:

- create the multi-module Maven project
- add the three Spring Boot services
- make all services start successfully

## Notes

- The root cause analysis in the MVP will be rule-based, not AI-based
- The local environment can start with a single Kafka broker and single Elasticsearch node
- The design should stay simple first, then improve incrementally
