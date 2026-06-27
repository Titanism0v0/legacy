$ErrorActionPreference = 'Stop'
$toolDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$manifest = @(Get-Content -Raw (Join-Path $toolDir 'catalog-manifest.json') | ConvertFrom-Json)
$overrides = Get-Content -Raw (Join-Path $toolDir 'openverse-overrides.json') | ConvertFrom-Json
$headers = @{ 'User-Agent' = 'legacy-demo-curator/1.0 (local portfolio demo)' }

foreach ($override in $overrides) {
    $query = [uri]::EscapeDataString([string]$override.query)
    $payload = Invoke-RestMethod -Uri "https://api.openverse.org/v1/images/?q=$query&page_size=12&license_type=commercial" -Headers $headers -TimeoutSec 30
    $candidates = @($payload.results | Where-Object {
        $_.mature -ne $true -and $_.width -ge 640 -and $_.height -ge 480 -and $_.url
    })
    $index = [int]$override.selectionIndex
    if ($index -ge $candidates.Count) { throw "Override index out of range for product $($override.productId)" }
    $selected = $candidates[$index]
    $item = $manifest | Where-Object { [int]$_.productId -eq [int]$override.productId }
    if ($null -eq $item) { throw "Missing manifest product $($override.productId)" }
    $item.sourceSite = 'Openverse'
    $item.sourceProvider = [string]$selected.source
    $item.sourcePage = if ($selected.foreign_landing_url) { [string]$selected.foreign_landing_url } else { [string]$selected.detail_url }
    $item.openversePage = [string]$selected.detail_url
    $item.downloadUrl = [string]$selected.url
    $item.thumbnailUrl = [string]$selected.thumbnail
    $item.author = if ($selected.creator) { [string]$selected.creator } else { 'Openverse contributor' }
    $item.license = (([string]$selected.license).ToUpper() + ' ' + [string]$selected.license_version).Trim()
    $item.licenseUrl = [string]$selected.license_url
    $item.searchTerm = [string]$override.query
    $item.selectedTitle = [string]$selected.title
    Write-Host "$($item.productId): $($item.selectedTitle)"
}

$manifest | Sort-Object productId | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $toolDir 'catalog-manifest.json')
Write-Host "Applied $($overrides.Count) reviewed overrides"
