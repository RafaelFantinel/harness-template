variable "aws_region" {
  description = "Regiao AWS de deploy"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Nome base dos recursos"
  type        = string
  default     = "eligibility-service"
}

variable "environment" {
  description = "Ambiente (dev, hml, prd)"
  type        = string
  default     = "dev"
}

variable "vpc_id" {
  description = "VPC onde o servico roda"
  type        = string
}

variable "public_subnet_ids" {
  description = "Subnets publicas do ALB"
  type        = list(string)
}

variable "private_subnet_ids" {
  description = "Subnets privadas das tasks ECS"
  type        = list(string)
}

variable "container_image" {
  description = "Imagem do container (ECR)"
  type        = string
}

variable "container_port" {
  description = "Porta exposta pela aplicacao"
  type        = number
  default     = 8080
}

variable "desired_count" {
  description = "Numero de tasks desejado"
  type        = number
  default     = 2
}

variable "task_cpu" {
  description = "CPU da task Fargate"
  type        = number
  default     = 512
}

variable "task_memory" {
  description = "Memoria da task Fargate em MiB"
  type        = number
  default     = 1024
}

variable "toggle_base_url" {
  description = "URL do servico externo de toggles"
  type        = string
}

variable "log_retention_days" {
  description = "Retencao dos logs no CloudWatch"
  type        = number
  default     = 30
}
