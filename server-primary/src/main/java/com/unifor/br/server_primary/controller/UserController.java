package com.unifor.br.server_primary.controller;

import com.unifor.br.server_primary.model.User;
import com.unifor.br.server_primary.service.UserService;
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
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/users")
    public User create(@RequestBody User user) throws IOException {
        return service.save(user);
    }

    @GetMapping("/users")
    public List<User> list() throws IOException {
        return service.findAll();
    }

    @GetMapping("/health")
    public String healthCheck() {
        return "Estou vivo!";
    }

    @GetMapping("/db/download")
    public ResponseEntity<Resource> downloadDatabase() {
        File file = new File("database.txt");
        if (!file.exists()) {
            return ResponseEntity.noContent().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"database.txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }

    @PostMapping("/replicate")
    public User replicate(@RequestBody User user) throws IOException {
        service.saveLocal(user);
        return user;
    }

    @PostMapping("/vote")
    public boolean vote(@RequestBody Map<String, Object> pedido) {
        System.out.println("[Primary] Voto solicitado pelo candidato: "
                + pedido.get("candidateUrl") + " | Termo: " + pedido.get("term"));
        return true;
    }
}