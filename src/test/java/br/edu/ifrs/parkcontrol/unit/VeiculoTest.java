package br.edu.ifrs.parkcontrol.unit;

import br.edu.ifrs.parkcontrol.model.TipoVaga;
import br.edu.ifrs.parkcontrol.model.Veiculo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class VeiculoTest {

    @Test
    void criaVeiculoValidoComSucesso() {
        assertDoesNotThrow(() -> new Veiculo("ABC1D23", TipoVaga.COMUM, false));
    }

    @Test
    void rejeitaPlacaNula() {
        assertThrows(NullPointerException.class, () -> new Veiculo(null, TipoVaga.COMUM, false));
    }

    @Test
    void rejeitaPlacaVazia() {
        assertThrows(IllegalArgumentException.class, () -> new Veiculo("  ", TipoVaga.COMUM, false));
    }

    @Test
    void rejeitaTipoClienteNulo() {
        assertThrows(NullPointerException.class, () -> new Veiculo("ABC1D23", null, false));
    }

    @Test
    void rejeitaTipoClienteMensalistaComoTipoDeElegibilidade() {
        // MENSALISTA e um tipo de VAGA, nao um tipo de cliente; a assinatura
        // mensal e representada pelo campo booleano separado.
        assertThrows(IllegalArgumentException.class,
                () -> new Veiculo("ABC1D23", TipoVaga.MENSALISTA, true));
    }
}
