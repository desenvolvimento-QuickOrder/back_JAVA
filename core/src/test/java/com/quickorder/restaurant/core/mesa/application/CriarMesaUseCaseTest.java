package com.quickorder.restaurant.core.mesa.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickorder.restaurant.core.mesa.domain.Mesa;
import com.quickorder.restaurant.core.mesa.domain.MesaRepository;
import com.quickorder.restaurant.core.mesa.domain.MesaStatus;
import org.junit.jupiter.api.Test;

class CriarMesaUseCaseTest {

  @Test
  void deveCriarMesaQuandoNumeroNaoExiste() {
    var mesaRepository = mock(MesaRepository.class);
    when(mesaRepository.existsByNumero("A1")).thenReturn(false);
    when(mesaRepository.save(any(Mesa.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var useCase = new CriarMesaUseCase(mesaRepository);
    var mesa = useCase.execute("A1", 4);

    assertEquals("A1", mesa.numero());
    assertEquals(4, mesa.capacidade());
    assertTrue(mesa.ativa());
    assertEquals(MesaStatus.LIVRE, mesa.status());
    verify(mesaRepository).existsByNumero("A1");
    verify(mesaRepository).save(any(Mesa.class));
  }
}
