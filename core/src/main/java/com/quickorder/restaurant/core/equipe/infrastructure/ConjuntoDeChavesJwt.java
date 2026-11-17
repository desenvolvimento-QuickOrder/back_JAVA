package com.quickorder.restaurant.core.equipe.infrastructure;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

public record ConjuntoDeChavesJwt(JWKSource<SecurityContext> fonte, JWKSet conjuntoPublico) {}
