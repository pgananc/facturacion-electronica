package com.wallparisoft.ebillbilling.controller;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.wallparisoft.ebillbilling.entity.PurchaseProof;
import com.wallparisoft.ebillbilling.service.IPurchaseProofService;

@RestController
@RequestMapping("/purchase-proof")
public class PurchaseProofController {

  @Autowired
  private IPurchaseProofService purchaseProofService;

  @GetMapping
  public ResponseEntity<List<PurchaseProof>> getAllPurchaseProofs() {
    List<PurchaseProof> purchaseProofs = purchaseProofService.list();
    return new ResponseEntity<List<PurchaseProof>>(purchaseProofs, HttpStatus.OK);
  }

  @PostMapping
  public ResponseEntity<Object> register(@Validated @RequestBody PurchaseProof purchaseProof) {
    PurchaseProof obj = purchaseProofService.register(purchaseProof);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
        .buildAndExpand(obj.getIdPurchaseProof()).toUri();
    return ResponseEntity.created(location).build();
  }
}
