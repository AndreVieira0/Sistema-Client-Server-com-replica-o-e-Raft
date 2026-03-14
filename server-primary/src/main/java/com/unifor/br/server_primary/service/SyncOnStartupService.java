package com.unifor.br.server_primary.service;

import com.unifor.br.server_primary.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SyncOnStartupService {

    private final UserRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    @Value("${server.own-url:http://localhost:8080}")
    private String ownUrl;

    public SyncOnStartupService(UserRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void syncOnStartup() {
        System.out.println("[Sync] Iniciando verificação de sincronização...");
        try {
            // 1. Pergunta ao Gateway quem é o líder atual
            String leaderUrl = restTemplate.getForObject(gatewayUrl + "/leader", String.class);

            if (leaderUrl == null || leaderUrl.isBlank()) {
                System.out.println("[Sync] Gateway não retornou líder — pulando sincronização.");
                return;
            }

            System.out.println("[Sync] Líder atual: " + leaderUrl);

            // 2. Se eu mesmo sou o líder, não precisa sincronizar
            if (normalize(leaderUrl).equals(normalize(ownUrl))) {
                System.out.println("[Sync] Sou o líder atual — nenhuma sincronização necessária.");
                return;
            }

            // 3. Baixa o banco do líder
            byte[] content = restTemplate.getForObject(leaderUrl + "/db/download", byte[].class);

            if (content == null || content.length == 0) {
                System.out.println("[Sync] Líder sem dados ainda — nada a sincronizar.");
                return;
            }

            // 4. Substitui o arquivo local
            repository.replaceAll(content);
            System.out.println("[Sync] Sincronização concluída! " + content.length + " bytes recebidos.");

        } catch (Exception e) {
            System.err.println("[Sync] Aviso: sincronização falhou — " + e.getMessage()
                    + ". Continuando com dados locais.");
        }
    }

    private String normalize(String url) {
        return url == null ? "" : url.trim().replaceAll("/+$", "").toLowerCase();
    }
}
