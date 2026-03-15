package com.unifor.br.server_replica2.service;

import com.unifor.br.server_replica2.repository.UserRepositoryReplica2;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SyncOnStartupService {

    private final UserRepositoryReplica2 repository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    @Value("${server.own-url:http://localhost:8082}")
    private String ownUrl;

    public SyncOnStartupService(UserRepositoryReplica2 repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void syncOnStartup() {
        System.out.println("[Sync-Replica2] Iniciando verificação de sincronização...");
        try {
            String leaderUrl = restTemplate.getForObject(gatewayUrl + "/leader", String.class);

            if (leaderUrl == null || leaderUrl.isBlank()) {
                System.out.println("[Sync-Replica2] Gateway sem líder — pulando sincronização.");
                return;
            }

            System.out.println("[Sync-Replica2] Líder atual: " + leaderUrl);

            if (normalize(leaderUrl).equals(normalize(ownUrl))) {
                System.out.println("[Sync-Replica2] Sou o líder — nenhuma sincronização necessária.");
                return;
            }

            byte[] content = restTemplate.getForObject(leaderUrl + "/db/download", byte[].class);

            if (content == null || content.length == 0) {
                System.out.println("[Sync-Replica2] Líder sem dados — nada a sincronizar.");
                return;
            }

            repository.replaceAll(content);
            System.out.println("[Sync-Replica2] Sincronização concluída! " + content.length + " bytes recebidos.");

        } catch (Exception e) {
            System.err.println("[Sync-Replica2] Aviso: sincronização falhou — " + e.getMessage()
                    + ". Continuando com dados locais.");
        }
    }

    private String normalize(String url) {
        return url == null ? "" : url.trim().replaceAll("/+$", "").toLowerCase();
    }
}
