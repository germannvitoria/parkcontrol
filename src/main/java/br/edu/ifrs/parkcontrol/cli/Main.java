package br.edu.ifrs.parkcontrol.cli;

import br.edu.ifrs.parkcontrol.model.TipoVaga;
import br.edu.ifrs.parkcontrol.model.Vaga;
import br.edu.ifrs.parkcontrol.repository.TicketRepository;
import br.edu.ifrs.parkcontrol.repository.VagaRepository;
import br.edu.ifrs.parkcontrol.repository.impl.ArquivoTicketRepository;
import br.edu.ifrs.parkcontrol.repository.impl.ArquivoVagaRepository;
import br.edu.ifrs.parkcontrol.service.CalculadoraDeValor;
import br.edu.ifrs.parkcontrol.service.EstacionamentoService;
import br.edu.ifrs.parkcontrol.service.TarifaConfig;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Scanner;

/**
 * Ponto de entrada do ParkControl. Monta as dependencias (persistencia em
 * arquivo, calculadora de tarifa, relogio do sistema) e inicia o loop de
 * comandos da CLI.
 */
public final class Main {

    private static final int VAGAS_COMUNS = 10;
    private static final int VAGAS_PCD = 2;
    private static final int VAGAS_IDOSO = 2;
    private static final int VAGAS_MENSALISTA = 3;

    private Main() {
    }

    public static void main(String[] args) {
        String diretorioDados = args.length > 0 ? args[0] : "./data";

        VagaRepository vagaRepository = new ArquivoVagaRepository(Path.of(diretorioDados, "vagas.json"));
        TicketRepository ticketRepository = new ArquivoTicketRepository(Path.of(diretorioDados, "tickets.json"));

        popularVagasIniciaisSeNecessario(vagaRepository);

        CalculadoraDeValor calculadoraDeValor = new CalculadoraDeValor(TarifaConfig.padrao());
        EstacionamentoService service = new EstacionamentoService(
                vagaRepository, ticketRepository, calculadoraDeValor, Clock.systemDefaultZone());

        ParkControlApp app = new ParkControlApp(service, new Scanner(System.in), System.out);
        app.executar();
    }

    private static void popularVagasIniciaisSeNecessario(VagaRepository vagaRepository) {
        if (!vagaRepository.listarTodas().isEmpty()) {
            return;
        }
        int numero = 1;
        for (int i = 0; i < VAGAS_COMUNS; i++) {
            vagaRepository.salvar(new Vaga(numero++, TipoVaga.COMUM));
        }
        for (int i = 0; i < VAGAS_PCD; i++) {
            vagaRepository.salvar(new Vaga(numero++, TipoVaga.PCD));
        }
        for (int i = 0; i < VAGAS_IDOSO; i++) {
            vagaRepository.salvar(new Vaga(numero++, TipoVaga.IDOSO));
        }
        for (int i = 0; i < VAGAS_MENSALISTA; i++) {
            vagaRepository.salvar(new Vaga(numero++, TipoVaga.MENSALISTA));
        }
    }
}
