## Git

- Nunca fazer commit diretamente nas branches `main` ou `dev`.
- Toda alteração deve ser desenvolvida em uma branch de trabalho.
- Todo commit deve seguir a skill `git-commit`.
- Não fazer push diretamente para `main` ou `dev`.
- Todo commit deve incluir:
  `Co-authored-by: Devin <devin@cognition.ai>`
- Nunca criar um commit sem o coautor Devin.
- Não fazer commits fora do escopo da tarefa solicitada.
- Toda branch deve começar com `feature/descricao-breve`, independente do tipo.
- Não fazer force push

## Segurança

- Nunca expor secrets, tokens ou credenciais.
- Nunca inserir credenciais diretamente no código.
- Não desabilitar mecanismos de segurança para contornar erros.
- Não commitar arquivos `.env` contendo credenciais.
- Dados sensíveis devem utilizar os mecanismos de configuração/secret management existentes.
- Nunca ignore as proteções do repositório ou os processos de revisão obrigatórios.


## Qualidade

- Toda mudança de código deve possuir testes apropriados.
- Nenhum PR pode ignorar os checks obrigatórios.

## Arquitetura

- Respeitar a separação entre domínio, aplicação e infraestrutura.
- Não acessar diretamente a infraestrutura a partir da camada de domínio.
- Reutilizar serviços existentes antes de criar novos.
- Não duplicar regras de negócio.
- Novas dependências devem ser justificadas antes de adicionadas.


