package com.quickorder.restaurant.core.equipe.infrastructure;

import com.quickorder.restaurant.core.equipe.domain.LimiteDeTentativas;
import com.quickorder.restaurant.core.equipe.domain.Relogio;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class LimiteDeTentativasBucket4j implements LimiteDeTentativas {

  private static final int MAXIMO_DE_FALHAS_DA_CONTA = 5;
  private static final Duration[] DURACOES_DE_BLOQUEIO = {
    Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15)
  };

  private final Relogio relogio;
  private final TimeMeter medidorDeTempo;
  private final ConcurrentMap<String, Bucket> baldesPorIp = new ConcurrentHashMap<>();
  private final ConcurrentMap<String, EstadoDaConta> estadosPorConta = new ConcurrentHashMap<>();

  public LimiteDeTentativasBucket4j(Relogio relogio) {
    this.relogio = relogio;
    medidorDeTempo =
        new TimeMeter() {
          @Override
          public long currentTimeNanos() {
            return relogio.agora().toEpochMilli() * 1_000_000L;
          }

          @Override
          public boolean isWallClockBased() {
            return true;
          }
        };
  }

  @Override
  public long reservarTentativaPorIp(String ip, Instant instante) {
    Bucket bucket = baldesPorIp.computeIfAbsent(ip, this::novoBaldePorIp);
    if (bucket.tryConsume(1)) {
      return 0;
    }
    long nanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
    return segundosArredondadosParaCima(nanos);
  }

  @Override
  public long tempoRestanteDeBloqueioDaConta(String conta, Instant instante) {
    EstadoDaConta estado = estadosPorConta.get(conta);
    return estado == null ? 0 : estado.tempoRestante(instante);
  }

  @Override
  public void registrarFalhaDaConta(String conta, Instant instante) {
    EstadoDaConta estado = estadosPorConta.computeIfAbsent(conta, chave -> new EstadoDaConta());
    estado.registrarFalha(instante);
  }

  @Override
  public void registrarSucessoDaConta(String conta) {
    estadosPorConta.remove(conta);
  }

  private Bucket novoBaldePorIp(String ip) {
    return Bucket.builder()
        .addLimit(Bandwidth.builder().capacity(30).refillGreedy(30, Duration.ofMinutes(1)).build())
        .withCustomTimePrecision(medidorDeTempo)
        .build();
  }

  private static long segundosArredondadosParaCima(long nanos) {
    return Math.max(1, (nanos + 999_999_999L) / 1_000_000_000L);
  }

  private static class EstadoDaConta {

    private int falhas;
    private int nivelDeBloqueio;
    private Instant bloqueadaAte;

    synchronized long tempoRestante(Instant instante) {
      if (bloqueadaAte == null || !instante.isBefore(bloqueadaAte)) {
        bloqueadaAte = null;
        return 0;
      }
      return segundosArredondadosParaCima(Duration.between(instante, bloqueadaAte).toNanos());
    }

    synchronized void registrarFalha(Instant instante) {
      if (bloqueadaAte != null && instante.isBefore(bloqueadaAte)) {
        return;
      }
      bloqueadaAte = null;
      falhas++;
      if (falhas == MAXIMO_DE_FALHAS_DA_CONTA) {
        Duration duracao =
            DURACOES_DE_BLOQUEIO[Math.min(nivelDeBloqueio, DURACOES_DE_BLOQUEIO.length - 1)];
        bloqueadaAte = instante.plus(duracao);
        nivelDeBloqueio = Math.min(nivelDeBloqueio + 1, DURACOES_DE_BLOQUEIO.length - 1);
        falhas = 0;
      }
    }
  }
}
