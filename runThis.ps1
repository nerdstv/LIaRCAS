param(
    [switch]$SkipBuild
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

function Wait-Http {
    param(
        [string]$Url,
        [int]$TimeoutSeconds = 90
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

function Start-ServiceWindow {
    param(
        [string]$JarPath
    )

    Start-Process pwsh -ArgumentList @(
        "-NoExit",
        "-Command",
        "Set-Location '$PSScriptRoot'; java -jar '$JarPath'"
    ) | Out-Null
}

Write-Host "Starting Kafka and Elasticsearch..."
docker compose up -d

Write-Host "Ensuring Kafka topic exists..."
docker compose exec -T kafka kafka-topics --create --if-not-exists --topic raw-logs --bootstrap-server kafka:29092 --partitions 1 --replication-factor 1

if (-not $SkipBuild) {
    Write-Host "Building ingestion and processing jars..."
    .\mvnw.cmd -pl log-ingestion-service,log-processing-service -am package -DskipTests
}

$processingUp = $false
try {
    Invoke-RestMethod http://localhost:8082/health -TimeoutSec 3 | Out-Null
    $processingUp = $true
}
catch {
}

if (-not $processingUp) {
    Write-Host "Starting log-processing-service..."
    Start-ServiceWindow ".\log-processing-service\target\log-processing-service-0.0.1-SNAPSHOT.jar"
}

$ingestionUp = $false
try {
    Invoke-RestMethod http://localhost:8081/health -TimeoutSec 3 | Out-Null
    $ingestionUp = $true
}
catch {
}

if (-not $ingestionUp) {
    Write-Host "Starting log-ingestion-service..."
    Start-ServiceWindow ".\log-ingestion-service\target\log-ingestion-service-0.0.1-SNAPSHOT.jar"
}

Write-Host "Waiting for services..."
$processingHealth = Wait-Http "http://localhost:8082/health"
$ingestionHealth = Wait-Http "http://localhost:8081/health"

Write-Host "Processing health:"
$processingHealth | Format-List

Write-Host "Ingestion health:"
$ingestionHealth | Format-List

$body = @{
    tenantId       = "tenant-001"
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
$response = Invoke-RestMethod -Method Post -Uri http://localhost:8081/logs -ContentType "application/json" -Body $body

Write-Host "Response from ingestion:"
$response | Format-List

if ($response.tenantId -ne "tenant-001") {
    throw "Ingestion response did not preserve tenantId"
}

Write-Host "Waiting for Elasticsearch persistence..."
Start-Sleep -Seconds 4

$stored = Invoke-RestMethod -Method Get -Uri "http://localhost:9200/logs/_doc/$($response.id)"

if (-not $stored.found) {
    throw "Document not found in Elasticsearch for id $($response.id)"
}

$source = $stored._source
$expected = @{
    tenantId       = "tenant-001"
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
# .\runThis.ps1 -SkipBuild