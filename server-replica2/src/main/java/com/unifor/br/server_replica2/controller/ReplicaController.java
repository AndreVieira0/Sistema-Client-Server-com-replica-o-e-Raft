package com.unifor.br.server_replica2.controller;

import com.unifor.br.server_replica2.model.User;
import com.unifor.br.server_replica2.repository.UserRepositoryReplica2;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
public class ReplicaController {

    private final UserRepositoryReplica2 repository;

    public ReplicaController(UserRepositoryReplica2 repository) {
        this.repository = repository;
    }

    @PostMapping("/replica/users")
    public User replicate(@RequestBody User user) throws IOException {
        repository.save(user);
        return user;
    }

    @PostMapping("/users")
    public User save(@RequestBody User user) throws IOException {
        repository.save(user);
        return user;
    }

    @GetMapping("/users")
    public List<User> list() throws IOException {
        return repository.findAll();
    }

    @PostMapping("/replicate")
    public User receiveReplicate(@RequestBody User user) throws IOException {
        repository.save(user);
        return user;
    }

    @GetMapping("/health")
    public String health() {
        return "Estou vivo!";
    }

    @GetMapping("/db/download")
    public ResponseEntity<Resource> downloadDatabase() {
        File file = new File("database-replica2.txt");
        if (!file.exists()) {
            return ResponseEntity.noContent().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"database-replica2.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

    @PostMapping("/vote")
    public boolean vote(@RequestBody Map<String, Object> pedido) {
        System.out.println("[Replica2] Voto solicitado pelo candidato: "
                + pedido.get("candidateUrl") + " | Termo: " + pedido.get("term"));
        return true;
    }
}