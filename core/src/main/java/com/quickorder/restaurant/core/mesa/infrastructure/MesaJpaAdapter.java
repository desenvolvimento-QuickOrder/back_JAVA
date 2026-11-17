package com.quickorder.restaurant.core.mesa.infrastructure;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import com.quickorder.restaurant.core.mesa.domain.MesaDuplicadoException;
import com.quickorder.restaurant.core.mesa.domain.MesaRepository;
import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import com.quickorder.restaurant.core.shared.tenant.TenantContext;
import java.sql.ResultSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MesaJpaAdapter implements MesaRepository {

  private final JpaMesaRepository jpaMesaRepository;
  private final JdbcTemplate jdbcTemplate;
  private final TenantContext tenantContext;

  public MesaJpaAdapter(
      JpaMesaRepository jpaMesaRepository, JdbcTemplate jdbcTemplate, TenantContext tenantContext) {
    this.jpaMesaRepository = jpaMesaRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.tenantContext = tenantContext;
  }

  @Override
  public Mesa save(Mesa mesa) {
    UUID restauranteId =
        tenantContext
            .tenantAtual()
            .orElseThrow(() -> new IllegalStateException("Tenant do restaurante ausente."));
    try {
      return jpaMesaRepository.saveAndFlush(MesaEntity.fromDomain(mesa, restauranteId)).toDomain();
    } catch (DataIntegrityViolationException exception) {
      if (causedByMesaNumeroConstraint(exception)) {
        throw new MesaDuplicadoException(mesa.numero());
      }
      throw exception;
    }
  }

  @Override
  public Optional<Mesa> findById(UUID id) {
    return jpaMesaRepository.findById(id).map(MesaEntity::toDomain);
  }

  @Override
  public Optional<Mesa> findByIdForUpdate(UUID id) {
    return jpaMesaRepository.findByIdForUpdate(id).map(MesaEntity::toDomain);
  }

  @Override
  public List<Mesa> findPage(int offset, int limit, MesaStatus status, Boolean ativa) {
    StringBuilder sql =
        new StringBuilder(
            """
            select m.id, m.numero, m.capacidade, m.ativa,
                   case when exists (
                     select 1 from comanda c
                     where c.mesa_id = m.id and c.status = 'ABERTA'
                   ) then 'OCUPADA' else 'LIVRE' end as status
            from mesa m
            where 1 = 1
            """);
    var args = new java.util.ArrayList<>();
    if (ativa != null) {
      sql.append(" and m.ativa = ?");
      args.add(ativa);
    }
    if (status != null) {
      sql.append(
          status == MesaStatus.OCUPADA
              ? " and exists (select 1 from comanda c where c.mesa_id = m.id and c.status = 'ABERTA')"
              : " and not exists (select 1 from comanda c where c.mesa_id = m.id and c.status = 'ABERTA')");
    }
    sql.append(" order by m.numero asc, m.id asc limit ? offset ?");
    args.add(limit);
    args.add(offset);
    return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> toDomain(rs), args.toArray()).stream()
        .toList();
  }

  @Override
  public long count(MesaStatus status, Boolean ativa) {
    StringBuilder sql = new StringBuilder("select count(*) from mesa m where 1 = 1");
    var args = new java.util.ArrayList<>();
    if (ativa != null) {
      sql.append(" and m.ativa = ?");
      args.add(ativa);
    }
    if (status != null) {
      sql.append(
          status == MesaStatus.OCUPADA
              ? " and exists (select 1 from comanda c where c.mesa_id = m.id and c.status = 'ABERTA')"
              : " and not exists (select 1 from comanda c where c.mesa_id = m.id and c.status = 'ABERTA')");
    }
    return jdbcTemplate.queryForObject(sql.toString(), Long.class, args.toArray());
  }

  @Override
  public boolean existsByNumero(String numero) {
    return jpaMesaRepository.existsByNumero(numero);
  }

  @Override
  public boolean existsByNumeroAndIdNot(String numero, UUID id) {
    return jpaMesaRepository.existsByNumeroAndIdNot(numero, id);
  }

  private static Mesa toDomain(ResultSet rs) throws java.sql.SQLException {
    return new Mesa(
        rs.getObject("id", UUID.class),
        rs.getString("numero"),
        rs.getInt("capacidade"),
        rs.getBoolean("ativa"),
        MesaStatus.valueOf(rs.getString("status")));
  }

  private static boolean causedByMesaNumeroConstraint(DataIntegrityViolationException exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause.getMessage() != null && cause.getMessage().contains("uq_mesa_numero")) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }
}
