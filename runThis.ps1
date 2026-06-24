param(
    [switch]$SkipComposeUp,
    [switch]$NoBuild,
    [int]$ServiceTimeoutSeconds = 180,
    [int]$PersistenceTimeoutSeconds = 90,
    [int]$MaxPublishAttempts = 3
)

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

function Wait-HttpWithServiceRecovery {
    param(
        [Parameter(Mandatory = $true)][string]$Url,
        [Parameter(Mandatory = $true)][string]$ComposeServiceName,
        [int]$TimeoutSeconds = 120,
        [int]$RecoveryAttempts = 1
    )

    $attempt = 0
    while ($attempt -le $RecoveryAttempts) {
        try {
            return Wait-Http -Url $Url -TimeoutSeconds $TimeoutSeconds
        }
        catch {
            if ($attempt -ge $RecoveryAttempts) {
                throw
            }

            Write-Host "Health check timed out for $Url. Restarting compose service '$ComposeServiceName' and retrying..." -ForegroundColor Yellow
            docker compose up -d $ComposeServiceName | Out-Null
            Start-Sleep -Seconds 6
        }

        $attempt++
    }
}

function Get-TenantIndexName {
    param([Parameter(Mandatory = $true)][string]$TenantId)

    if ([string]::IsNullOrWhiteSpace($TenantId)) {
        throw "TenantId cannot be null or blank"
    }

    $normalized = $TenantId.ToLowerInvariant()
    $normalized = [regex]::Replace($normalized, "[^a-z0-9._-]", "-")
    $normalized = [regex]::Replace($normalized, "^[._-]+", "")

    if ([string]::IsNullOrWhiteSpace($normalized)) {
        $normalized = "unknown"
    }

    return "liarcas-logs-$normalized"
}

function Wait-ForTenantDocument {
    param(
        [Parameter(Mandatory = $true)][string]$IndexName,
        [Parameter(Mandatory = $true)][string]$DocumentId,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $uri = "http://localhost:9200/$IndexName/_doc/$DocumentId"

    while ((Get-Date) -lt $deadline) {
        try {
            $result = Invoke-RestMethod -Method Get -Uri $uri -TimeoutSec 5
            if ($result.found) {
                return $result
            }
        }
        catch {
            # Retry while index and document are still propagating through Kafka -> ES.
        }

        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for document '$DocumentId' in index '$IndexName'"
}

$authHeaderName = if ($env:LIARCAS_AUTH_HEADER_NAME) { $env:LIARCAS_AUTH_HEADER_NAME } else { "X-API-Key" }
$apiKey = if ($env:LIARCAS_API_KEY) { $env:LIARCAS_API_KEY } else { "local-dev-api-key" }
$expectedTenantId = if ($env:LIARCAS_TENANT_ID) { $env:LIARCAS_TENANT_ID } else { "tenant-001" }
$tenantIndexName = Get-TenantIndexName -TenantId $expectedTenantId

if (-not $SkipComposeUp) {
    Write-Host "Starting full stack with Docker Compose..." -ForegroundColor Cyan
    if ($NoBuild) {
        docker compose up -d
    }
    else {
        docker compose up -d --build
    }
}
else {
    Write-Host "Skipping Docker Compose startup (SkipComposeUp provided)." -ForegroundColor Yellow
}

Write-Host "Waiting for services..." -ForegroundColor Cyan
$processingHealth = Wait-HttpWithServiceRecovery -Url "http://localhost:8082/health" -ComposeServiceName "log-processing-service" -TimeoutSeconds $ServiceTimeoutSeconds -RecoveryAttempts 1
$ingestionHealth = Wait-HttpWithServiceRecovery -Url "http://localhost:8081/health" -ComposeServiceName "log-ingestion-service" -TimeoutSeconds $ServiceTimeoutSeconds -RecoveryAttempts 1

Write-Host "Processing health:"
$processingHealth | Format-List

Write-Host "Ingestion health:"
$ingestionHealth | Format-List

# Give Kafka consumer container a brief window to complete partition assignment.
Start-Sleep -Seconds 8

$headers = @{}
$headers[$authHeaderName] = $apiKey

if ($MaxPublishAttempts -lt 1) {
    throw "MaxPublishAttempts must be at least 1"
}

$bodyBase = @{
    serviceName    = "payment-service"
    component      = "db-client"
    environment    = "prod"
    serviceVersion = "1.4.2"
    instanceId     = "payment-pod-7"
    level          = "ERROR"
    message        = "Database timeout"
    exceptionType  = "SQLTransientConnectionException"
    stackTraceHash = "sth-9f8c2d"
}

$response = $null
$stored = $null

for ($attempt = 1; $attempt -le $MaxPublishAttempts; $attempt++) {
    $bodyObject = $bodyBase.Clone()
    $bodyObject.traceId = "trace-abc-123-attempt-$attempt"
    $body = $bodyObject | ConvertTo-Json

    Write-Host "Posting test log to ingestion service (attempt $attempt of $MaxPublishAttempts)..." -ForegroundColor Cyan
    $response = Invoke-RestMethod -Method Post -Uri "http://localhost:8081/logs" -Headers $headers -ContentType "application/json" -Body $body

    Write-Host "Response from ingestion:"
    $response | Format-List

    if (-not $response.id) {
        throw "Ingestion response did not contain an id"
    }

    if (-not $response.timestamp) {
        throw "Ingestion response did not contain a timestamp"
    }

    if ($response.tenantId -ne $expectedTenantId) {
        throw "Ingestion response did not resolve tenantId from API key. Expected '$expectedTenantId', got '$($response.tenantId)'"
    }

    Write-Host "Waiting for Elasticsearch persistence in tenant index '$tenantIndexName'..." -ForegroundColor Cyan
    try {
        $stored = Wait-ForTenantDocument -IndexName $tenantIndexName -DocumentId $response.id -TimeoutSeconds $PersistenceTimeoutSeconds
        break
    }
    catch {
        if ($attempt -eq $MaxPublishAttempts) {
            throw
        }

        Write-Host "Document was not found on attempt $attempt. Retrying with a new event..." -ForegroundColor Yellow
    }
}

$source = $stored._source

$expected = @{
    tenantId       = $expectedTenantId
    serviceName    = "payment-service"
    component      = "db-client"
    environment    = "prod"
    serviceVersion = "1.4.2"
    instanceId     = "payment-pod-7"
    traceId        = $source.traceId
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

Write-Host ""
Write-Host "Manual E2E test passed." -ForegroundColor Green
Write-Host "Tenant index verified: $tenantIndexName" -ForegroundColor Green
Write-Host "Stored Elasticsearch document (_source):"
$source | ConvertTo-Json -Depth 10

Write-Host ""
Write-Host "Useful commands:" -ForegroundColor Yellow
Write-Host "  Show tenant index docs:"
Write-Host "    curl http://localhost:9200/$tenantIndexName/_search?pretty"
Write-Host "  Stop stack:"
Write-Host "    docker compose down"