package br.edu.ifrs.parkcontrol.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Responsavel pelo calculo do valor a ser cobrado por um periodo de permanencia
 * no estacionamento, de acordo com a {@link TarifaConfig} vigente.
 *
 * Regras:
 * - Permanencias dentro da tolerancia gratuita nao sao cobradas.
 * - Fracoes de hora sao arredondadas para cima (cobra-se a hora cheia iniciada).
 * - Mensalistas recebem o percentual de desconto configurado.
 */
public class CalculadoraDeValor {

    private final TarifaConfig tarifaConfig;

    public CalculadoraDeValor(TarifaConfig tarifaConfig) {
        this.tarifaConfig = Objects.requireNonNull(tarifaConfig);
    }

    public BigDecimal calcular(LocalDateTime entrada, LocalDateTime saida, boolean mensalista) {
        Objects.requireNonNull(entrada, "Entrada nao pode ser nula");
        Objects.requireNonNull(saida, "Saida nao pode ser nula");
        if (saida.isBefore(entrada)) {
            throw new IllegalArgumentException("Saida nao pode ser anterior a entrada");
        }

        long minutosPermanencia = Duration.between(entrada, saida).toMinutes();

        if (minutosPermanencia <= tarifaConfig.toleranciaMinutosGratis()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long horasCobradas = calcularHorasCheias(minutosPermanencia);
        BigDecimal valorBase = tarifaConfig.valorPorHora().multiply(BigDecimal.valueOf(horasCobradas));

        BigDecimal valorFinal = mensalista
                ? aplicarDesconto(valorBase)
                : valorBase;

        return valorFinal.setScale(2, RoundingMode.HALF_UP);
    }

    private long calcularHorasCheias(long minutosPermanencia) {
        return (long) Math.ceil(minutosPermanencia / 60.0);
    }

    private BigDecimal aplicarDesconto(BigDecimal valorBase) {
        BigDecimal fatorDesconto = BigDecimal.ONE.subtract(tarifaConfig.percentualDescontoMensalista());
        return valorBase.multiply(fatorDesconto);
    }
}
