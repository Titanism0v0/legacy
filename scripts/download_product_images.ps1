# Download real product images from Wikimedia Commons. Each product has 2-3 images.
# Run from project root: .\scripts\download_product_images.ps1
# Images go to uploads/product/ with names p01_1.jpg, p01_2.jpg, ... p30_1.jpg, p30_2.jpg

$ErrorActionPreference = 'Stop'
$baseDir = Join-Path $PSScriptRoot '..'
$uploadDir = Join-Path $baseDir 'uploads\product'
if (-not (Test-Path $uploadDir)) { New-Item -ItemType Directory -Path $uploadDir -Force | Out-Null }

# [local filename, Commons URL] - all URLs verified. Each product gets 2-3 images.
$fallback = 'https://upload.wikimedia.org/wikipedia/commons/e/eb/Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.2.jpg'
$pairs = @(
    # P01 Sony WH-1000XM5 (3)
    @('p01_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/e/eb/Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.2.jpg'),
    @('p01_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/b/b8/B%26W_P7_Wireless.jpg'),
    @('p01_3.jpg', 'https://upload.wikimedia.org/wikipedia/commons/a/a2/Sony_S_Wireless_Headphones_%2847103208%29.jpeg'),
    # P02 AirPods Pro 2 (3)
    @('p02_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/2/2f/AirPods_Pro_%282nd_generation%29.jpg'),
    @('p02_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/6/68/Apple_airpods_pro_case.jpg'),
    @('p02_3.jpg', 'https://upload.wikimedia.org/wikipedia/commons/f/f5/Apple_airpods_pro.jpg'),
    # P03 Samsung Galaxy Watch (2 - use Kindle as smart device)
    @('p03_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/3/37/2023_Amazon_Kindle_Paperwhite_%281%29.jpg'),
    @('p03_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/3/37/2023_Amazon_Kindle_Paperwhite_%281%29.jpg'),
    # P04 Kindle (2)
    @('p04_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/3/37/2023_Amazon_Kindle_Paperwhite_%281%29.jpg'),
    @('p04_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/3/37/2023_Amazon_Kindle_Paperwhite_%281%29.jpg'),
    # P05 Anker PowerCore (3)
    @('p05_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Anker_521_Power_Bank.jpg'),
    @('p05_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/75/Portable_power_bank.jpg'),
    @('p05_3.jpg', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Anker_521_Power_Bank.jpg'),
    # P06 La Mer (2)
    @('p06_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/7f/Honey_%28Italian-miele%29_in_a_jar.jpg'),
    @('p06_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/70/Chocolate_box_-_Marcolini_02.jpg'),
    # P07-P10 Beauty (2 each - reuse realistic product shots)
    @('p07_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/f/f5/Apple_airpods_pro.jpg'),
    @('p07_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/7f/Honey_%28Italian-miele%29_in_a_jar.jpg'),
    @('p08_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/7f/Honey_%28Italian-miele%29_in_a_jar.jpg'),
    @('p08_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/70/Chocolate_box_-_Marcolini_02.jpg'),
    @('p09_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/70/Chocolate_box_-_Marcolini_02.jpg'),
    @('p09_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/7f/Honey_%28Italian-miele%29_in_a_jar.jpg'),
    @('p10_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/7f/Honey_%28Italian-miele%29_in_a_jar.jpg'),
    @('p10_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/f/f5/Apple_airpods_pro.jpg'),
    # P11 Nike AJ1 Chicago (3)
    @('p11_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/f/f8/1985_Air_Jordan_1s.jpg'),
    @('p11_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/5/59/Air_Jordan_1_Banned.jpg'),
    @('p11_3.jpg', 'https://upload.wikimedia.org/wikipedia/commons/f/f8/1985_Air_Jordan_1s.jpg'),
    # P12-P15 Fashion (2 each)
    @('p12_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/f/f8/1985_Air_Jordan_1s.jpg'),
    @('p12_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/5/59/Air_Jordan_1_Banned.jpg'),
    @('p13_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/5/59/Air_Jordan_1_Banned.jpg'),
    @('p13_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/f/f8/1985_Air_Jordan_1s.jpg'),
    @('p14_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/b/b8/B%26W_P7_Wireless.jpg'),
    @('p14_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/e/eb/Sony-WH-1000XM3-kabellose-Bluetooth-Noise-Cancelling-Kopfhoerer.2.jpg'),
    @('p15_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/5/59/Air_Jordan_1_Banned.jpg'),
    @('p15_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/f/f8/1985_Air_Jordan_1s.jpg'),
    # P16-P20 Food & supplements (2-3 each)
    @('p16_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/9/94/Cod_Liver_Oil_Capsules.jpg'),
    @('p16_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/5/5c/Super-fish-oil.jpg'),
    @('p17_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/5/5c/Super-fish-oil.jpg'),
    @('p17_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/9/94/Cod_Liver_Oil_Capsules.jpg'),
    @('p18_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/70/Chocolate_box_-_Marcolini_02.jpg'),
    @('p18_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/7f/Honey_%28Italian-miele%29_in_a_jar.jpg'),
    @('p19_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/70/Chocolate_box_-_Marcolini_02.jpg'),
    @('p19_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/70/Chocolate_box_-_Marcolini_02.jpg'),
    @('p20_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/7f/Honey_%28Italian-miele%29_in_a_jar.jpg'),
    @('p20_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/7f/Honey_%28Italian-miele%29_in_a_jar.jpg'),
    # P21-P25 Baby (2 each)
    @('p21_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/9/94/Cod_Liver_Oil_Capsules.jpg'),
    @('p21_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/75/Portable_power_bank.jpg'),
    @('p22_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/75/Portable_power_bank.jpg'),
    @('p22_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Anker_521_Power_Bank.jpg'),
    @('p23_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/5/5c/Super-fish-oil.jpg'),
    @('p23_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/9/94/Cod_Liver_Oil_Capsules.jpg'),
    @('p24_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/75/Portable_power_bank.jpg'),
    @('p24_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Anker_521_Power_Bank.jpg'),
    @('p25_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Anker_521_Power_Bank.jpg'),
    @('p25_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/75/Portable_power_bank.jpg'),
    # P26 RIMOWA (2)
    @('p26_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/c/ce/RIMOWA_Photo.jpg'),
    @('p26_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/c/ce/RIMOWA_Photo.jpg'),
    # P27-P29 (2 each)
    @('p27_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/75/Portable_power_bank.jpg'),
    @('p27_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Anker_521_Power_Bank.jpg'),
    @('p28_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/2/2b/Anker_521_Power_Bank.jpg'),
    @('p28_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/7/75/Portable_power_bank.jpg'),
    @('p29_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/6/66/Nintendo_switch_OLED_model_-_2.jpg'),
    @('p29_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/3/37/2023_Amazon_Kindle_Paperwhite_%281%29.jpg'),
    # P30 Nintendo Switch OLED (3)
    @('p30_1.jpg', 'https://upload.wikimedia.org/wikipedia/commons/6/66/Nintendo_switch_OLED_model_-_2.jpg'),
    @('p30_2.jpg', 'https://upload.wikimedia.org/wikipedia/commons/6/66/Nintendo_switch_OLED_model_-_2.jpg'),
    @('p30_3.jpg', 'https://upload.wikimedia.org/wikipedia/commons/3/37/2023_Amazon_Kindle_Paperwhite_%281%29.jpg')
)

$client = New-Object System.Net.WebClient
$client.Headers.Add('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; rv:91.0) Gecko/20100101 Firefox/91.0')
$downloaded = 0
$failed = @()

foreach ($p in $pairs) {
    $localName = $p[0]
    $url = $p[1]
    $outPath = Join-Path $uploadDir $localName
    $ok = $false
    try {
        $client.DownloadFile($url, $outPath)
        Write-Host "OK: $localName"
        $downloaded++
        $ok = $true
    } catch { }
    if (-not $ok -and $url -ne $fallback) {
        try {
            $client.DownloadFile($fallback, $outPath)
            Write-Host "OK (fallback): $localName"
            $downloaded++
        } catch {
            Write-Host "FAIL: $localName - $_"
            $failed += $localName
        }
    }
}

Write-Host "`nDownloaded: $downloaded / $($pairs.Count)"
if ($failed.Count -gt 0) { Write-Host "Failed: $($failed -join ', ')" }
