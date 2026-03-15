package com.unifor.br.client_app;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.client.RestTemplate;

import java.util.Scanner;

// ══════════════════════════════════════════════════════════════════════════════
// ClientApp — envia mensagens para o Gateway, que repassa ao servidor primário
//
// Fluxo:
//   ClientApp → Gateway (8090) → Líder atual → Réplicas
// ══════════════════════════════════════════════════════════════════════════════

@SpringBootApplication
public class ClientApp implements CommandLineRunner {

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        SpringApplication.run(ClientApp.class, args);
    }

    @Override
    public void run(String... args) {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║        SISTEMA CLIENTE-SERVIDOR      ║");
        System.out.println("║         Conectado ao Gateway         ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("Gateway: " + gatewayUrl);
        System.out.println();

        boolean rodando = true;

        while (rodando) {
            System.out.println("┌─────────────────────────────────────┐");
            System.out.println("│  1. Enviar novo usuário             │");
            System.out.println("│  2. Listar todos os usuários        │");
            System.out.println("│  3. Ver status do cluster           │");
            System.out.println("│  4. Sair                            │");
            System.out.println("└─────────────────────────────────────┘");
            System.out.print("Escolha uma opção: ");

            String opcao = scanner.nextLine().trim();

            switch (opcao) {
                case "1" -> enviarUsuario();
                case "2" -> listarUsuarios();
                case "3" -> verStatus();
                case "4" -> {
                    System.out.println("Encerrando cliente...");
                    rodando = false;
                }
                default -> System.out.println("Opção inválida. Tente novamente.\n");
            }
        }
    }

    // ── Opção 1: envia um novo usuário para o Gateway ─────────────────────────

    private void enviarUsuario() {
        System.out.println("\n--- Cadastrar novo usuário ---");

        System.out.print("ID: ");
        long id;
        try {
            id = Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("ID inválido. Operação cancelada.\n");
            return;
        }

        System.out.print("Nome: ");
        String nome = scanner.nextLine().trim();

        System.out.print("Email: ");
        String email = scanner.nextLine().trim();

        User user = new User(id, nome, email);

        try {
            System.out.println("\nEnviando para o Gateway...");
            String resposta = restTemplate.postForObject(
                    gatewayUrl + "/users", user, String.class);
            System.out.println("✔ Usuário enviado com sucesso!");
            System.out.println("Resposta: " + resposta);
        } catch (Exception e) {
            System.out.println("✘ Erro ao enviar usuário: " + e.getMessage());
        }

        System.out.println();
    }

    // ── Opção 2: lista todos os usuários do líder atual ───────────────────────

    private void listarUsuarios() {
        System.out.println("\n--- Listando usuários ---");
        try {
            String resposta = restTemplate.getForObject(
                    gatewayUrl + "/users", String.class);
            System.out.println("Usuários cadastrados:");
            System.out.println(resposta);
        } catch (Exception e) {
            System.out.println("✘ Erro ao listar usuários: " + e.getMessage());
        }
        System.out.println();
    }

    // ── Opção 3: mostra o status do cluster ───────────────────────────────────

    private void verStatus() {
        System.out.println("\n--- Status do Cluster ---");
        try {
            String resposta = restTemplate.getForObject(
                    gatewayUrl + "/status", String.class);
            System.out.println(resposta);
        } catch (Exception e) {
            System.out.println("✘ Erro ao consultar status: " + e.getMessage());
        }
        System.out.println();
    }
}
