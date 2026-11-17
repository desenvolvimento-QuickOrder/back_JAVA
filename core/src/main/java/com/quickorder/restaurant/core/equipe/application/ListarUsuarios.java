package com.quickorder.restaurant.core.equipe.application;

import com.quickorder.restaurant.core.equipe.domain.PaginaDeUsuarios;
import com.quickorder.restaurant.core.equipe.domain.Papel;
import com.quickorder.restaurant.core.equipe.domain.RepositorioDeUsuarios;
import com.quickorder.restaurant.core.shared.error.ErroDeCampo;
import com.quickorder.restaurant.core.shared.error.ErroDeValidacaoDaRequisicao;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarUsuarios {

  private final RepositorioDeUsuarios usuarios;

  public ListarUsuarios(RepositorioDeUsuarios usuarios) {
    this.usuarios = usuarios;
  }

  @Transactional(readOnly = true)
  public PaginaDeUsuarios executar(Papel papel, Boolean ativo, int pagina, int tamanho) {
    if (tamanho > 100) {
      throw new ErroDeValidacaoDaRequisicao(
          List.of(new ErroDeCampo("size", "O tamanho máximo da página é 100.")));
    }
    return usuarios.listar(papel, ativo, pagina, tamanho);
  }
}
