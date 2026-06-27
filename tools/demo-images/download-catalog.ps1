param(
    [int[]]$ProductId = @(),
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$toolDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$manifest = Get-Content -Raw (Join-Path $toolDir 'catalog-manifest.json') | ConvertFrom-Json
$rawDir = Join-Path $toolDir 'raw'
New-Item -ItemType Directory -Force -Path $rawDir | Out-Null
$headers = @{ 'User-Agent' = 'Mozilla/5.0 (compatible; legacy-demo-asset-downloader/1.0)' }

foreach ($item in $manifest) {
    if ($ProductId.Count -gt 0 -and [int]$item.productId -notin $ProductId) { continue }
    $target = Join-Path $rawDir (([int]$item.productId).ToString('000') + '.source')
    if (-not $Force -and (Test-Path -LiteralPath $target) -and (Get-Item -LiteralPath $target).Length -ge 10000) {
        Write-Host "$($item.productId): cached"
        continue
    }
    $urls = @([string]$item.downloadUrl, [string]$item.thumbnailUrl) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) } | Select-Object -Unique
    $downloaded = $false
    foreach ($url in $urls) {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri $url -Headers $headers -TimeoutSec 45 -OutFile $target
            if ((Get-Item -LiteralPath $target).Length -lt 10000) { throw 'downloaded file is too small' }
            $downloaded = $true
            break
        } catch {
            Remove-Item -LiteralPath $target -Force -ErrorAction SilentlyContinue
        }
    }
    if (-not $downloaded) { throw "Unable to download image for product $($item.productId)" }
    Write-Host "$($item.productId): $((Get-Item -LiteralPath $target).Length) bytes"
}

Write-Host 'Requested source images are available'
