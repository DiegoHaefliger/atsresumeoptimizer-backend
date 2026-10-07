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
`scoring`, `selection`, `notification`, `googlecalendar`), cada um dividido em `adapter`, `application` e `domain`.

## Como rodar

Veja o passo a passo no [README do repositório principal](https://github.com/DiegoHaefliger/atsresumeoptimizer#como-rodar).

## Contrato de API

O contrato é exposto via OpenAPI em `/v3/api-docs` (Swagger UI em `/swagger-ui.html`). O front gera os tipos a
partir dele.

## API de integração

`POST /api/v1/integrations/jobs` cadastra vagas para sistemas externos. Exige o header `X-API-Key` com o valor
da variável de ambiente `INTEGRATION_API_KEY`. Sem a variável definida, toda rota `/api/v1/integrations/**`
responde 401. O payload é o mesmo de `POST /api/v1/jobs`.

```bash
curl -X POST http://localhost:8080/api/v1/integrations/jobs \
  -H "X-API-Key: $INTEGRATION_API_KEY" -H "Content-Type: application/json" \
  -d '{"text":"Vaga de backend Java...","company":"Acme","sourceUrl":"https://exemplo.com/vaga/1"}'
```

## Google Agenda (opcional)

Com a integração configurada, a aba Notificações das Configurações mostra o botão **Conectar com o Google**:
o usuário entra com a conta Google, autoriza o acesso à agenda e os agendamentos dos processos passam a virar
eventos no Google Agenda. Sem a integração, o botão aparece desativado e a agenda do app continua funcionando.

O Google só abre a tela de login para aplicativos registrados, então quem instala o app faz este registro
uma única vez (o Google não oferece API para isso):

1. Acesse o [Google Cloud Console](https://console.cloud.google.com) e crie um projeto (ou use um existente).
2. Em **APIs e serviços > Biblioteca**, ative a **Google Calendar API**.
3. Em **APIs e serviços > Tela de consentimento OAuth** (ou **Google Auth Platform**), escolha o tipo
   **Externo** e informe nome do app e e-mail de suporte.
4. Ainda na tela de consentimento, em **Público-alvo > Usuários de teste**, clique em **+ Adicionar usuários** e
   inclua o e-mail de cada conta que vai conectar a agenda. Sem isso o login falha com `403: access_denied`.
5. Em **APIs e serviços > Credenciais > Criar credenciais > ID do cliente OAuth**, escolha **Aplicativo da Web**.
6. No mesmo formulário, na seção **URIs de redirecionamento autorizados** (não em "Origens JavaScript
   autorizadas"), adicione o endereço do backend seguido de `/api/v1/google-calendar/callback`
   (local: `http://localhost:8080/api/v1/google-calendar/callback`) e salve. Endereço diferente gera
   `400: redirect_uri_mismatch`; a mudança pode levar alguns minutos para valer.
7. Copie o ID do cliente e a chave secreta para o `.env` do backend e reinicie:

```properties
GOOGLE_CLIENT_ID=123456789-abc.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=GOCSPX-...
# Fora do ambiente local, informe também os endereços públicos:
# GOOGLE_REDIRECT_URI=https://api.seudominio.com/api/v1/google-calendar/callback
# FRONTEND_URL=https://app.seudominio.com
```

Enquanto o app estiver em modo de teste no Google, a conexão expira a cada 7 dias e precisa ser refeita; para
evitar, publique o app na tela de consentimento (o escopo de agenda exige verificação do Google).

## Licença

[PolyForm Noncommercial 1.0.0](LICENSE). Uso, estudo e modificação livres para fins não comerciais. Vender,
revender ou embutir em produto ou serviço pago não é permitido.
