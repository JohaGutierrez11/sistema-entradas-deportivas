<#
.SYNOPSIS
    Levanta PostgreSQL 16 en Docker y prepara las tres bases del sistema:
    crea las bases, las tablas, carga los datos semilla y corre todas las pruebas.

.DESCRIPTION
    Pasos:
      1. Crea (o enciende) el contenedor pg-entradas en el puerto 5433.
      2. Crea auth_db, match_db y reservation_db si no existen.
      3. Ejecuta 01_schema.sql y 02_seed.sql de cada base.
      4. Ejecuta las verificaciones de semillas y las pruebas de restricciones.
    Se puede ejecutar varias veces: los scripts vacían y recargan los datos.

.EXAMPLE
    cd database
    .\setup-db.ps1            # resúmenes por archivo y total general
    .\setup-db.ps1 -Detail    # además, cada caso PASS

    Si PowerShell bloquea el script por la política de ejecución:
    powershell -ExecutionPolicy Bypass -File .\setup-db.ps1
#>
param(
    [string]$Container = "pg-entradas",
    [int]$Port = 5433,
    # Muestra cada caso PASS, no solo los resúmenes y los fallos
    [switch]$Detail
)

# Mostrar bien tildes y símbolos que devuelve PostgreSQL (UTF-8)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$script:TotalCases = 0
$script:TotalPass  = 0
$script:TotalFail  = 0

$Databases = @("auth_db", "match_db", "reservation_db")

function Invoke-Psql([string]$Database, [string]$File) {
    Write-Host "  -> $Database : $File" -ForegroundColor Cyan
    # 2>&1 dentro del contenedor: los NOTICE de las pruebas llegan como texto normal
    $output = docker exec -w /database $Container sh -c "psql -U postgres -d $Database -q -v ON_ERROR_STOP=1 -f $File 2>&1"
    $exitCode = $LASTEXITCODE

    foreach ($line in $output) {
        $text = ("$line" -replace '^psql:\S+ (NOTICE|ERROR):\s+', '').Trim()
        if (-not $text) { continue }
        if ($text -match '(\d+) casos\D+(\d+) PASS\D+(\d+) FAIL') {
            $script:TotalCases += [int]$Matches[1]
            $script:TotalPass  += [int]$Matches[2]
            $script:TotalFail  += [int]$Matches[3]
            Write-Host "     $text" -ForegroundColor Green
        } elseif ($text -match '^FAIL' -or "$line" -match 'ERROR') {
            Write-Host "     $text" -ForegroundColor Red
        } elseif ($Detail) {
            Write-Host "     $text" -ForegroundColor DarkGray
        }
    }

    if ($exitCode -ne 0) {
        Write-Host "ERROR ejecutando $File en $Database" -ForegroundColor Red
        exit 1
    }
}

# 1. Contenedor -------------------------------------------------------
$existing = docker ps -a --filter "name=^${Container}$" --format "{{.Names}}"
if (-not $existing) {
    Write-Host "Creando el contenedor $Container (PostgreSQL 16, puerto $Port)..." -ForegroundColor Yellow
    docker run -d --name $Container -e POSTGRES_PASSWORD=postgres -p "${Port}:5432" -v "${PSScriptRoot}:/database" postgres:16 | Out-Null
    $firstRun = $true
} else {
    Write-Host "Encendiendo el contenedor $Container..." -ForegroundColor Yellow
    docker start $Container | Out-Null
    $firstRun = $false
}
if ($LASTEXITCODE -ne 0) { Write-Host "No se pudo iniciar Docker. ¿Está abierto Docker Desktop?" -ForegroundColor Red; exit 1 }

# Esperar a que PostgreSQL acepte conexiones
Write-Host "Esperando a PostgreSQL..." -ForegroundColor Yellow
for ($i = 0; $i -lt 30; $i++) {
    docker exec $Container pg_isready -U postgres -q 2>$null
    if ($LASTEXITCODE -eq 0) { break }
    Start-Sleep -Seconds 1
}
# En la primera ejecución el contenedor se reinicia una vez tras inicializarse.
if ($firstRun) { Start-Sleep -Seconds 4 }

# 2. Bases de datos ---------------------------------------------------
foreach ($db in $Databases) {
    $found = docker exec $Container psql -U postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$db'"
    if (-not $found) {
        Write-Host "Creando la base $db" -ForegroundColor Yellow
        docker exec $Container psql -U postgres -q -c "CREATE DATABASE $db"
    }
}

# 3. Esquema y datos semilla ------------------------------------------
Write-Host "`nCreando tablas y cargando datos semilla" -ForegroundColor Yellow
foreach ($db in $Databases) {
    Invoke-Psql $db "$db/01_schema.sql"
    Invoke-Psql $db "$db/02_seed.sql"
}

# 4. Pruebas ----------------------------------------------------------
Write-Host "`nVerificando datos semilla y restricciones" -ForegroundColor Yellow
foreach ($db in $Databases) {
    Invoke-Psql $db "tests/${db}_seed_checks.sql"
    Invoke-Psql $db "tests/${db}_tests.sql"
}

Write-Host ""
Write-Host "================================================" -ForegroundColor Green
Write-Host " TOTAL: $script:TotalCases casos · $script:TotalPass PASS · $script:TotalFail FAIL" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host "`nListo. Conecta DBeaver a localhost:$Port (usuario postgres / clave postgres)." -ForegroundColor Green
