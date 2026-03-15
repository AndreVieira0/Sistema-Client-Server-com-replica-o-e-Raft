package com.unifor.br.gateway.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class GatewayController {

    private final RestTemplate restTemplate = new RestTemplate();

    private static class Node {
        String url;
        int priority;
        boolean alive;

        Node(String url, int priority) {
            this.url = url;
            this.priority = priority;
            this.alive = false;
        }
    }

    private final List<Node> nodes = new ArrayList<>(List.of(
            new Node("http://localhost:8080", 0),
            new Node("http://localhost:8081", 1),
            new Node("http://localhost:8082", 2)
    ));

    private volatile Node currentLeader = null;
    private final AtomicInteger currentTerm = new AtomicInteger(0);
    private volatile boolean electionInProgress = false;

    @PostConstruct
    public void init() {
        System.out.println("[Gateway-Raft] Iniciado. Termo atual: " + currentTerm.get());
    }

    @Scheduled(fixedDelay = 5000)
    public void checkHealth() {
        boolean liderCaiu = false;

        for (Node node : nodes) {
            boolean eraVivo = node.alive;
            try {
                restTemplate.getForObject(node.url + "/health", String.class);
                node.alive = true;
            } catch (Exception e) {
                node.alive = false;
                if (eraVivo) {
                    System.err.println("[Gateway-Raft] No offline: " + node.url);
                }
            }

            if (currentLeader != null
                    && currentLeader.url.equals(node.url)
                    && !node.alive) {
                liderCaiu = true;
            }
        }

        if ((currentLeader == null || liderCaiu) && !electionInProgress) {
            iniciarEleicao();
        }
    }

    private void iniciarEleicao() {
        electionInProgress = true;

        int novoTermo = currentTerm.incrementAndGet();
        System.out.println("[Gateway-Raft] *** Iniciando eleicao — Termo " + novoTermo + " ***");

        long nosVivos = nodes.stream().filter(n -> n.alive).count();
        int quorum = (int) Math.ceil((double) nosVivos / 2) + 1;
        if (nosVivos == 0) quorum = 1;

        System.out.println("[Gateway-Raft] Nos vivos: " + nosVivos
                + " | Quorum necessario: " + quorum);

        Node novoLider = null;

        for (Node candidato : nodes) {
            if (!candidato.alive) continue;

            System.out.println("[Gateway-Raft] Candidato: " + candidato.url
                    + " (prioridade " + candidato.priority + ")");

            int votos = solicitarVotos(candidato, novoTermo);
            System.out.println("[Gateway-Raft] Votos recebidos: " + votos
                    + " | Quorum: " + quorum);

            if (votos >= quorum) {
                novoLider = candidato;
                System.out.println("[Gateway-Raft] >> ELEITO: " + candidato.url
                        + " com " + votos + " voto(s) no Termo " + novoTermo);
                break;
            } else {
                System.out.println("[Gateway-Raft] Candidato " + candidato.url
                        + " nao atingiu quorum. Tentando proximo...");
            }
        }

        if (novoLider != null) {
            currentLeader = novoLider;
        } else {
            System.err.println("[Gateway-Raft] Eleicao falhou no Termo " + novoTermo);
            currentLeader = null;
        }

        electionInProgress = false;
    }

    private int solicitarVotos(Node candidato, int termo) {
        int votos = 1; // candidato vota em si mesmo

        Map<String, Object> pedidoVoto = new HashMap<>();
        pedidoVoto.put("candidateUrl", candidato.url);
        pedidoVoto.put("term", termo);

        for (Node eleitor : nodes) {
            if (eleitor.url.equals(candidato.url)) continue;
            if (!eleitor.alive) continue;

            try {
                Boolean voto = restTemplate.postForObject(
                        eleitor.url + "/vote", pedidoVoto, Boolean.class);
                if (Boolean.TRUE.equals(voto)) {
                    votos++;
                    System.out.println("[Gateway-Raft] Voto SIM de " + eleitor.url);
                } else {
                    System.out.println("[Gateway-Raft] Voto NAO de " + eleitor.url);
                }
            } catch (Exception e) {
                System.err.println("[Gateway-Raft] Sem resposta de " + eleitor.url);
            }
        }

        return votos;
    }

    @GetMapping("/leader")
    public ResponseEntity<String> getLeader() {
        if (currentLeader == null) {
            return ResponseEntity.status(503).body("Nenhum lider disponivel.");
        }
        return ResponseEntity.ok(currentLeader.url);
    }

    @PostMapping("/users")
    public ResponseEntity<String> forwardToLeader(@RequestBody Object body) {
        if (currentLeader == null) {
            return ResponseEntity.status(503).body("Servico indisponivel.");
        }
        try {
            String response = restTemplate.postForObject(
                    currentLeader.url + "/users", body, String.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("[Gateway-Raft] Falha ao encaminhar: " + e.getMessage());
            currentLeader.alive = false;
            iniciarEleicao();
            return ResponseEntity.status(503).body("Lider caiu. Tente novamente.");
        }
    }

    @GetMapping("/users")
    public ResponseEntity<String> listFromLeader() {
        if (currentLeader == null) return ResponseEntity.status(503).body("[]");
        try {
            return ResponseEntity.ok(
                    restTemplate.getForObject(currentLeader.url + "/users", String.class));
        } catch (Exception e) {
            return ResponseEntity.status(503).body("[]");
        }
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("leader", currentLeader != null ? currentLeader.url : "nenhum");
        result.put("term", currentTerm.get());

        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (Node n : nodes) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("url", n.url);
            info.put("priority", n.priority);
            info.put("alive", n.alive);
            info.put("isLeader", currentLeader != null && currentLeader.url.equals(n.url));
            nodeList.add(info);
        }
        result.put("nodes", nodeList);
        return result;
    }
}