# Aula 40 — Anti-Corruption Layer e contratos de API · SOLUÇÃO

Formação Backend Java e Quarkus · Turma 2 · 28/10/2026

Dois projetos Quarkus 3.17 / Java 21: `legado-mock/` (o sistema legado, porta 4001) e `acl/` (a API nova, porta 4000) com a prática guiada e o desafio resolvidos: `ClienteMapper` implementado, `ClienteAdapter` com REST Client e classificação de falhas (404 negócio · 502 tradução · 503 transporte), `ExceptionMapper`s, validação do id, OpenAPI e **8 testes verdes** (`./mvnw test`).

Em relação ao starter mudam só dois arquivos: `adapter/ClienteMapper.java` (implementado) e `adapter/ClienteAdapter.java` (novo). Compare-os lado a lado no VS Code (*Select for Compare*).

## Como rodar (Java 21 + Maven Wrapper incluso; não precisa instalar Maven)

Dois terminais, um por projeto:

```bash
cd legado-mock && ./mvnw quarkus:dev        # legado simulado em http://localhost:4001
cd acl         && ./mvnw quarkus:dev        # API nova em http://localhost:4000  (tecla r = testes contínuos)
```

No Windows sem WSL use `mvnw.cmd` no lugar de `./mvnw`. Quem preferir container: `docker compose up -d && docker compose exec dev bash` e os mesmos comandos dentro de `/workspace`.

Postman: *Import* → `postman/FIAP-Aula-40.postman_collection.json` e `postman/FIAP-Aula-40-local.postman_environment.json`; selecione o ambiente "FIAP Aula 40 — local". O *Runner* na coleção inteira é o critério de aceite: tudo verde, entregou.

Swagger UI: http://localhost:4000/q/swagger-ui · Dev UI: http://localhost:4000/q/dev-ui

## Cenários do legado (`GET http://localhost:4001/cadastro/{id}`)

| id | Legado devolve | ACL deve responder |
|---|---|---|
| 1 | `SIT=A`, nome com espaços | 200 `{"id":1,"nome":"Ana Souza","ativo":true}` |
| 2 | `SIT=I` | 200 `ativo=false` |
| 3 | `SIT=X` | 502 `resposta-invalida-do-legado` |
| 4 | HTTP 404 | 404 `cliente-nao-encontrado` |
| 5 | corpo que não é JSON | 502 |
| 6 | demora 5 s (> read-timeout 2 s) | 503 `legado-indisponivel` |
| 7 | `COD_CLI=ABC` | 502 |
| 8 | `NOME` vazio | 502 |
| 9 | campo extra `DT_CAD` | 200 (tolerant reader) |
| abc | — (validação na ACL) | 400 `id deve ser numérico` |
