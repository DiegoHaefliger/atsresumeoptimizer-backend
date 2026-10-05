# atsresumeoptimizer-backend

Backend do [ATSResumeOptimizer](https://github.com/DiegoHaefliger/atsresumeoptimizer): sistema que adapta currículos para aumentar a chance de passar nos filtros de ATS.

## Escopo

- Cadastra e importa currículos (PDF ou DOCX, até 2 MB), com versionamento e retenção automática (LGPD)
- Opcionalmente recebe a descrição da vaga (texto colado, sem scraping de URL)
- Avalia ortografia, semântica e aderência ao ATS, mais preferências do candidato sobre a vaga
- Reescreve o currículo para a vaga, com guard rails determinísticos, e exporta em PDF e DOCX
- Currículos apenas em PT-BR
- Sem freemium: cobrança por análise (fora do MVP, entra após a fase de testes)

## Stack

- Java 25 (LTS), Spring Boot, Liquibase
- PostgreSQL e MinIO (subem via `compose.yaml`)
- IA configurável pela tela de Configurações: OpenAI, Anthropic, Gemini ou Ollama

## Estrutura

Um pacote por contexto (`ai`, `analysis`, `job`, `language`, `parsing`, `preference`, `resume`, `rewrite`,
`scoring`), cada um dividido em `adapter`, `application` e `domain`.

## Como rodar

Veja o passo a passo no [README do repositório principal](https://github.com/DiegoHaefliger/atsresumeoptimizer#como-rodar).

## Contrato de API

O contrato é exposto via OpenAPI em `/v3/api-docs` (Swagger UI em `/swagger-ui.html`). O front gera os tipos a
partir dele.
