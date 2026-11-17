package com.quickorder.restaurant.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.quickorder.restaurant.core.equipe.domain.CodigoFuncionario;
import com.quickorder.restaurant.core.equipe.domain.Pin;
import org.junit.jupiter.api.Test;

class PinTest {

  @Test
  void exigeQuatroDigitosNoCodigoENoPin() {
    assertThrows(IllegalArgumentException.class, () -> new CodigoFuncionario("123"));
    assertThrows(IllegalArgumentException.class, () -> new CodigoFuncionario(null));
    assertThrows(IllegalArgumentException.class, () -> new Pin("12a4"));
    assertThrows(IllegalArgumentException.class, () -> new Pin(null));
    assertTrue(new CodigoFuncionario("1234").valor().equals("1234"));
  }

  @Test
  void rejeitaPinIgualAoCodigoDigitosRepetidosESequencias() {
    assertFalse(new Pin("2580").podeSerUsadoCom(new CodigoFuncionario("2580")));
    assertFalse(new Pin("7777").podeSerUsadoCom(new CodigoFuncionario("1234")));
    assertFalse(new Pin("1234").podeSerUsadoCom(new CodigoFuncionario("5678")));
    assertFalse(new Pin("4321").podeSerUsadoCom(new CodigoFuncionario("5678")));
    assertTrue(new Pin("1235").podeSerUsadoCom(new CodigoFuncionario("5678")));
  }

  @Test
  void aceitaPinNaoSequencialDiferenteDoCodigo() {
    assertTrue(new Pin("2580").podeSerUsadoCom(new CodigoFuncionario("1234")));
  }
}
