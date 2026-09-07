data "aws_iam_policy_document" "ecs_assume_role" {
  statement {
    actions = ["sts:AssumeRole"]

    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "task_execution" {
  name               = "${local.name}-task-execution"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

resource "aws_iam_role_policy_attachment" "task_execution_managed" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "task" {
  name               = "${local.name}-task"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume_role.json
}

# Menor privilegio: leitura apenas da tabela de controle de CNPJ.
data "aws_iam_policy_document" "task_dynamodb_read" {
  statement {
    actions   = ["dynamodb:GetItem", "dynamodb:Query"]
    resources = [aws_dynamodb_table.cnpj_product_control.arn]
  }
}

resource "aws_iam_role_policy" "task_dynamodb_read" {
  name   = "${local.name}-dynamodb-read"
  role   = aws_iam_role.task.id
  policy = data.aws_iam_policy_document.task_dynamodb_read.json
}
