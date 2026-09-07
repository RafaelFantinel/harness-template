#!/usr/bin/env bash
# Cria e popula a tabela de controle de CNPJ no LocalStack.
set -euo pipefail

TABLE_NAME="${DYNAMO_TABLE_NAME:-cnpj-product-control}"
ENDPOINT="${DYNAMO_ENDPOINT:-http://localhost:4566}"

awslocal dynamodb create-table \
  --table-name "$TABLE_NAME" \
  --attribute-definitions AttributeName=cnpj,AttributeType=S AttributeName=regiao,AttributeType=S \
  --key-schema AttributeName=cnpj,KeyType=HASH AttributeName=regiao,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url "$ENDPOINT"

awslocal dynamodb put-item \
  --table-name "$TABLE_NAME" \
  --item '{"cnpj":{"S":"12345678000195"},"regiao":{"S":"SUDESTE"},"allowed":{"BOOL":true}}' \
  --endpoint-url "$ENDPOINT"

awslocal dynamodb put-item \
  --table-name "$TABLE_NAME" \
  --item '{"cnpj":{"S":"98765432000188"},"regiao":{"S":"SUDESTE"},"allowed":{"BOOL":false}}' \
  --endpoint-url "$ENDPOINT"
