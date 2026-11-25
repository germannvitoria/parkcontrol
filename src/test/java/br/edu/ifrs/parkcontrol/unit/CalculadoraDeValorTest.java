package br.edu.ifrs.parkcontrol.unit;

import br.edu.ifrs.parkcontrol.service.CalculadoraDeValor;
import br.edu.ifrs.parkcontrol.service.TarifaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes unitarios da regra de calculo de tarifa.
 * Tarifa de referencia (ver TarifaConfig.padrao()): R$ 6,00/hora,
 * 15 minutos de tolerancia gratuita, 50% de desconto para mensalistas.
 */
class CalculadoraDeValorTest {

    private CalculadoraDeValor calculadora;
    private LocalDateTime entrada;

    @BeforeEach
    void setUp() {
        calculadora = new CalculadoraDeValor(TarifaConfig.padrao());
        entrada = LocalDateTime.of(2026, 1, 10, 8, 0);
    }

    @Test
    @DisplayName("Permanencia dentro da tolerancia gratuita nao gera cobranca")
    void permanenciaDentroDaToleranciaNaoCobra() {
        LocalDateTime saida = entrada.plusMinutes(15);
        BigDecimal valor = calculadora.calcular(entrada, saida, false);
        assertEquals(new BigDecimal("0.00"), valor);
    }

    @Test
    @DisplayName("Permanencia de 1 minuto alem da tolerancia cobra 1 hora cheia")
    void permanenciaLogoAposToleranciaCobraHoraCheia() {
        LocalDateTime saida = entrada.plusMinutes(16);
        BigDecimal valor = calculadora.calcular(entrada, saida, false);
        assertEquals(new BigDecimal("6.00"), valor);
    }

    @Test
    @DisplayName("Fracao de hora iniciada e cobrada como hora cheia (arredondamento para cima)")
    void fracaoDeHoraCobraHoraCheia() {
        LocalDateTime saida = entrada.plusHours(1).plusMinutes(1);
        BigDecimal valor = calculadora.calcular(entrada, saida, false);
        assertEquals(new BigDecimal("12.00"), valor);
    }

    @Test
    @DisplayName("Permanencia de exatamente 2 horas cobra 2 horas, sem arredondar para cima")
    void permanenciaExataNaoArredondaParaCima() {
        LocalDateTime saida = entrada.plusHours(2);
        BigDecimal valor = calculadora.calcular(entrada, saida, false);
        assertEquals(new BigDecimal("12.00"), valor);
    }

    @Test
    @DisplayName("Mensalista recebe o percentual de desconto configurado")
    void mensalistaRecebeDesconto() {
        LocalDateTime saida = entrada.plusHours(3);
        BigDecimal valor = calculadora.calcular(entrada, saida, true);
        // 3h * 6.00 = 18.00; 50% de desconto = 9.00
        assertEquals(new BigDecimal("9.00"), valor);
    }

    @Test
    @DisplayName("Saida anterior a entrada lanca excecao")
    void saidaAnteriorAEntradaLancaExcecao() {
        LocalDateTime saidaInvalida = entrada.minusMinutes(1);
        assertThrows(IllegalArgumentException.class,
                () -> calculadora.calcular(entrada, saidaInvalida, false));
    }

    @Test
    @DisplayName("Construtor rejeita TarifaConfig nula")
    void construtorRejeitaTarifaConfigNula() {
        assertThrows(NullPointerException.class, () -> new CalculadoraDeValor(null));
    }

    @Test
    @DisplayName("TarifaConfig rejeita valor por hora negativo")
    void tarifaConfigRejeitaValorPorHoraNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> new TarifaConfig(new BigDecimal("-1.00"), 15, new BigDecimal("0.5")));
    }

    @Test
    @DisplayName("TarifaConfig rejeita percentual de desconto fora do intervalo [0,1]")
    void tarifaConfigRejeitaPercentualDescontoInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new TarifaConfig(new BigDecimal("6.00"), 15, new BigDecimal("1.5")));
    }
}
