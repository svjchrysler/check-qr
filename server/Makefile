DB ?= postgres:///checkqr_dev?sslmode=disable
DB_TEST ?= postgres:///checkqr_test?sslmode=disable

.PHONY: migrar migrar-test correr test test-integracion claves fmt

migrar:
	@sed -n '/-- +goose Up/,/-- +goose Down/p' migraciones/001_inicial.sql \
	  | grep -v '^-- +goose' | psql -q -d checkqr_dev -v ON_ERROR_STOP=1
	@echo "esquema aplicado en checkqr_dev"

migrar-test:
	@dropdb --if-exists checkqr_test && createdb checkqr_test
	@sed -n '/-- +goose Up/,/-- +goose Down/p' migraciones/001_inicial.sql \
	  | grep -v '^-- +goose' | psql -q -d checkqr_test -v ON_ERROR_STOP=1
	@echo "base de test lista"

correr:
	DATABASE_URL="$(DB)" JWT_SECRET=desarrollo GOOGLE_CLIENT_ID=desarrollo go run ./cmd/api

test:
	go test ./...

test-integracion: migrar-test
	CHECKQR_TEST_DB="$(DB_TEST)" go test ./... -count=1

claves:
	go run ./cmd/firmar -generar-claves

fmt:
	gofmt -w .
