package com.quickorder.restaurant.core.mesa.infrastructure;

import com.quickorder.restaurant.core.mesa.application.MesaStatusResolver;
import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class MesaStatusResolverImpl implements MesaStatusResolver {

  private final JdbcTemplate jdbcTemplate;

  public MesaStatusResolverImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public MesaStatus resolve(UUID mesaId) {
    Integer count = jdbcTemplate.queryForObject(
        "select count(*) from comanda where restaurante_id = ? and mesa_id = ? and status = 'ABERTA'",
        Integer.class,
        MesaEntity.DEFAULT_RESTAURANTE_ID,
        mesaId);
    return MesaStatus.of(count != null && count > 0);
  }
}
