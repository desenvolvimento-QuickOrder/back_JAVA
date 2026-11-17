package com.quickorder.restaurant.core.equipe.web;

import com.quickorder.restaurant.api.AuthApi;
import com.quickorder.restaurant.api.model.Login200Response;
import com.quickorder.restaurant.api.model.LoginPinRequest;
import com.quickorder.restaurant.api.model.LoginRequest;
import com.quickorder.restaurant.api.model.RenovarTokenRequest;
import com.quickorder.restaurant.api.model.TrocarPinRequest;
import com.quickorder.restaurant.core.equipe.application.Autenticar;
import com.quickorder.restaurant.core.equipe.application.AutenticarPorPin;
import com.quickorder.restaurant.core.equipe.application.EncerrarSessao;
import com.quickorder.restaurant.core.equipe.application.RenovarSessao;
import com.quickorder.restaurant.core.equipe.application.TrocarPin;
import com.quickorder.restaurant.core.equipe.domain.ResultadoDeSessao;
import com.quickorder.restaurant.core.shared.tenant.TenantContext;
import com.quickorder.restaurant.core.shared.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class AuthController implements AuthApi {

  private final Autenticar autenticar;
  private final AutenticarPorPin autenticarPorPin;
  private final RenovarSessao renovarSessao;
  private final EncerrarSessao encerrarSessao;
  private final TrocarPin trocarPin;
  private final TenantContext tenantContext;
  private final ClientIpResolver clientIpResolver;
  private final HttpServletRequest request;

  public AuthController(
      Autenticar autenticar,
      AutenticarPorPin autenticarPorPin,
      RenovarSessao renovarSessao,
      EncerrarSessao encerrarSessao,
      TrocarPin trocarPin,
      TenantContext tenantContext,
      ClientIpResolver clientIpResolver,
      HttpServletRequest request) {
    this.autenticar = autenticar;
    this.autenticarPorPin = autenticarPorPin;
    this.renovarSessao = renovarSessao;
    this.encerrarSessao = encerrarSessao;
    this.trocarPin = trocarPin;
    this.tenantContext = tenantContext;
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
  public ResponseEntity<Login200Response> renovarToken(
      RenovarTokenRequest renovarTokenRequest, String xDeviceToken) {
    return ResponseEntity.ok(
        resposta(renovarSessao.executar(renovarTokenRequest.getRefreshToken(), xDeviceToken)));
  }

  @Override
  public ResponseEntity<Login200Response> loginPin(
      String xDeviceToken, LoginPinRequest loginPinRequest) {
    ResultadoDeSessao sessao =
        autenticarPorPin.executar(
            loginPinRequest.getRestaurante(),
            loginPinRequest.getCodigo(),
            loginPinRequest.getPin(),
            xDeviceToken,
            clientIpResolver.resolver(request));
    return ResponseEntity.ok(
        new Login200Response(
            sessao.accessToken(),
            sessao.refreshToken(),
            Login200Response.TokenTypeEnum.BEARER,
            sessao.expiresIn(),
            sessao.deveTrocarPin()));
  }

  @Override
  public ResponseEntity<Void> trocarPin(TrocarPinRequest trocarPinRequest) {
    Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
    UUID usuarioId =
        UUID.fromString(((JwtAuthenticationToken) autenticacao).getToken().getSubject());
    UUID restauranteId = tenantContext.tenantAtual().orElseThrow();
    trocarPin.executar(
        restauranteId, usuarioId, trocarPinRequest.getPinAtual(), trocarPinRequest.getPinNovo());
    return ResponseEntity.noContent().build();
  }

  private static Login200Response resposta(ResultadoDeSessao sessao) {
    return new Login200Response(
        sessao.accessToken(),
        sessao.refreshToken(),
        Login200Response.TokenTypeEnum.BEARER,
        sessao.expiresIn(),
        sessao.deveTrocarPin());
  }
}
