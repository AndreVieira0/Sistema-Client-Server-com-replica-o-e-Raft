# 🖧 Sistema Cliente-Servidor com Replicação e Eleição de Líder (Raft)

---

## 📋 Sobre o Projeto

Sistema distribuído implementado em **Java com Spring Boot**, composto por 5 módulos independentes que simulam uma arquitetura cliente-servidor com:

- ✅ Replicação de dados entre múltiplos servidores
- ✅ Detecção automática de falhas via health check
- ✅ Eleição de líder inspirada no algoritmo **Raft**
- ✅ Sincronização automática no boot (failback)
- ✅ Persistência em arquivos `.txt` por nó

---

## 🏗️ Arquitetura

```
ClientApp (terminal interativo)
         |
         v
  Gateway Service :8090  ←── health check + votação Raft a cada 5s
         |
         v
  Server Primary :8080  ──→  Server Replica 1 :8081
  (prioridade 0)   └──────→  Server Replica 2 :8082
                              (prioridade 2)
```

### Módulos

| Módulo | Porta | Prioridade | Arquivo de Dados |
|--------|-------|------------|-----------------|
| `client-app` | — | — | — |
| `gateway-service` | 8090 | — | — |
| `server-primary` | 8080 | 0 (maior) | `database.txt` |
| `server-replica` | 8081 | 1 | `database-replica.txt` |
| `server-replica2` | 8082 | 2 | `database-replica2.txt` |

---

## ⚙️ Tecnologias

- Java 17
- Spring Boot 4.0.3
- Gradle 9.x
- RestTemplate (comunicação entre serviços)
- Lombok

---

## 🗳️ Algoritmo de Eleição — Inspirado no Raft

O Gateway implementa os seguintes conceitos do Raft:

### Conceitos implementados

| Conceito | Descrição |
|----------|-----------|
| **Termo (Term)** | Contador global incrementado a cada eleição. Garante que votos antigos sejam descartados. |
| **Candidatura** | Quando o líder cai, o Gateway inicia rodada de votação perguntando a cada nó: "você aceita este candidato?" |
| **Votação (`POST /vote`)** | Cada nó responde com `true` (SIM) ou `false` (NÃO). O candidato sempre vota em si mesmo. |
| **Quorum** | O candidato precisa de `ceil(n/2) + 1` votos para ser eleito. Com 2 nós vivos, precisa de 2 votos. |

### Fluxo da eleição

```
1. Gateway detecta que o líder caiu via health check
2. Incrementa o termo: currentTerm++
3. Para cada nó vivo (por ordem de prioridade):
   a. Envia POST /vote com { candidateUrl, term }
   b. Conta os votos recebidos + voto próprio
   c. Se votos >= quorum → ELEITO
   d. Senão → tenta o próximo candidato
```

### Log real capturado durante os testes

```
[Gateway-Raft] *** Iniciando eleicao — Termo 12 ***
[Gateway-Raft] Nos vivos: 2 | Quorum necessario: 2
[Gateway-Raft] Candidato: http://localhost:8081 (prioridade 1)
[Gateway-Raft] Voto SIM de http://localhost:8082
[Gateway-Raft] Votos recebidos: 2 | Quorum: 2
[Gateway-Raft] >> ELEITO: http://localhost:8081 com 2 voto(s) no Termo 12
```

---

## 🔄 Mecanismo de Replicação

Quando o líder recebe um `POST /users`:

1. Persiste localmente no arquivo `.txt`
2. Replica para Replica 1 via `POST /replica/users`
3. Replica para Replica 2 via `POST /replica/users`
4. Cada replicação está em `try-catch` independente — réplica offline não interrompe o fluxo

---

## 🔁 Sincronização no Boot (Failback)

Quando um nó reinicia, o `SyncOnStartupService` executa no `@PostConstruct`:

1. Consulta `GET /leader` no Gateway
2. Se for o próprio líder → nenhuma ação
3. Se for outro nó → baixa o `.txt` via `GET /db/download`
4. Substitui o arquivo local pelo conteúdo recebido
5. Só então começa a aceitar requisições

---

## 🚀 Como Executar

### Pré-requisitos

- Java 17
- IntelliJ IDEA

### Ordem de inicialização (obrigatória)

```
1º → gateway-service   (GatewayApplication.java)      porta 8090
2º → server-primary    (ServerPrimaryApplication.java) porta 8080
3º → server-replica    (ServerReplicaApplication.java) porta 8081
4º → server-replica2   (ServerReplicaApplication.java) porta 8082
5º → client-app        (ClientApp.java)
```

> ⚠️ **server-replica2**: caso o IntelliJ não reconheça o botão ▶, use o terminal integrado (`Alt+F12`) e execute `.\gradlew.bat bootRun`

### Limpando dados entre testes

```powershell
Remove-Item database.txt -ErrorAction SilentlyContinue          # server-primary
Remove-Item database-replica.txt -ErrorAction SilentlyContinue  # server-replica
Remove-Item database-replica2.txt -ErrorAction SilentlyContinue # server-replica2
```

---

## 🧪 Roteiro de Testes

### Teste 1 — Sistema normal
- Cadastra usuários pelo Client App (opção 1)
- Lista os usuários (opção 2)
- Verifica que os 3 arquivos `.txt` estão idênticos

### Teste 2 — Derrubar a primária
- Para o `server-primary` (botão ⏹)
- Aguarda 10 segundos
- Verifica no Client App (opção 3) que a eleição Raft elegeu a Replica 1

### Teste 3 — Enviar dado com failover ativo
- Com a primária offline, cadastra novo usuário
- Sistema continua funcionando normalmente

### Teste 4 — Failback
- Sobe a primária novamente
- Ela sincroniza automaticamente os dados do líder atual
- Todos os usuários aparecem na listagem

---

## 🌐 Endpoints Principais

| Endpoint | Método | Descrição |
|----------|--------|-----------|
| `localhost:8090/users` | POST | Cadastra usuário (sempre pelo Gateway) |
| `localhost:8090/users` | GET | Lista todos os usuários |
| `localhost:8090/status` | GET | Estado do cluster + termo Raft atual |
| `localhost:8090/leader` | GET | URL do líder atual |
| `localhost:808X/health` | GET | Health check de cada nó |
| `localhost:808X/vote` | POST | Endpoint de votação Raft |
| `localhost:808X/db/download` | GET | Download do banco para sincronização |

---

## 📁 Estrutura do Projeto

```
Sistema-Client-Server/
├── client-app/
│   └── ClientApp.java              # Menu interativo no terminal
├── gateway-service/
│   └── GatewayController.java      # Health check + eleição Raft
├── server-primary/
│   ├── UserController.java         # Endpoints REST
│   ├── UserService.java            # Lógica de negócio + replicação
│   ├── UserRepository.java         # Persistência em .txt
│   └── SyncOnStartupService.java   # Sincronização no boot
├── server-replica/
│   ├── ReplicaController.java      # Endpoints + votação Raft
│   ├── UserRepositoryReplica.java  # Persistência em .txt
│   └── SyncOnStartupService.java   # Sincronização no boot
└── server-replica2/
    ├── ReplicaController.java
    ├── UserRepositoryReplica2.java
    └── SyncOnStartupService.java
```
