output "alb_dns_name" {
  description = "DNS publico do servico"
  value       = aws_lb.this.dns_name
}

output "dynamodb_table_name" {
  description = "Tabela de controle de CNPJ"
  value       = aws_dynamodb_table.cnpj_product_control.name
}

output "ecs_cluster_name" {
  description = "Cluster ECS"
  value       = aws_ecs_cluster.this.name
}
