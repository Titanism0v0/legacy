$ErrorActionPreference = 'Stop'
$toolDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$queries = Get-Content -Raw (Join-Path $toolDir 'catalog-queries.json') | ConvertFrom-Json
$usedUrls = [System.Collections.Generic.HashSet[string]]::new()
$manifest = [System.Collections.Generic.List[object]]::new()
$headers = @{ 'User-Agent' = 'legacy-demo-curator/1.0 (local portfolio demo)' }
$excludedTitle = '(?i)\b(logo|icon|diagram|drawing|vector|flag|map|manual|screenshot|advertisement|poster)\b'
$unsplashSources = @{
    1='photo-1695048133142-1a20484d2569'; 2='photo-1618366712010-f4ae9c647dcb';
    3='photo-1517336714731-489689fd1ca8'; 4='photo-1578303512597-81e6cc155b3e';
    5='photo-1600294037681-c80b4cb5b434'; 6='photo-1571781926291-c477ebfd024b';
    7='photo-1608248543803-ba4f8c70ae0b'; 8='photo-1620916566398-39f1143ab7be';
    9='photo-1541643600914-78b084683601'; 10='photo-1556228578-0d85b1a4d571';
    11='photo-1542291026-7eec264c27ff'; 12='photo-1587563871167-1ee9c731aefb';
    13='photo-1539533018447-63fcce2678e3'; 14='photo-1506629082955-511b1aa562c8';
    15='photo-1520903920243-00d872a2d1c9'; 16='photo-1584308666744-24d5c474f2ae';
    17='photo-1559757175-5700dde675bc'; 18='photo-1549007994-cb92caebd54b';
    19='photo-1481391319762-47dff72954d9'; 20='photo-1587049352846-4a222e784d38';
    23='photo-1515942400420-2b98fed1f515'; 24='photo-1522771930-78848d9293e8';
    25='photo-1555252333-9f8e92e65df9'; 26='photo-1565026057447-bc90a3dceb87';
    27='photo-1558317374-067fb5f30001'; 28='photo-1602928321679-560bb453f190';
    29='photo-1592496001020-d31bd830651f'; 30='photo-1587654780291-39c9404d746b'
}
$productFallback = @{
    6='facial essence bottle'; 7='face cream jar'; 8='skincare serum bottle';
    9='men perfume bottle'; 10='skincare serum bottle'; 13='black parka jacket';
    14='black yoga pants'; 15='beige check scarf'; 16='vitamin supplement bottle';
    17='fish oil capsules'; 18='Japanese biscuits'; 19='chocolate gift box';
    23='infant formula tin'; 24='baby carrier'; 25='wooden high chair';
    26='aluminum suitcase'; 27='cordless vacuum cleaner'; 28='aroma diffuser';
    30='toy brick castle'
}
$categoryFallback = @{
    1='consumer electronics'; 2='cosmetic bottle'; 3='clothing product';
    4='food product'; 5='baby product'; 6='household product'
}

function Get-CategoryId([int]$ProductId) {
    if ($ProductId -le 5 -or ($ProductId -ge 31 -and $ProductId -le 35)) { return 1 }
    if (($ProductId -ge 6 -and $ProductId -le 10) -or ($ProductId -ge 36 -and $ProductId -le 40)) { return 2 }
    if (($ProductId -ge 11 -and $ProductId -le 15) -or ($ProductId -ge 41 -and $ProductId -le 45)) { return 3 }
    if (($ProductId -ge 16 -and $ProductId -le 20) -or ($ProductId -ge 46 -and $ProductId -le 50)) { return 4 }
    if (($ProductId -ge 23 -and $ProductId -le 25) -or ($ProductId -ge 51 -and $ProductId -le 55)) { return 5 }
    return 6
}

function Find-OpenverseCandidate([string]$SearchTerm) {
    $encoded = [uri]::EscapeDataString($SearchTerm)
    $url = "https://api.openverse.org/v1/images/?q=$encoded&page_size=20&license_type=commercial"
    $payload = Invoke-RestMethod -Uri $url -Headers $headers -TimeoutSec 30
    return @($payload.results | Where-Object {
        if ($_.mature -eq $true) { return $false }
        if ($_.title -match $excludedTitle) { return $false }
        if ($_.filetype) {
            if ($_.filetype -notmatch '^(jpg|jpeg|png|webp)$') { return $false }
        } elseif ($_.url -notmatch '(?i)\.(jpg|jpeg|png|webp)(\?|$)') {
            return $false
        }
        if ([int]$_.width -lt 640 -or [int]$_.height -lt 480) { return $false }
        $ratio = [double]$_.width / [double]$_.height
        if ($ratio -lt 0.55 -or $ratio -gt 2.2) { return $false }
        if ([string]::IsNullOrWhiteSpace([string]$_.url)) { return $false }
        return -not $usedUrls.Contains([string]$_.url)
    })
}

foreach ($item in $queries) {
    $id = [int]$item.productId
    $paddedId = $id.ToString('000')
    $localPath = "/demo/products/catalog/$paddedId-$($item.slug).webp"
    if ($unsplashSources.ContainsKey($id)) {
        $imageUrl = "https://images.unsplash.com/$($unsplashSources[$id])?auto=format&fit=max&w=1600&q=88"
        $manifest.Add([ordered]@{
            productId = $id
            title = [string]$item.title
            sourceSite = 'Unsplash (existing project seed)'
            sourceProvider = 'unsplash'
            sourcePage = $imageUrl
            openversePage = ''
            downloadUrl = $imageUrl
            thumbnailUrl = $imageUrl
            author = 'Original project seed; photographer metadata not retained'
            license = 'Unsplash License'
            licenseUrl = 'https://unsplash.com/license'
            retrievedAt = '2026-06-24'
            searchTerm = 'existing project seed image'
            selectedTitle = [string]$item.title
            localPath = $localPath
        })
        Write-Host "$id [existing Unsplash seed]: $($item.title)"
        continue
    }
    $cleanQuery = ([string]$item.query -replace '(?i)\s+product\s*$', '').Trim()
    $terms = [System.Collections.Generic.List[string]]::new()
    $terms.Add($cleanQuery)
    if ($productFallback.ContainsKey($id)) { $terms.Add([string]$productFallback[$id]) }
    $terms.Add([string]$categoryFallback[(Get-CategoryId $id)])
    $selected = $null
    $selectedTerm = ''
    foreach ($term in $terms | Select-Object -Unique) {
        $candidates = @(Find-OpenverseCandidate $term)
        if ($candidates.Count -gt 0) {
            $selectionIndex = if ($null -ne $item.selectionIndex) { [int]$item.selectionIndex } else { 0 }
            if ($selectionIndex -ge $candidates.Count) { $selectionIndex = 0 }
            $selected = $candidates[$selectionIndex]
            $selectedTerm = $term
            break
        }
    }
    if ($null -eq $selected) { throw "No Openverse candidate for product $id" }
    [void]$usedUrls.Add([string]$selected.url)
    $manifest.Add([ordered]@{
        productId = $id
        title = [string]$item.title
        sourceSite = 'Openverse'
        sourceProvider = [string]$selected.source
        sourcePage = if ($selected.foreign_landing_url) { [string]$selected.foreign_landing_url } else { [string]$selected.detail_url }
        openversePage = [string]$selected.detail_url
        downloadUrl = [string]$selected.url
        thumbnailUrl = [string]$selected.thumbnail
        author = if ($selected.creator) { [string]$selected.creator } else { 'Openverse contributor' }
        license = (([string]$selected.license).ToUpper() + ' ' + [string]$selected.license_version).Trim()
        licenseUrl = [string]$selected.license_url
        retrievedAt = '2026-06-24'
        searchTerm = $selectedTerm
        selectedTitle = [string]$selected.title
        localPath = $localPath
    })
    Write-Host "$id [$selectedTerm]: $($selected.title)"
    Start-Sleep -Milliseconds 180
}

$manifestPath = Join-Path $toolDir 'catalog-manifest.json'
$manifest | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 $manifestPath
Write-Host "Wrote $($manifest.Count) Openverse entries"
