$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Wait-Http {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 120
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)

    while ((Get-Date) -lt $deadline) {
        try {
            return Invoke-RestMethod -Uri $Url -TimeoutSec 5
        }
        catch {
            Start-Sleep -Seconds 2
        }
    }

    throw "Timed out waiting for $Url"
}

$authHeaderName = if ($env:LIARCAS_AUTH_HEADER_NAME) { $env:LIARCAS_AUTH_HEADER_NAME } else { "X-API-Key" }
$apiKey = if ($env:LIARCAS_API_KEY) { $env:LIARCAS_API_KEY } else { "local-dev-api-key" }
$expectedTenantId = if ($env:LIARCAS_TENANT_ID) { $env:LIARCAS_TENANT_ID } else { "tenant-001" }

Write-Host "Starting full stack with Docker Compose..."
docker compose up -d --build

Write-Host "Waiting for services..."
$processingHealth = Wait-Http "http://localhost:8082/health"
$ingestionHealth = Wait-Http "http://localhost:8081/health"

Write-Host "Processing health:"
$processingHealth | Format-List

Write-Host "Ingestion health:"
$ingestionHealth | Format-List

$headers = @{}
$headers[$authHeaderName] = $apiKey

$body = @{
    serviceName    = "payment-service"
    component      = "db-client"
    environment    = "prod"
    serviceVersion = "1.4.2"
    instanceId     = "payment-pod-7"
    traceId        = "trace-abc-123"
    level          = "ERROR"
    message        = "Database timeout"
    exceptionType  = "SQLTransientConnectionException"
    stackTraceHash = "sth-9f8c2d"
} | ConvertTo-Json

Write-Host "Posting test log..."
$response = Invoke-RestMethod -Method Post -Uri http://localhost:8081/logs -Headers $headers -ContentType "application/json" -Body $body

Write-Host "Response from ingestion:"
$response | Format-List

if ($response.tenantId -ne $expectedTenantId) {
    throw "Ingestion response did not resolve tenantId from API key"
}

Write-Host "Waiting for Elasticsearch persistence..."
Start-Sleep -Seconds 4

$stored = Invoke-RestMethod -Method Get -Uri "http://localhost:9200/logs/_doc/$($response.id)"

if (-not $stored.found) {
    throw "Document not found in Elasticsearch for id $($response.id)"
}

$source = $stored._source
$expected = @{
    tenantId       = $expectedTenantId
    serviceName    = "payment-service"
    component      = "db-client"
    environment    = "prod"
    serviceVersion = "1.4.2"
    instanceId     = "payment-pod-7"
    traceId        = "trace-abc-123"
    level          = "ERROR"
    message        = "Database timeout"
    exceptionType  = "SQLTransientConnectionException"
    stackTraceHash = "sth-9f8c2d"
}

foreach ($key in $expected.Keys) {
    if ($source.$key -ne $expected[$key]) {
        throw "Mismatch for $key. Expected '$($expected[$key])', got '$($source.$key)'"
    }
}

if (-not $response.id) {
    throw "Ingestion response did not contain an id"
}

if (-not $response.timestamp) {
    throw "Ingestion response did not contain a timestamp"
}

Write-Host ""
Write-Host "E2E manual test passed." -ForegroundColor Green
Write-Host "Stored Elasticsearch document:"
$source | ConvertTo-Json -Depth 10
# .\runThis.ps1