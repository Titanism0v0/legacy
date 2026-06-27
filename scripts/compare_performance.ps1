<#
.SYNOPSIS
  Compare API performance before and after optimization.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts/compare_performance.ps1 `
    -BeforeBaseUrl "http://localhost:8080/api" `
    -AfterBaseUrl "http://localhost:8082/api" `
    -Iterations 50 -Warmup 5

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts/compare_performance.ps1 `
    -BeforeBaseUrl "http://localhost:8080/api" `
    -Iterations 30

.NOTES
  Optional endpoint JSON format:
  [
    { "name": "product-list", "path": "/product/list?status=ON_SALE&page=1&size=10" },
    { "name": "category-list", "path": "/category/list" }
  ]
#>

[CmdletBinding()]
param(
  [string]$BeforeBaseUrl = "http://localhost:8080/api",
  [string]$AfterBaseUrl = "",
  [string]$EndpointsFile = "",
  [int]$Iterations = 30,
  [int]$Warmup = 5,
  [int]$TimeoutSec = 10,
  [int]$DelayMs = 0,
  [string]$OutDir = "reports/performance",
  [string]$BearerToken = ""
)

Set-StrictMode -Version 2.0
$ErrorActionPreference = "Stop"

function Resolve-FullUrl {
  param(
    [string]$BaseUrl,
    [string]$Path
  )

  $base = $BaseUrl.TrimEnd("/")
  $relative = $Path.TrimStart("/")
  return "$base/$relative"
}

function Get-Percentile {
  param(
    [double[]]$Values,
    [double]$Percentile
  )

  if ($null -eq $Values -or $Values.Count -eq 0) {
    return 0
  }

  $sorted = @($Values | Sort-Object)
  $rank = [Math]::Ceiling(($Percentile / 100.0) * $sorted.Count) - 1
  if ($rank -lt 0) {
    $rank = 0
  }
  if ($rank -ge $sorted.Count) {
    $rank = $sorted.Count - 1
  }
  return [Math]::Round([double]$sorted[$rank], 2)
}

function Invoke-BenchmarkRequest {
  param(
    [string]$Url,
    [hashtable]$Headers,
    [int]$TimeoutSec
  )

  $watch = [System.Diagnostics.Stopwatch]::StartNew()
  $statusCode = 0
  $ok = $false
  $bytes = 0
  $errorMessage = ""

  try {
    $response = Invoke-WebRequest -Uri $Url -Method GET -Headers $Headers -TimeoutSec $TimeoutSec -UseBasicParsing
    $watch.Stop()
    $statusCode = [int]$response.StatusCode
    $ok = $statusCode -ge 200 -and $statusCode -lt 400
    if ($null -ne $response.Content) {
      $bytes = [Text.Encoding]::UTF8.GetByteCount([string]$response.Content)
    }
  } catch {
    $watch.Stop()
    $errorMessage = $_.Exception.Message
    if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
      $statusCode = [int]$_.Exception.Response.StatusCode
    }
  }

  return [pscustomobject]@{
    DurationMs = [Math]::Round($watch.Elapsed.TotalMilliseconds, 2)
    StatusCode = $statusCode
    Success = $ok
    Bytes = $bytes
    Error = $errorMessage
  }
}

function Invoke-EndpointBenchmark {
  param(
    [string]$Label,
    [string]$BaseUrl,
    [object[]]$Endpoints,
    [int]$Iterations,
    [int]$Warmup,
    [int]$TimeoutSec,
    [int]$DelayMs,
    [hashtable]$Headers
  )

  $rows = New-Object System.Collections.Generic.List[object]

  foreach ($endpoint in $Endpoints) {
    $name = [string]$endpoint.name
    $path = [string]$endpoint.path
    $url = Resolve-FullUrl -BaseUrl $BaseUrl -Path $path

    Write-Host "[$Label] Warmup $name -> $path"
    for ($i = 1; $i -le $Warmup; $i++) {
      [void](Invoke-BenchmarkRequest -Url $url -Headers $Headers -TimeoutSec $TimeoutSec)
      if ($DelayMs -gt 0) {
        Start-Sleep -Milliseconds $DelayMs
      }
    }

    Write-Host "[$Label] Benchmark $name ($Iterations requests)"
    for ($i = 1; $i -le $Iterations; $i++) {
      $result = Invoke-BenchmarkRequest -Url $url -Headers $Headers -TimeoutSec $TimeoutSec
      $rows.Add([pscustomobject]@{
        Label = $Label
        Endpoint = $name
        Path = $path
        Iteration = $i
        DurationMs = $result.DurationMs
        StatusCode = $result.StatusCode
        Success = $result.Success
        Bytes = $result.Bytes
        Error = $result.Error
      })
      if ($DelayMs -gt 0) {
        Start-Sleep -Milliseconds $DelayMs
      }
    }
  }

  return $rows
}

function Summarize-Rows {
  param(
    [object[]]$Rows
  )

  $summary = New-Object System.Collections.Generic.List[object]
  $groups = $Rows | Group-Object Label, Endpoint

  foreach ($group in $groups) {
    $items = @($group.Group)
    $successItems = @($items | Where-Object { $_.Success -eq $true })
    $durations = @($successItems | ForEach-Object { [double]$_.DurationMs })
    $avg = 0
    $min = 0
    $max = 0

    if ($durations.Count -gt 0) {
      $avg = [Math]::Round((($durations | Measure-Object -Average).Average), 2)
      $min = [Math]::Round((($durations | Measure-Object -Minimum).Minimum), 2)
      $max = [Math]::Round((($durations | Measure-Object -Maximum).Maximum), 2)
    }

    $summary.Add([pscustomobject]@{
      Label = [string]$items[0].Label
      Endpoint = [string]$items[0].Endpoint
      Path = [string]$items[0].Path
      Requests = $items.Count
      Success = $successItems.Count
      SuccessRate = if ($items.Count -eq 0) { 0 } else { [Math]::Round(($successItems.Count * 100.0 / $items.Count), 2) }
      AvgMs = $avg
      P50Ms = Get-Percentile -Values $durations -Percentile 50
      P95Ms = Get-Percentile -Values $durations -Percentile 95
      MinMs = $min
      MaxMs = $max
      AvgBytes = if ($successItems.Count -eq 0) { 0 } else { [Math]::Round((($successItems | Measure-Object Bytes -Average).Average), 2) }
    })
  }

  return $summary
}

function Build-ComparisonRows {
  param(
    [object[]]$Summary
  )

  $rows = New-Object System.Collections.Generic.List[object]
  $endpointNames = @($Summary | Select-Object -ExpandProperty Endpoint -Unique)

  foreach ($endpoint in $endpointNames) {
    $before = $Summary | Where-Object { $_.Label -eq "before" -and $_.Endpoint -eq $endpoint } | Select-Object -First 1
    $after = $Summary | Where-Object { $_.Label -eq "after" -and $_.Endpoint -eq $endpoint } | Select-Object -First 1
    if ($null -eq $before -or $null -eq $after) {
      continue
    }

    $avgImprove = 0
    $p95Improve = 0
    if ([double]$before.AvgMs -gt 0) {
      $avgImprove = [Math]::Round((([double]$before.AvgMs - [double]$after.AvgMs) * 100.0 / [double]$before.AvgMs), 2)
    }
    if ([double]$before.P95Ms -gt 0) {
      $p95Improve = [Math]::Round((([double]$before.P95Ms - [double]$after.P95Ms) * 100.0 / [double]$before.P95Ms), 2)
    }

    $rows.Add([pscustomobject]@{
      Endpoint = $endpoint
      Path = $before.Path
      BeforeAvgMs = $before.AvgMs
      AfterAvgMs = $after.AvgMs
      AvgImprovementPercent = $avgImprove
      BeforeP95Ms = $before.P95Ms
      AfterP95Ms = $after.P95Ms
      P95ImprovementPercent = $p95Improve
      BeforeSuccessRate = $before.SuccessRate
      AfterSuccessRate = $after.SuccessRate
    })
  }

  return $rows
}

function Write-MarkdownReport {
  param(
    [string]$Path,
    [object[]]$Summary,
    [object[]]$Comparison,
    [string]$BeforeBaseUrl,
    [string]$AfterBaseUrl,
    [int]$Iterations,
    [int]$Warmup
  )

  $lines = New-Object System.Collections.Generic.List[string]
  $lines.Add("# Performance Comparison Report")
  $lines.Add("")
  $lines.Add("- Generated at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')")
  $lines.Add("- Before base URL: ``$BeforeBaseUrl``")
  if ($AfterBaseUrl) {
    $lines.Add("- After base URL: ``$AfterBaseUrl``")
  }
  $lines.Add("- Iterations per endpoint: ``$Iterations``")
  $lines.Add("- Warmup requests per endpoint: ``$Warmup``")
  $lines.Add("")

  if ($Comparison.Count -gt 0) {
    $lines.Add("## Before vs After")
    $lines.Add("")
    $lines.Add("| Endpoint | Before Avg(ms) | After Avg(ms) | Avg Improve | Before P95(ms) | After P95(ms) | P95 Improve | Success Rate |")
    $lines.Add("| --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |")
    foreach ($row in $Comparison) {
      $lines.Add("| $($row.Endpoint) | $($row.BeforeAvgMs) | $($row.AfterAvgMs) | $($row.AvgImprovementPercent)% | $($row.BeforeP95Ms) | $($row.AfterP95Ms) | $($row.P95ImprovementPercent)% | $($row.BeforeSuccessRate)% -> $($row.AfterSuccessRate)% |")
    }
    $lines.Add("")
  }

  $lines.Add("## Summary")
  $lines.Add("")
  $lines.Add("| Label | Endpoint | Requests | Success Rate | Avg(ms) | P50(ms) | P95(ms) | Min(ms) | Max(ms) |")
  $lines.Add("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
  foreach ($row in $Summary) {
    $lines.Add("| $($row.Label) | $($row.Endpoint) | $($row.Requests) | $($row.SuccessRate)% | $($row.AvgMs) | $($row.P50Ms) | $($row.P95Ms) | $($row.MinMs) | $($row.MaxMs) |")
  }

  $lines | Set-Content -Path $Path -Encoding UTF8
}

if ($Iterations -le 0) {
  throw "Iterations must be greater than 0."
}
if ($Warmup -lt 0) {
  throw "Warmup cannot be negative."
}

$defaultEndpoints = @(
  [pscustomobject]@{ name = "product-list"; path = "/product/list?status=ON_SALE&page=1&size=10" },
  [pscustomobject]@{ name = "category-list"; path = "/category/list" },
  [pscustomobject]@{ name = "exchange-rate"; path = "/exchange-rate/current" },
  [pscustomobject]@{ name = "community-posts"; path = "/community/posts?page=1&size=10" }
)

if ($EndpointsFile) {
  if (!(Test-Path -LiteralPath $EndpointsFile)) {
    throw "Endpoints file not found: $EndpointsFile"
  }
  $parsedEndpoints = Get-Content -Raw -Encoding UTF8 -Path $EndpointsFile | ConvertFrom-Json
  $endpoints = @($parsedEndpoints | ForEach-Object { $_ })
} else {
  $endpoints = $defaultEndpoints
}

$headers = @{}
if ($BearerToken) {
  $headers["Authorization"] = "Bearer $BearerToken"
}

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$rawPath = Join-Path $OutDir "benchmark_raw_$stamp.csv"
$summaryPath = Join-Path $OutDir "benchmark_summary_$stamp.csv"
$comparisonPath = Join-Path $OutDir "benchmark_comparison_$stamp.csv"
$reportPath = Join-Path $OutDir "benchmark_report_$stamp.md"

$allRows = New-Object System.Collections.Generic.List[object]
$beforeRows = Invoke-EndpointBenchmark -Label "before" -BaseUrl $BeforeBaseUrl -Endpoints $endpoints -Iterations $Iterations -Warmup $Warmup -TimeoutSec $TimeoutSec -DelayMs $DelayMs -Headers $headers
foreach ($row in $beforeRows) {
  $allRows.Add($row)
}

if ($AfterBaseUrl) {
  $afterRows = Invoke-EndpointBenchmark -Label "after" -BaseUrl $AfterBaseUrl -Endpoints $endpoints -Iterations $Iterations -Warmup $Warmup -TimeoutSec $TimeoutSec -DelayMs $DelayMs -Headers $headers
  foreach ($row in $afterRows) {
    $allRows.Add($row)
  }
}

$summary = @(Summarize-Rows -Rows $allRows)
$comparison = @(Build-ComparisonRows -Summary $summary)

$allRows | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $rawPath
$summary | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $summaryPath
if ($comparison.Count -gt 0) {
  $comparison | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $comparisonPath
}
Write-MarkdownReport -Path $reportPath -Summary $summary -Comparison $comparison -BeforeBaseUrl $BeforeBaseUrl -AfterBaseUrl $AfterBaseUrl -Iterations $Iterations -Warmup $Warmup

Write-Host ""
Write-Host "Benchmark finished."
Write-Host "Raw CSV:      $rawPath"
Write-Host "Summary CSV:  $summaryPath"
if ($comparison.Count -gt 0) {
  Write-Host "Compare CSV:  $comparisonPath"
}
Write-Host "Report:       $reportPath"
