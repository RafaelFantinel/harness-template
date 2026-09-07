resource "aws_security_group" "alb" {
  name        = "${local.name}-alb"
  description = "Trafego de entrada do ALB"
  vpc_id      = var.vpc_id

  ingress {
    description = "HTTP de qualquer origem"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    description = "Saida liberada"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "service" {
  name        = "${local.name}-service"
  description = "Tasks ECS do eligibility-service"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Somente do ALB"
    from_port       = var.container_port
    to_port         = var.container_port
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  egress {
    description = "Saida para DynamoDB e servico de toggles"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}
