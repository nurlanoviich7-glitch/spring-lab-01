$ErrorActionPreference = 'Stop'
$lab8Root = Join-Path $PSScriptRoot '.lab8-runtime'
$pgBin = Join-Path $lab8Root 'tools\pgsql\bin'
$pgData = Join-Path $lab8Root 'runtime\pgdata'
$runtimeDir = Join-Path $lab8Root 'runtime'
if (-not (Test-Path -LiteralPath (Join-Path $pgBin 'initdb.exe'))) { throw 'Install PostgreSQL 16 or newer, use compose.yml, or provide the prepared .lab8-runtime folder.' }
New-Item -ItemType Directory -Path $runtimeDir -Force | Out-Null
$passwordFile = Join-Path $runtimeDir 'bootstrap-password.txt'
if (-not (Test-Path -LiteralPath (Join-Path $pgData 'PG_VERSION'))) {
    Set-Content -LiteralPath $passwordFile -Value 'springlab' -NoNewline -Encoding ascii
    & (Join-Path $pgBin 'initdb.exe') -D $pgData -U springlab --pwfile=$passwordFile -A scram-sha-256 --locale=C --encoding=UTF8
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL initialisation failed' }
    Remove-Item -LiteralPath $passwordFile
}
& (Join-Path $pgBin 'pg_ctl.exe') -D $pgData status
if ($LASTEXITCODE -ne 0) {
    & (Join-Path $pgBin 'pg_ctl.exe') -D $pgData -l (Join-Path $runtimeDir 'postgresql.log') -o '-h 127.0.0.1 -p 5438' -w -t 60 start
    if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL startup failed' }
}
$env:PGPASSWORD = 'springlab'
foreach ($database in @('springlab','springlab_test')) {
    $existing = & (Join-Path $pgBin 'psql.exe') -h 127.0.0.1 -p 5438 -U springlab -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$database'"
    if ($existing -ne '1') {
        & (Join-Path $pgBin 'createdb.exe') -h 127.0.0.1 -p 5438 -U springlab $database
        if ($LASTEXITCODE -ne 0) { throw "Database creation failed: $database" }
    }
}
& (Join-Path $pgBin 'psql.exe') -h 127.0.0.1 -p 5438 -U springlab -d springlab -c 'select version(), current_database(), current_user;'
