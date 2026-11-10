package com.quickorder.restaurant.core.shared.tenant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioDeIsolamentoTeste
    extends JpaRepository<RegistroDeIsolamentoTeste, UUID> {

  @Query("select registro from RegistroDeIsolamentoTeste registro where registro.id = :id")
  Optional<RegistroDeIsolamentoTeste> buscarPorIdComJpql(@Param("id") UUID id);

  @Query(value = "select count(*) from isolamento_teste where id = :id", nativeQuery = true)
  long contarPorIdComSqlNativo(@Param("id") UUID id);
}
