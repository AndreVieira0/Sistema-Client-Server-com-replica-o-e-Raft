package com.unifor.br.server_primary.service;

import com.unifor.br.server_primary.model.User;
import com.unifor.br.server_primary.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;

@Service
public class UserService {

    private final UserRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    // URLs das duas réplicas
    private final String REPLICA1_URL = "http://localhost:8081/replica/users";
    private final String REPLICA2_URL = "http://localhost:8082/replica/users";

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public User save(User user) throws IOException {
        // 1. Salva no banco local (database.txt)
        repository.save(user);

        // 2. Tenta enviar para a Réplica 1
        try {
            restTemplate.postForObject(REPLICA1_URL, user, User.class);
            System.out.println("Replicado para Réplica 1 com sucesso.");
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao replicar para a Réplica 1. Ela pode estar offline.");
        }

        // 3. Tenta enviar para a Réplica 2
        try {
            restTemplate.postForObject(REPLICA2_URL, user, User.class);
            System.out.println("Replicado para Réplica 2 com sucesso.");
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao replicar para a Réplica 2. Ela pode estar offline.");
        }

        return user;
    }

    public List<User> findAll() throws IOException {
        return repository.findAll();
    }

    // Método novo: salva só localmente, sem replicar
    // Usado quando este nó vira réplica e recebe dados do novo líder
    public void saveLocal(User user) throws IOException {
        repository.save(user);
    }
}
