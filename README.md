# ParkControl — Sistema de Controle de Estacionamento

Sistema em Java que controla entrada/saída de veículos,
calcula valores cobrados por tempo de permanência, gerencia disponibilidade
de vagas por tipo (comum, PCD, idoso, mensalista) e aplica desconto para
mensalistas. Operado via linha de comando (CLI).

O plano de teste completo está em [`docs/plano-de-teste.md`](docs/plano-de-teste.md).

## Pré-requisitos

- **Java 17 ou superior (no máximo java 21)** (JDK, não apenas JRE)
- **Maven 3.9 ou superior**

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
