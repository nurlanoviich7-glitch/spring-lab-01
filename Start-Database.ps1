$ErrorActionPreference='Stop'
$lab9PreviousProject=if($env:LAB8_PROJECT){$env:LAB8_PROJECT}else{Join-Path $env:USERPROFILE 'Downloads\spring-lab-01\Spring lab8'}
$lab9Start=Join-Path $lab9PreviousProject 'Start-Database.ps1'
if(-not(Test-Path -LiteralPath $lab9Start)){throw 'Use the included compose.yml or provide LAB8_PROJECT for the prepared portable PostgreSQL server.'}
& $lab9Start
$lab9Bin=Join-Path $lab9PreviousProject '.lab8-runtime\tools\pgsql\bin'
$env:PGPASSWORD='springlab'
foreach($lab9Database in @('txlab9','txlab9_test','txlab9_verify')){
    $lab9Exists=& (Join-Path $lab9Bin 'psql.exe') -h 127.0.0.1 -p 5438 -U springlab -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$lab9Database'"
    if($lab9Exists -ne '1'){
        & (Join-Path $lab9Bin 'createdb.exe') -h 127.0.0.1 -p 5438 -U springlab $lab9Database
        if($LASTEXITCODE -ne 0){throw "Could not create $lab9Database"}
    }
}
Write-Output 'Lab 9 PostgreSQL databases ready on 127.0.0.1:5438'
