package br.edu.ifrs.parkcontrol.service;

import java.math.BigDecimal;

/**
 * Configuracao de tarifas do estacionamento.
 *
 * @param valorPorHora           valor cobrado por hora (ou fracao de hora) de permanencia
 * @param toleranciaMinutosGratis tempo de permanencia, em minutos, que nao gera cobranca
 * @param percentualDescontoMensalista percentual de desconto (0.0 a 1.0) aplicado a mensalistas
 */
public record TarifaConfig(BigDecimal valorPorHora, int toleranciaMinutosGratis,
                            BigDecimal percentualDescontoMensalista) {

    public TarifaConfig {
        if (valorPorHora == null || valorPorHora.signum() < 0) {
            throw new IllegalArgumentException("Valor por hora deve ser nao-negativo");
        }
        if (toleranciaMinutosGratis < 0) {
            throw new IllegalArgumentException("Tolerancia nao pode ser negativa");
        }
        if (percentualDescontoMensalista == null
                || percentualDescontoMensalista.signum() < 0
                || percentualDescontoMensalista.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Percentual de desconto deve estar entre 0.0 e 1.0");
        }
    }

    /**
     * Configuracao padrao: R$ 6,00/hora, 15 minutos de tolerancia gratuita,
     * 50% de desconto para mensalistas.
     */
    public static TarifaConfig padrao() {
        return new TarifaConfig(new BigDecimal("6.00"), 15, new BigDecimal("0.50"));
    }
}
