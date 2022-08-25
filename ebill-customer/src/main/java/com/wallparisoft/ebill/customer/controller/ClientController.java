package com.wallparisoft.ebill.customer.controller;

import com.wallparisoft.ebill.customer.dto.ClientDto;
import com.wallparisoft.ebill.customer.entity.Client;
import com.wallparisoft.ebill.customer.entity.Contact;
import com.wallparisoft.ebill.customer.mapper.MapStructMapper;
import com.wallparisoft.ebill.customer.service.IClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/client")
public class ClientController {

    @Autowired
    private IClientService clientService;

    @Autowired
    private MapStructMapper mapstructMapper;

    @PostMapping
    public ResponseEntity<ClientDto> save(@Validated @RequestBody ClientDto clientDto) {
        Client client = mapstructMapper.convertClientDtoToClient(clientDto);
        List<Contact> contacts = mapstructMapper.convertContactDtoListToContactList(clientDto.getContacts());
        clientService.saveClientAndContact(client, contacts);
        return new ResponseEntity<>(clientDto, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> save(@PathVariable("id") Long id) {
        clientService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping
    public ResponseEntity<List<ClientDto>> findClientsActiveAndContactActive() {
        List<ClientDto> clients= clientService.findClientsActiveAndContactActive();
        return new ResponseEntity<>(clients,HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<ClientDto> update(@Validated @RequestBody ClientDto clientDto) {
        Client client = mapstructMapper.convertClientDtoToClient(clientDto);
        List<Contact> contacts = mapstructMapper.convertContactDtoListToContactList(clientDto.getContacts());
        clientService.updateClientAndContact(client, contacts);
        return new ResponseEntity<>(clientDto, HttpStatus.OK);
    }
}
