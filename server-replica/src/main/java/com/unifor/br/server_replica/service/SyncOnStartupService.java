package com.unifor.br.server_replica.service;

import com.unifor.br.server_replica.repository.UserRepositoryReplica;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SyncOnStartupService {

    private final UserRepositoryReplica repository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    @Value("${server.own-url:http://localhost:8081}")
    private String ownUrl;

    public SyncOnStartupService(UserRepositoryReplica repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void syncOnStartup() {
        System.out.println("[Sync-Replica1] Iniciando verificação de sincronização...");
        try {
            String leaderUrl = restTemplate.getForObject(gatewayUrl + "/leader", String.class);

            if (leaderUrl == null || leaderUrl.isBlank()) {
                System.out.println("[Sync-Replica1] Gateway sem líder — pulando sincronização.");
                return;
            }

            System.out.println("[Sync-Replica1] Líder atual: " + leaderUrl);

            if (normalize(leaderUrl).equals(normalize(ownUrl))) {
                System.out.println("[Sync-Replica1] Sou o líder — nenhuma sincronização necessária.");
                return;
            }

            byte[] content = restTemplate.getForObject(leaderUrl + "/db/download", byte[].class);

            if (content == null || content.length == 0) {
                System.out.println("[Sync-Replica1] Líder sem dados — nada a sincronizar.");
                return;
            }

            repository.replaceAll(content);
            System.out.println("[Sync-Replica1] Sincronização concluída! " + content.length + " bytes recebidos.");

        } catch (Exception e) {
            System.err.println("[Sync-Replica1] Aviso: sincronização falhou — " + e.getMessage()
                    + ". Continuando com dados locais.");
        }
    }

    private String normalize(String url) {
        return url == null ? "" : url.trim().replaceAll("/+$", "").toLowerCase();
    }
}
