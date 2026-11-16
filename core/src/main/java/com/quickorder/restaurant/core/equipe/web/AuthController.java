package com.quickorder.restaurant.core.equipe.web;

import com.quickorder.restaurant.api.AuthApi;
import com.quickorder.restaurant.api.model.Login200Response;
import com.quickorder.restaurant.api.model.LoginRequest;
import com.quickorder.restaurant.api.model.RenovarTokenRequest;
import com.quickorder.restaurant.core.equipe.application.Autenticar;
import com.quickorder.restaurant.core.equipe.application.EncerrarSessao;
import com.quickorder.restaurant.core.equipe.application.RenovarSessao;
import com.quickorder.restaurant.core.equipe.domain.ResultadoDeSessao;
import com.quickorder.restaurant.core.shared.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController implements AuthApi {

  private final Autenticar autenticar;
  private final RenovarSessao renovarSessao;
  private final EncerrarSessao encerrarSessao;
  private final ClientIpResolver clientIpResolver;
  private final HttpServletRequest request;

  public AuthController(
      Autenticar autenticar,
      RenovarSessao renovarSessao,
      EncerrarSessao encerrarSessao,
      ClientIpResolver clientIpResolver,
      HttpServletRequest request) {
    this.autenticar = autenticar;
    this.renovarSessao = renovarSessao;
    this.encerrarSessao = encerrarSessao;
    this.clientIpResolver = clientIpResolver;
    this.request = request;
  }

  @Override
  public ResponseEntity<Void> encerrarSessao(RenovarTokenRequest renovarTokenRequest) {
    encerrarSessao.executar(renovarTokenRequest.getRefreshToken());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Login200Response> login(LoginRequest loginRequest) {
    return ResponseEntity.ok(
        resposta(
            autenticar.executar(
                loginRequest.getRestaurante(),
                loginRequest.getLogin(),
                loginRequest.getSenha(),
                clientIpResolver.resolver(request))));
  }

  @Override
  public ResponseEntity<Login200Response> renovarToken(RenovarTokenRequest renovarTokenRequest) {
    return ResponseEntity.ok(
        resposta(renovarSessao.executar(renovarTokenRequest.getRefreshToken())));
  }

  private static Login200Response resposta(ResultadoDeSessao sessao) {
    return new Login200Response(
        sessao.accessToken(),
        sessao.refreshToken(),
        Login200Response.TokenTypeEnum.BEARER,
        sessao.expiresIn());
  }
}
