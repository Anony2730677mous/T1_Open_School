package ru.t1.java.demo.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.t1.java.demo.aop.HandlingResult;
import ru.t1.java.demo.aop.LoggableException;
import ru.t1.java.demo.kafka.KafkaClientProducer;
import ru.t1.java.demo.model.Client;
import ru.t1.java.demo.model.dto.ClientDto;
import ru.t1.java.demo.repository.ClientRepository;
import ru.t1.java.demo.service.ClientService;
import ru.t1.java.demo.util.ClientMapper;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("${alias.api.clients}")
public class ClientController {

    private final ClientService clientService;
    private final KafkaClientProducer kafkaClientProducer;
    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    @Value("${t1.kafka.topic.client_registration}")
    private String topic;

    @PostConstruct
    void init() {
//        parseSource();
    }

    @HandlingResult
    @GetMapping(value = "/parse")
    @LoggableException
    public void parseSource() {
        clientRepository.save(Client.builder()
                .firstName("John42")
                .build());
        clientRepository.findClientByFirstName("John42");
    }

    @GetMapping("/admin")
//    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "Admin Board.";
    }

    /*
    используется кебаб-кейс в написании url
     */
    @PostMapping("/create-new-client")
    public ResponseEntity<ClientDto> createNewClient(@RequestBody ClientDto clientDto) {
        ClientDto responseEntity = clientMapper.toDto(clientService
                .createNewClient(clientMapper.toEntity(clientDto)));
        log.info("Создан новый клиент");
        return ResponseEntity.ok(responseEntity);
    }
}
