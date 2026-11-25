# Plano de Teste — ParkControl

## 2. Escopo do teste

### 2.1 O que será testado

- **Regras de negócio de cálculo de tarifa**: tolerância gratuita,
  arredondamento de fração de hora, desconto para mensalistas
  (`CalculadoraDeValor`).
- **Orquestração do domínio**: registro de entrada e saída de veículos,
  alocação de vaga compatível, bloqueio de entrada duplicada, bloqueio de
  saída sem entrada registrada, consulta de disponibilidade
  (`EstacionamentoService`).
- **Persistência em arquivo**: gravação e leitura de vagas e tickets em
  arquivos JSON, garantindo que os dados sobrevivem entre execuções
  (`ArquivoVagaRepository`, `ArquivoTicketRepository`).
- **Comportamento do sistema do ponto de vista do usuário final**: os
  fluxos completos operados via CLI (entrada → consulta → saída), incluindo
  fluxos de erro (vaga indisponível, veículo duplicado, ticket inexistente).
- **Qualidade estática do código-fonte de produção** (`src/main`), via PMD.

### 2.2 O que não será testado

- **Desempenho e carga** (quantidade de requisições simultâneas, tempo de
  resposta sob stress) — fora do escopo deste trabalho, que é individual e
  de porte acadêmico. Não há testes de carga (K6) nesta entrega.
- **Segurança** (autenticação, autorização, injeção de dados) — o sistema
  não expõe rede nem múltiplos usuários concorrentes; não há testes de
  segurança nesta entrega.
- **Interface gráfica** — o sistema é operado via linha de comando (CLI);
  não há testes de usabilidade de UI gráfica.
- **Concorrência/multiusuário** — o ParkControl foi projetado para uso por
  um único operador por vez; condições de corrida em acesso simultâneo ao
  arquivo de persistência não são tratadas nem testadas.
- **Compatibilidade entre sistemas operacionais** — os testes assumem um
  ambiente com JDK 17+ e Maven, sem validação específica de Windows/Linux/Mac.

## 3. Estratégia de teste

| Nível | Técnica / Ferramenta | O que valida |
|---|---|---|
| Estático | PMD (via `maven-pmd-plugin`) | Boas práticas, código propenso a erro, complexidade, estilo, no código de produção (`src/main`) |
| Unitário | JUnit 5 + Mockito | Lógica pura de cálculo de tarifa (`CalculadoraDeValorTest`), validações de modelo (`VeiculoTest`) e orquestração de regras de negócio isolando as dependências de persistência com mocks (`EstacionamentoServiceTest`) |
| Componente | JUnit 5 + I/O real em diretório temporário (`@TempDir`) | Repositórios de persistência em arquivo (`ArquivoVagaRepositoryTest`, `ArquivoTicketRepositoryTest`), validando leitura/escrita real, sem mocks |
| Sistema | JUnit 5, simulando stdin/stdout da CLI | Fluxos completos de ponta a ponta do ponto de vista do usuário final (`ParkControlSystemTest`): fluxo principal e fluxos alternativos/erro |
| Cobertura | JaCoCo | Mede o percentual de código exercitado pelos testes unitários e de componente |

**Justificativa da arquitetura para testabilidade:** o sistema foi
estruturado em camadas (domínio → serviço → repositório → CLI) justamente
para permitir que cada nível de teste isole a camada correspondente: o
serviço depende de *interfaces* de repositório (não de implementações
concretas), o que permite substituí-las por mocks no teste unitário e por
implementações reais de arquivo no teste de componente. A CLI (`ParkControlApp`)
recebe `Scanner`/`PrintStream` injetados no construtor, o que permite
simular um usuário real no teste de sistema sem depender de
`System.in`/`System.out` globais.

## 4. Critérios de entrada e saída

### 4.1 Critérios de entrada (quando os testes começam)

- Código da camada de domínio e serviço implementado e compilando
  (`mvn compile` sem erros).
- Estrutura de pacotes definida (`model`, `service`, `repository`,
  `repository.impl`, `cli`, `exception`).
- Dependências de teste (JUnit 5, Mockito) configuradas no `pom.xml`.

### 4.2 Critérios de saída (quando os testes são considerados concluídos)

- `mvn verify` executa sem falhas de teste (todos os testes unitários, de
  componente e de sistema passam).
- Cobertura de código (JaCoCo) de pelo menos **80%** de instruções nas
  classes de `service` e `model` (regras de negócio), medida em
  `target/site/jacoco/index.html`.
- PMD executado com sucesso e relatório gerado em `target/pmd.xml`;
  apontamentos de prioridade alta (1–2) revisados e corrigidos ou
  justificados.
- Todos os fluxos descritos no escopo (item 2.1) cobertos por pelo menos um
  caso de teste automatizado.
- Nenhuma exceção não tratada (stack trace) observada ao operar os fluxos
  principal e alternativos via CLI.

## 5. Riscos e mitigação

| Risco | Probabilidade | Impacto | Mitigação |
|---|---|---|---|
| Erros de arredondamento no cálculo de tarifa (frações de hora, `BigDecimal`) | Média | Alto | Casos de teste unitários dedicados a valores-limite (exatamente na tolerância, 1 minuto acima, hora exata, fração) |
| Corrupção do arquivo JSON de persistência (escrita incompleta, concorrência) | Baixa | Alto | Teste de componente verificando leitura após escrita e persistência entre instâncias; escopo documentado como uso single-user |
| Regra de compatibilidade vaga/veículo implementada incorretamente (ex: mensalista ocupando vaga comum) | Média | Médio | Testes unitários específicos de alocação por tipo, incluindo o caso mensalista |
| Ambiente do avaliador sem Maven/Java configurado corretamente | Média | Médio | `README.md` com passo a passo de instalação e execução; versões de JDK/Maven documentadas |
| Prazo apertado para cobrir os 5 artefatos exigidos | Média | Alto | Cronograma (seção 6) com buffer antes da entrega final |

## 6. Cronograma das atividades

| Atividade |
|---|---|
| 1 | Definição do projeto e entrega da Etapa 1 (PDF) |
| 2 | Estruturação do projeto Maven, implementação do domínio (`model`, `service`) |
| 3 | Implementação da persistência em arquivo e da CLI |
| 4 | Escrita dos testes unitários e de componente |
| 5 | Escrita dos testes de sistema e configuração do PMD/JaCoCo |
| 6 | Revisão geral, correção de apontamentos do PMD, redação final do plano de teste e do README |
| 7 | Buffer de contingência e entrega final no repositório Git |

## 7. Ambiente e ferramentas utilizadas

| Item | Ferramenta/Versão |
|---|---|
| Linguagem | Java 17+ |
| Build e gerenciamento de dependências | Maven 3.9+ |
| Testes unitários e de componente | JUnit 5 (Jupiter) |
| Mocking | Mockito |
| Cobertura de código | JaCoCo |
| Análise estática | PMD (`maven-pmd-plugin`) |
| Persistência | Arquivos JSON (biblioteca Jackson) |
| Controle de versão | Git / GitHub |

