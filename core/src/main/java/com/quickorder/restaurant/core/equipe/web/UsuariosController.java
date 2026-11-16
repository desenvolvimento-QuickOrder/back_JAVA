package com.quickorder.restaurant.core.equipe.web;

import com.quickorder.restaurant.api.UsuariosApi;
import com.quickorder.restaurant.api.model.AtualizarUsuarioRequest;
import com.quickorder.restaurant.api.model.CriarUsuario201Response;
import com.quickorder.restaurant.api.model.CriarUsuarioRequest;
import com.quickorder.restaurant.api.model.ListarUsuarios200Response;
import com.quickorder.restaurant.api.model.ListarUsuarios200ResponseAllOfItemsInner;
import com.quickorder.restaurant.core.equipe.application.AtivarUsuario;
import com.quickorder.restaurant.core.equipe.application.AtualizarUsuario;
import com.quickorder.restaurant.core.equipe.application.CampoAtualizacao;
import com.quickorder.restaurant.core.equipe.application.CriarUsuario;
import com.quickorder.restaurant.core.equipe.application.DesativarUsuario;
import com.quickorder.restaurant.core.equipe.application.ListarUsuarios;
import com.quickorder.restaurant.core.equipe.application.ObterUsuario;
import com.quickorder.restaurant.core.equipe.domain.PaginaDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.Usuario;
import com.quickorder.restaurant.core.shared.error.ErroDeCampo;
import com.quickorder.restaurant.core.shared.error.ErroDeValidacaoDaRequisicao;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UsuariosController implements UsuariosApi {

  private final CriarUsuario criarUsuario;
  private final ListarUsuarios listarUsuarios;
  private final ObterUsuario obterUsuario;
  private final AtualizarUsuario atualizarUsuario;
  private final DesativarUsuario desativarUsuario;
  private final AtivarUsuario ativarUsuario;

  public UsuariosController(
      CriarUsuario criarUsuario,
      ListarUsuarios listarUsuarios,
      ObterUsuario obterUsuario,
      AtualizarUsuario atualizarUsuario,
      DesativarUsuario desativarUsuario,
      AtivarUsuario ativarUsuario) {
    this.criarUsuario = criarUsuario;
    this.listarUsuarios = listarUsuarios;
    this.obterUsuario = obterUsuario;
    this.atualizarUsuario = atualizarUsuario;
    this.desativarUsuario = desativarUsuario;
    this.ativarUsuario = ativarUsuario;
  }

  @Override
  public ResponseEntity<CriarUsuario201Response> criarUsuario(CriarUsuarioRequest request) {
    Papel papel = papel(valor(request.getPapel()));
    Usuario usuario =
        criarUsuario.executar(request.getNome(), papel, request.getLogin(), request.getSenha());
    URI location = URI.create("/api/v1/usuarios/" + usuario.id());
    return ResponseEntity.created(location).body(resposta(usuario));
  }

  @Override
  public ResponseEntity<ListarUsuarios200Response> listarUsuarios(
      Integer page, Integer size, String papel, Boolean ativo) {
    PaginaDeUsuarios pagina =
        listarUsuarios.executar(
            papel(papel), ativo, page == null ? 0 : page, size == null ? 20 : size);
    List<ListarUsuarios200ResponseAllOfItemsInner> itens =
        pagina.itens().stream().map(UsuariosController::item).toList();
    ListarUsuarios200Response resposta = new ListarUsuarios200Response();
    resposta.setPage(pagina.pagina());
    resposta.setSize(pagina.tamanho());
    resposta.setTotal(pagina.total());
    resposta.setItems(itens);
    return ResponseEntity.ok(resposta);
  }

  @Override
  public ResponseEntity<CriarUsuario201Response> obterUsuario(UUID usuarioId) {
    return ResponseEntity.ok(resposta(obterUsuario.executar(usuarioId)));
  }

  @Override
  public ResponseEntity<CriarUsuario201Response> atualizarUsuario(
      UUID usuarioId, AtualizarUsuarioRequest request) {
    Usuario usuario =
        atualizarUsuario.executar(
            usuarioId,
            idDoSolicitante(),
            campo(request.getNome()),
            campoPapel(request.getPapel()),
            campo(request.getSenha()),
            campo(request.getSenhaAtual()));
    return ResponseEntity.ok(resposta(usuario));
  }

  @Override
  public ResponseEntity<CriarUsuario201Response> desativarUsuario(UUID usuarioId) {
    return ResponseEntity.ok(resposta(desativarUsuario.executar(usuarioId)));
  }

  @Override
  public ResponseEntity<CriarUsuario201Response> ativarUsuario(UUID usuarioId) {
    return ResponseEntity.ok(resposta(ativarUsuario.executar(usuarioId)));
  }

  private static CriarUsuario201Response resposta(Usuario usuario) {
    CriarUsuario201Response resposta = new CriarUsuario201Response();
    resposta.setId(usuario.id());
    resposta.setNome(usuario.nome());
    resposta.setLogin(usuario.login());
    resposta.setPapel(CriarUsuario201Response.PapelEnum.valueOf(usuario.papel().name()));
    resposta.setAtivo(usuario.ativo());
    return resposta;
  }

  private static ListarUsuarios200ResponseAllOfItemsInner item(Usuario usuario) {
    ListarUsuarios200ResponseAllOfItemsInner item = new ListarUsuarios200ResponseAllOfItemsInner();
    item.setId(usuario.id());
    item.setNome(usuario.nome());
    item.setLogin(usuario.login());
    item.setPapel(
        JsonNullable.of(
            ListarUsuarios200ResponseAllOfItemsInner.PapelEnum.valueOf(usuario.papel().name())));
    item.setAtivo(usuario.ativo());
    return item;
  }

  private static Papel papel(String papel) {
    if (papel == null) {
      return null;
    }
    try {
      return Papel.valueOf(papel);
    } catch (IllegalArgumentException excecao) {
      throw new ErroDeValidacaoDaRequisicao(
          List.of(new ErroDeCampo("papel", "O papel informado não é válido.")));
    }
  }

  private static Papel papel(CriarUsuarioRequest.PapelEnum papel) {
    return papel == null ? null : Papel.valueOf(papel.name());
  }

  private static <T> CampoAtualizacao<T> campo(JsonNullable<T> valor) {
    return valor == null || !valor.isPresent()
        ? CampoAtualizacao.ausente()
        : CampoAtualizacao.informado(valor.get());
  }

  private static <T> T valor(JsonNullable<T> campo) {
    return campo == null || !campo.isPresent() ? null : campo.get();
  }

  private static CampoAtualizacao<Papel> campoPapel(
      JsonNullable<AtualizarUsuarioRequest.PapelEnum> valor) {
    if (valor == null || !valor.isPresent()) {
      return CampoAtualizacao.ausente();
    }
    AtualizarUsuarioRequest.PapelEnum papel = valor.get();
    return CampoAtualizacao.informado(papel == null ? null : Papel.valueOf(papel.name()));
  }

  private static UUID idDoSolicitante() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken jwt) {
      return UUID.fromString(jwt.getToken().getSubject());
    }
    throw new IllegalStateException("Token JWT ausente no contexto de segurança.");
  }
}
