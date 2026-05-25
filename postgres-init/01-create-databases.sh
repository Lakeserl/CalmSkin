#!/bin/bash
# Creates one PostgreSQL database per microservice inside the shared
# `postgres` container. Runs once, only when the data volume is empty
# (Docker entrypoint executes /docker-entrypoint-initdb.d/* on first init).
#
# Database names are taken from the POSTGRES_DB_* environment variables
# passed to the container in docker-compose.yml. An unset variable is
# skipped, so a service can be added later without editing this script.
set -e

create_database() {
    local db_name="$1"
    if [ -z "$db_name" ]; then
        return 0
    fi
    echo "postgres-init: ensuring database '$db_name'"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
        SELECT 'CREATE DATABASE "$db_name"'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db_name')\gexec
EOSQL
}

create_database "$POSTGRES_DB_USER_SERVICE"
create_database "$POSTGRES_DB_PRODUCT_SERVICE"
create_database "$POSTGRES_DB_INVENTORY_SERVICE"
create_database "$POSTGRES_DB_ORDER_SERVICE"
create_database "$POSTGRES_DB_PAYMENT_SERVICE"
create_database "$POSTGRES_DB_NOTIFICATION_SERVICE"
create_database "$POSTGRES_DB_PROMOTION_SERVICE"
create_database "$POSTGRES_DB_REVIEW_SERVICE"
create_database "$POSTGRES_DB_SHIPPING_SERVICE"

echo "postgres-init: database provisioning complete"
