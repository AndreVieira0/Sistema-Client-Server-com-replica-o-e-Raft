package com.unifor.br.server_replica.repository;

import com.unifor.br.server_replica.model.User;
import org.springframework.stereotype.Repository;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class UserRepositoryReplica {

    private final String FILE_NAME = "database-replica.txt";

    public void save(User user) throws IOException {
        FileWriter fw = new FileWriter(FILE_NAME, true);
        fw.write(user.getId() + "," + user.getName() + "," + user.getEmail() + "\n");
        fw.close();
    }

    // Método novo — leitura de todos os registros
    public List<User> findAll() throws IOException {
        Path path = Paths.get(FILE_NAME);
        if (!Files.exists(path)) return new ArrayList<>();
        return Files.lines(path)
                .filter(line -> !line.isBlank())
                .map(line -> {
                    String[] parts = line.split(",");
                    return new User(Long.parseLong(parts[0]), parts[1], parts[2]);
                })
                .collect(Collectors.toList());
    }

    // Método novo — substitui o arquivo inteiro, usado no failback
    public void replaceAll(byte[] content) throws IOException {
        Files.write(Paths.get(FILE_NAME), content,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}

