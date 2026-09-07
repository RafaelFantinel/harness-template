resource "aws_dynamodb_table" "cnpj_product_control" {
  name         = local.table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "cnpj"
  range_key    = "regiao"

  attribute {
    name = "cnpj"
    type = "S"
  }

  attribute {
    name = "regiao"
    type = "S"
  }

  point_in_time_recovery {
    enabled = true
  }

  server_side_encryption {
    enabled = true
  }
}
