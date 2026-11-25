# ParkControl — Sistema de Controle de Estacionamento

Projeto desenvolvido para a disciplina de Verificação e Validação de
Sistemas. Sistema em Java que controla entrada/saída de veículos,
calcula valores cobrados por tempo de permanência, gerencia disponibilidade
de vagas por tipo (comum, PCD, idoso, mensalista) e aplica desconto para
mensalistas. Operado via linha de comando (CLI).

O plano de teste completo está em [`docs/plano-de-teste.md`](docs/plano-de-teste.md).

## Pré-requisitos

- **Java 17 ou superior** (JDK, não apenas JRE)
- **Maven 3.9 ou superior**

### Instalando (se ainda não tiver)

**Windows:** baixe o instalador do [Adoptium Temurin](https://adoptium.net/) (JDK 17+)
e do [Maven](https://maven.apache.org/download.cgi), seguindo os instaladores.

**Mac (com Homebrew):**
```bash
brew install openjdk@17 maven
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-17-jdk maven
```

Verifique a instalação:
```bash
java -version
mvn -version
```

## Como rodar os testes

```bash
# Roda os testes unitarios e de componente
mvn test

# Roda TODOS os testes (unitario + componente + sistema) + analise estatica (PMD)
# + gera relatorio de cobertura (JaCoCo)
mvn verify
```

Ao final, os relatórios ficam em:
- **Cobertura de código (JaCoCo):** `target/site/jacoco/index.html` (abra no navegador)
- **Análise estática (PMD):** `target/pmd.xml`

## Como rodar o sistema (CLI)

```bash
mvn compile exec:java -Dexec.mainClass="br.edu.ifrs.parkcontrol.cli.Main"
```

Ou, para gerar um `.jar` executável e rodá-lo diretamente:

```bash
mvn package
java -jar target/parkcontrol-jar-with-dependencies.jar
```

Na primeira execução, o sistema cria automaticamente uma pasta `data/` com
os arquivos `vagas.json` e `tickets.json`, já populados com um conjunto
inicial de vagas (10 comuns, 2 PCD, 2 idoso, 3 mensalistas).

### Comandos disponíveis na CLI

```
entrada <placa> <COMUM|PCD|IDOSO> <s|n>   - registra entrada (s/n = e mensalista?)
saida <placa>                              - registra saida e calcula valor
disponibilidade <COMUM|PCD|IDOSO|MENSALISTA> - consulta vagas livres do tipo
listar                                      - lista todas as vagas e status
ajuda                                       - mostra os comandos
sair                                        - encerra o programa
```

Exemplo de sessão:
```
parkcontrol> entrada ABC1D23 COMUM n
Entrada registrada. Placa=ABC1D23 vaga=1 entrada=2026-01-10T08:00
parkcontrol> disponibilidade COMUM
Vagas disponiveis do tipo COMUM: 9
parkcontrol> saida ABC1D23
Saida registrada. Placa=ABC1D23 valor cobrado=R$ 6.00
parkcontrol> sair
```

## Estrutura do projeto

```
parkcontrol/
├── docs/
│   └── plano-de-teste.md
├── src/
│   ├── main/java/br/edu/ifrs/parkcontrol/
│   │   ├── model/           # Vaga, Veiculo, Ticket, TipoVaga
│   │   ├── service/         # EstacionamentoService, CalculadoraDeValor, TarifaConfig
│   │   ├── repository/      # Interfaces (VagaRepository, TicketRepository)
│   │   │   └── impl/        # Implementacao em arquivo JSON
│   │   ├── cli/             # ParkControlApp (CLI), Main (entry point)
│   │   └── exception/       # Excecoes de dominio
│   └── test/java/br/edu/ifrs/parkcontrol/
│       ├── unit/            # Testes unitarios (JUnit + Mockito)
│       ├── component/       # Testes de componente (repositorios, I/O real)
│       └── system/          # Teste de sistema (fluxo completo via CLI)
├── pom.xml
└── README.md
```

## Observação sobre o ambiente de desenvolvimento

Este projeto foi desenvolvido com auxílio do Claude (Anthropic). O ambiente
de geração de código não teve acesso ao Maven Central durante o
desenvolvimento, portanto os testes não foram executados automaticamente
durante a criação — **é necessário rodar `mvn verify` localmente** para
confirmar que tudo compila e passa, conforme instruções acima.
