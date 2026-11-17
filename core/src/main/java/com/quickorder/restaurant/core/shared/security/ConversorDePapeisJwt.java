package com.quickorder.restaurant.core.shared.security;

import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class ConversorDePapeisJwt implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Object papeis = jwt.getClaims().get("roles");
    if (!(papeis instanceof Collection<?> valores)) {
      return List.of();
    }

    return valores.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .filter(papel -> !papel.isBlank())
        .map(papel -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + papel))
        .toList();
  }
}
