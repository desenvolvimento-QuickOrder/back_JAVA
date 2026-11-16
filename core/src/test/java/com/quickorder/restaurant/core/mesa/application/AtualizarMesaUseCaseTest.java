package com.quickorder.restaurant.core.mesa.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import com.quickorder.restaurant.core.mesa.domain.MesaOcupadaException;
import com.quickorder.restaurant.core.mesa.domain.MesaRepository;
import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AtualizarMesaUseCaseTest {

  @Test
  void deveRejeitarDesativacaoQuandoMesaEstaOcupada() {
    UUID mesaId = UUID.randomUUID();
    var mesaRepository = mock(MesaRepository.class);
    var statusResolver = mock(MesaStatusResolver.class);

    when(mesaRepository.findById(mesaId))
        .thenReturn(Optional.of(Mesa.criar("A1", 4).comStatus(MesaStatus.OCUPADA)));
    when(statusResolver.resolve(mesaId)).thenReturn(MesaStatus.OCUPADA);

    var useCase = new AtualizarMesaUseCase(mesaRepository, statusResolver);

    assertThrows(MesaOcupadaException.class, () -> useCase.execute(mesaId, null, null, false));
  }
}
