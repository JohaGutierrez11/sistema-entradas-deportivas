#!/bin/bash
# Crea las tres bases del sistema y ejecuta su esquema y sus semillas.
# PostgreSQL ejecuta este archivo UNA sola vez: cuando el volumen de datos está vacío.
set -e

for db in auth_db match_db reservation_db; do
  echo "== Preparando $db"
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -c "CREATE DATABASE $db"
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$db" -f "/database/$db/01_schema.sql"
  psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$db" -f "/database/$db/02_seed.sql"
done