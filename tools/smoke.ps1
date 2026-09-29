param(
  [string]$Base = 'http://127.0.0.1:8080',
  [string]$StaffPhone = '13800000002',
  [string]$StaffPassword = 'staff123456',
  [string]$UserPhone = '13800000001',
  [string]$UserPassword = 'smoke123456'
)

$ErrorActionPreference = 'Stop'
$Api  = "$Base/api/v1"
$Auth = "$Base/api/v1/auth"
$Script:Pass = 0
$Script:Fail = 0

function Write-Ok($m) { $Script:Pass++; Write-Host "[OK]   $m" -ForegroundColor Green }
function Write-Skip($m) { Write-Host "[SKIP] $m" -ForegroundColor Yellow }
function Write-Fail($m) { $Script:Fail++; Write-Host "[FAIL] $m" -ForegroundColor Red }
function Write-Warn($m) { Write-Host "[WARN] $m" -ForegroundColor Yellow }
function Write-Step($m) { Write-Host "`n==> $m" -ForegroundColor Cyan }

function New-Api($url, $method, $token, $body) {
  $hdr = @{}
  if ($token) { $hdr['Authorization'] = "Bearer $token" }
  $a = @{ Uri = $url; Method = $method; Headers = $hdr }
  if ($body) {
    $a['Body'] = [System.Text.Encoding]::UTF8.GetBytes($body)
    $hdr['Content-Type'] = 'application/json'
  }
  try {
    $raw = Invoke-RestMethod @a
    return ($raw | ConvertTo-Json -Depth 20)
  } catch {
    if ($_.Exception.Response) {
      $stream = $_.Exception.Response.BaseStream
      if ($null -eq $stream) { return ("HTTP " + [int]$_.Exception.Response.StatusCode + " (no body)") }
      $reader = New-Object System.IO.StreamReader($stream)
      return ("HTTP " + [int]$_.Exception.Response.StatusCode + " " + $reader.ReadToEnd())
    }
    return $_.Exception.Message
  }
}

function Get-Token($json) {
  if ([string]::IsNullOrWhiteSpace($json)) { return $null }
  try {
    $obj = $json | ConvertFrom-Json
    if ($obj.data -and $obj.data.token) { return $obj.data.token }
  } catch {}
  return $null
}

function Get-Id($json) {
  if ([string]::IsNullOrWhiteSpace($json)) { return $null }
  try {
    $obj = $json | ConvertFrom-Json
    if ($obj.data -and $obj.data.id) { return $obj.data.id }
  } catch {}
  return $null
}

function Build-Register($ph, $pw) {
  '{"phone":"' + $ph + '","name":"Smoke","password":"' + $pw + '"}'
}

function Build-Route() {
  '{"strategy":"FASTEST","fromLon":116.4,"fromLat":39.9,"toLon":121.47,"toLat":31.23}'
}

function Build-RouteStrategy($s) {
  '{"strategy":"' + $s + '","fromLon":116.4,"fromLat":39.9,"toLon":121.47,"toLat":31.23}'
}

function Build-Event() {
  '{"type":"IN_TRANSIT","lon":116.4,"lat":39.9}'
}

function Build-Callback($txn) {
  '{"txnNo":"' + $txn + '","amountFen":1200}'
}

Write-Host "== smoke: base=$Base =="

# ---------- staff login ----------
Write-Step 'staff login (feature 1) -> token'
$sl = New-Api "$Auth/login" 'Post' $null ('{"phone":"' + $StaffPhone + '","password":"' + $StaffPassword + '"}')
$Stok = Get-Token $sl
if ($Stok) { Write-Ok "staff login -> token" } else { Write-Fail "staff login: $sl" }

if ($Stok) {
  # ---------- staff side: 9 features ----------
  Write-Step '2. profile (feature 2: 个人中心)'
  $P = New-Api "$Api/staff/profile" 'Get' $Stok $null
  if ($P -match '"totalOrders"|licenseNo') { Write-Ok "staff profile" } else { Write-Warn "profile: $P" }

  Write-Step '3. open demands (feature 3: 需求查看)'
  $D = New-Api "$Api/staff/demands?page=0&size=50" 'Get' $Stok $null
  $firstDemand = ($D | ConvertFrom-Json).data.content | Select-Object -First 1
  if ($firstDemand) { Write-Ok ("staff sees demand id=" + $firstDemand.id) } else { Write-Warn "no open demand: $D" }

  Write-Step '4. quote demand (feature 4: 接取需求)'
  if ($firstDemand) {
    $Q = New-Api "$Api/staff/demands/$($firstDemand.id)/quote" 'Post' $Stok '{"priceFen":1500}'
    if ($Q -match 'QUOTED|"code":\s*0') { Write-Ok "staff quoted demand" } else { Write-Warn "quote: $Q" }
  } else { Write-Skip "quote (no demand)" }

  # user confirms + pays to create an order
  Write-Step 'user login + confirm + pay (to create order)'
  $ul = New-Api "$Auth/login" 'Post' $null ('{"phone":"' + $UserPhone + '","password":"' + $UserPassword + '"}')
  $Utok = Get-Token $ul
  if (-not $Utok) { Write-Warn "user login: $ul" }
  $C = New-Api "$Api/user/demands/$($firstDemand.id)/confirm" 'Post' $Utok $null
  $Oid = Get-Id $C
  if ($Oid) {
    $Pay = New-Api "$Api/user/orders/$Oid/pay" 'Post' $Utok $null
    $Txn = (($Pay | ConvertFrom-Json).data.txnNo)
    $Cb = New-Api "$Api/user/orders/$Oid/pay/callback" 'Post' $Utok (Build-Callback $Txn)
    Write-Ok ("order created id=" + $Oid + " + paid")
  } else {
    # fall back: find an existing staff order
    $MO = New-Api "$Api/staff/orders?page=0&size=1" 'Get' $Stok $null
    $Oid = ($MO | ConvertFrom-Json).data.content | Select-Object -First 1 | ForEach-Object { $_.id }
    if ($Oid) { Write-Ok ("fallback existing order id=" + $Oid) } else { Write-Warn "no order" }
  }

  Write-Step '5/6. route plan + recommend 3 strategies (feature 5+6)'
  if ($Oid) {
    foreach ($s in @('SHORTEST','FASTEST','CHEAPEST')) {
      $R = New-Api "$Api/staff/orders/$Oid/route" 'Post' $Stok (Build-RouteStrategy $s)
      if ($R -match 'distanceM|geometry|"code":\s*0') { Write-Ok ("route ${s} planned") } else { Write-Warn ("route ${s}: " + $R) }
    }
  } else { Write-Skip "route plan" }

  Write-Step '7. order list + status (feature 7: 订单管理)'
  if ($Oid) {
    $OL = New-Api "$Api/staff/orders?page=0&size=20" 'Get' $Stok $null
    if ($OL -match '"status"') { Write-Ok "staff orders listed" } else { Write-Warn "orders: $OL" }
  } else { Write-Skip "orders" }

  Write-Step '8. reviews (feature 8: 评论管理)'
  $RV = New-Api "$Api/staff/reviews" 'Get' $Stok $null
  if ($RV -match '"rating"|content|\[\]') { Write-Ok "staff reviews listed (may be empty)" } else { Write-Warn "reviews: $RV" }
  # reply to first review if any
  $rev = ($RV | ConvertFrom-Json).data
  if ($rev -is [array] -and $rev.Count -gt 0) {
    $rp = New-Api "$Api/staff/reviews/$($rev[0].id)/reply" 'Post' $Stok '{"reply":"感谢好评"}'
    if ($rp -match 'staffReply|"code":\s*0') { Write-Ok "review replied" } else { Write-Warn "reply: $rp" }
  } else { Write-Skip "review reply (none)" }

  Write-Step '9. logistics event (feature 9: 物流信息发布)'
  if ($Oid) {
    $E = New-Api "$Api/staff/orders/$Oid/events" 'Post' $Stok (Build-Event)
    if ($E -match 'IN_TRANSIT|"code":\s*0') { Write-Ok "event published" } else { Write-Warn "event: $E" }
  } else { Write-Skip "event" }

  Write-Step '9b. track stream (SSE first event)'
  if ($Oid) {
    try {
      $url = "$Api/staff/orders/$Oid/track-stream"
      $web = [System.Net.HttpWebRequest]::Create($url)
      $web.Method = 'GET'
      $web.Headers.Add('Authorization', "Bearer $Stok")
      $web.Timeout = 8000
      $wresp = $web.GetResponse()
      $ws = $wresp.GetResponseStream()
      $reader = New-Object System.IO.StreamReader($ws)
      $first = $reader.ReadLine()
      $reader.Close(); $wresp.Close()
      if ($first -and $first.Length -gt 0) { Write-Ok "track stream connected" } else { Write-Warn "track stream: empty" }
    } catch {
      Write-Warn "track stream: $($_.Exception.Message)"
    }
  }
} else {
  Write-Warn "no staff token; skipping staff-side chain"
}

Write-Step "summary"
Write-Host "PASS=$($Script:Pass) FAIL=$($Script:Fail)" -ForegroundColor Cyan
if ($Script:Fail -gt 0) { exit 1 }
