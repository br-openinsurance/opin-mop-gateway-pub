$ErrorActionPreference = 'Stop'
$headers = @{ 'User-Agent' = 'opin-mop-client-gateway-spec-sync' }
$base = Join-Path $PSScriptRoot '..\src\main\resources\swagger'
$currentDir = Join-Path $base 'current'
$versionDir = Join-Path $base 'version'
New-Item -ItemType Directory -Force -Path $versionDir | Out-Null

function Get-ApiVersionFromYaml([string]$text) {
  if ($text -match 'open-insurance/[^/\s''"]+/v(\d+)') { return [int]$Matches[1] }
  return $null
}
function Get-ApiKeyFromFileName([string]$name) {
  $stem = [IO.Path]::GetFileNameWithoutExtension($name)
  if ($stem -match '^(.*)_v\d+$') { return $Matches[1] }
  return $stem
}

$currentMax = @{}
Get-ChildItem $currentDir -Filter *.yaml | ForEach-Object {
  $key = Get-ApiKeyFromFileName $_.Name
  $text = [IO.File]::ReadAllText($_.FullName)
  $ver = Get-ApiVersionFromYaml $text
  if ($null -eq $ver) { $ver = 1 }
  if (-not $currentMax.ContainsKey($key) -or $ver -gt $currentMax[$key]) {
    $currentMax[$key] = $ver
  }
}

$list = Invoke-RestMethod -Headers $headers -Uri 'https://api.github.com/repos/br-openinsurance/areadesenvolvedor/contents/documentation/source/files/swagger'
$downloaded = New-Object System.Collections.Generic.List[string]
$skipped = New-Object System.Collections.Generic.List[string]

foreach ($item in $list) {
  if ($item.name -notmatch '\.ya?ml$') { continue }
  $key = Get-ApiKeyFromFileName $item.name
  Write-Host "fetch $($item.name) ..."
  $resp = Invoke-WebRequest -Headers $headers -Uri $item.download_url -UseBasicParsing
  $text = $resp.Content
  $ver = Get-ApiVersionFromYaml $text
  if ($null -eq $ver) {
    $skipped.Add("$($item.name) | no /vN in servers")
    continue
  }
  if (-not $currentMax.ContainsKey($key)) {
    $skipped.Add("$($item.name) | API not in current")
    continue
  }
  $max = $currentMax[$key]
  if ($ver -ge $max) {
    $skipped.Add("$($item.name) | v$ver >= current v$max")
    continue
  }
  $destName = "${key}_v$ver.yaml"
  $dest = Join-Path $versionDir $destName
  [IO.File]::WriteAllText($dest, $text)
  $downloaded.Add("$destName (github $($item.name), v$ver < current v$max)")
}

$report = New-Object System.Collections.Generic.List[string]
$report.Add('# swagger/version')
$report.Add('')
$report.Add('Arquivo oficial de versoes **abaixo** da que ja esta em `swagger/current/`.')
$report.Add('Fonte: https://github.com/br-openinsurance/areadesenvolvedor/tree/main/documentation/source/files/swagger')
$report.Add('Arquivos na raiz de `swagger/version/` (ex.: `version/customers_v1.yaml`) para facilitar a busca.')
$report.Add('Nao e indexado pelo gateway (apenas `swagger/current/` entra na validacao).')
$report.Add('')
$report.Add('## Baixados')
$report.Add('')
if ($downloaded.Count -eq 0) { $report.Add('- (nenhum)') } else { $downloaded | ForEach-Object { $report.Add("- $_") } }
$report.Add('')
$report.Add('## Ignorados')
$report.Add('')
$skipped | ForEach-Object { $report.Add("- $_") }
$report.Add('')
$report.Add('## Versao corrente em current/')
$report.Add('')
$currentMax.GetEnumerator() | Sort-Object Name | ForEach-Object { $report.Add("- $($_.Key): v$($_.Value)") }
[IO.File]::WriteAllText((Join-Path $versionDir 'README.md'), ($report -join "`n"))
Write-Host '--- DOWNLOADED ---'
$downloaded | ForEach-Object { Write-Host $_ }
Write-Host "count downloaded=$($downloaded.Count) skipped=$($skipped.Count)"
