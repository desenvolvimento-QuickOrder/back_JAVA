package com.quickorder.restaurant.core.equipe.web;

import com.quickorder.restaurant.core.equipe.domain.ProvedorDeJwks;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JwksController {

  private final ProvedorDeJwks provedorDeJwks;

  public JwksController(ProvedorDeJwks provedorDeJwks) {
    this.provedorDeJwks = provedorDeJwks;
  }

  @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> jwks() {
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(provedorDeJwks.obterJwksPublico());
  }
}
