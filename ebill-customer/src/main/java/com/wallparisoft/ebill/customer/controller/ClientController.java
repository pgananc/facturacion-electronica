package com.wallparisoft.ebill.customer.controller;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.response.BasicResponse;
import com.wallparisoft.ebill.customer.response.ClientDtoResponse;
import com.wallparisoft.ebill.customer.service.IClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("api/client")
public class ClientController {

    @Autowired
    private IClientService clientService;


    @PostMapping
    public ResponseEntity<BasicResponse> save(@Valid @RequestBody ClientDto clientDto) {
        clientService.saveClientAndContact(clientDto);
        BasicResponse response = BasicResponse.builder().status("OK").build();
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<BasicResponse> delete(@PathVariable("id") Long id) {
        clientService.delete(id);
        BasicResponse response = BasicResponse.builder().status("OK").build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ClientDtoResponse> findClientsActiveAndContactActive() {
        List<ClientDto> clients = clientService.findClientsActiveAndContactActive();
        ClientDtoResponse response = ClientDtoResponse.builder()
                .status("OK")
                .clientDtos(clients)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<BasicResponse> update(@Valid @RequestBody ClientDto clientDto) {
        clientService.updateClientAndContact(clientDto);
        BasicResponse response = BasicResponse.builder().status("OK").build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
