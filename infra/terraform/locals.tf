locals {
  name        = "${var.project_name}-${var.environment}"
  table_name  = "${local.name}-cnpj-product-control"
  health_path = "/actuator/health"
}
