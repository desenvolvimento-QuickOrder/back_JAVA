package com.quickorder.restaurant.core.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientIpResolver.class);
  private final int posicaoIpDaDireita;

  public ClientIpResolver(
      @Value("${quickorder.proxy.posicao-ip-da-direita:0}") int posicaoIpDaDireita) {
    if (posicaoIpDaDireita < 0) {
      throw new IllegalArgumentException("A posição do IP não pode ser negativa.");
    }
    this.posicaoIpDaDireita = posicaoIpDaDireita;
  }

  public String resolver(HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();
    if (posicaoIpDaDireita == 0) {
      return remoteAddr;
    }

    String encaminhamentos = request.getHeader("X-Forwarded-For");
    List<String> ipsValidos =
        encaminhamentos == null
            ? List.of()
            : Arrays.stream(encaminhamentos.split(","))
                .map(String::trim)
                .filter(ClientIpResolver::ehEnderecoIp)
                .toList();

    if (ipsValidos.size() < posicaoIpDaDireita) {
      LOGGER.warn(
          "X-Forwarded-For tem {} IP(s) válido(s), mas a posição configurada é {}; usando remoteAddr.",
          ipsValidos.size(),
          posicaoIpDaDireita);
      return remoteAddr;
    }

    return ipsValidos.get(ipsValidos.size() - posicaoIpDaDireita);
  }

  private static boolean ehEnderecoIp(String valor) {
    if (valor.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
      return Arrays.stream(valor.split("\\."))
          .mapToInt(Integer::parseInt)
          .allMatch(octeto -> octeto <= 255);
    }
    if (!valor.contains(":") || !valor.matches("[0-9A-Fa-f:.]+")) {
      return false;
    }
    try {
      return InetAddress.getByName(valor) instanceof Inet6Address;
    } catch (UnknownHostException exception) {
      return false;
    }
  }
}
