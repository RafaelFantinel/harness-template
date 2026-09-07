---

name: security-pr-checklist-skill
description: Cria um checklist de revisão de segurança reutilizável para Pull Requests (PRs), com verificações obrigatórias, armadilhas comuns e validações automatizadas. Use para "revisão de segurança", "checklist de PR", "code review" ou "gates de segurança".
---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

# Skill de Checklist de Segurança para PRs

Revisão de segurança padronizada para Pull Requests.

## Checklist de Segurança do PR

```markdown
## Checklist de Revisão de Segurança

### Autenticação e Autorização

- [ ] Não existem credenciais armazenadas diretamente no código
- [ ] Existem verificações de autorização em todos os endpoints
- [ ] O gerenciamento de sessão é seguro
- [ ] Existe limitação de requisições (rate limiting) nos endpoints de autenticação

### Validação de Entrada

- [ ] Todas as entradas são devidamente validadas
- [ ] As saídas são devidamente codificadas (output encoding)
- [ ] Não existem riscos de SQL Injection
- [ ] Não existem vulnerabilidades de XSS

### Proteção de Dados

- [ ] Dados sensíveis são criptografados em repouso
- [ ] HTTPS é obrigatório
- [ ] Não existem dados pessoais (PII) nos logs
- [ ] Os cookies possuem configuração segura

### Dependências

- [ ] Não foram introduzidas vulnerabilidades novas de severidade alta ou crítica
- [ ] As dependências estão atualizadas
- [ ] Não existem pacotes suspeitos ou não confiáveis

### Gerenciamento de Segredos

- [ ] Não existem segredos ou credenciais no código
- [ ] Variáveis de ambiente são utilizadas para configurações sensíveis
- [ ] Arquivos `.env` estão incluídos no `.gitignore`

### Tratamento de Erros

- [ ] Mensagens de erro não expõem informações sensíveis
- [ ] Mensagens de erro são genéricas quando apropriado
- [ ] O registro de logs está implementado corretamente
```

## Checklist de Entrega

* [ ] Template de PR criado
* [ ] Verificações de segurança obrigatórias definidas
* [ ] Armadilhas e problemas comuns documentados
* [ ] Verificações automatizadas configuradas no CI
* [ ] Diretrizes de revisão definidas
