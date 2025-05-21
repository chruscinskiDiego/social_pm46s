# 🏋️‍♂️ Rede Social de Atividade Física

Bem-vindo ao projeto **Rede Social de Atividade Física**, uma aplicação desenvolvida como parte da disciplina **Tópicos Avançados em Programação para Dispositivos Móveis (PM46S-2025-1)**, do curso de **Análise e Desenvolvimento de Sistemas (ADS)** da **Universidade Tecnológica Federal do Paraná (UTFPR)**.

---

## 💻 Equipe do Projeto

### 🧙 Scrum Master
- Marcos Felipe Altenhofen

### 👨‍💻 Devs
- Diego Chruscinski de Souza  
- João Gabriel Jarutais  
- Gustavo Moretto Dalla Costa  
- Geovane de Campos Soares  

---

## 🧠 Sobre o Projeto

### Visão Geral
Este aplicativo Android visa **mensurar o nível de atividade física** dos usuários, funcionando como uma rede social focada em bem-estar e saúde. O app utilizará o sensor acelerômetro do dispositivo para identificar o movimento, enviar esses dados para um servidor e apresentar um ranking de participação individual e por grupos, promovendo a interação e o incentivo mútuo entre os praticantes de atividades físicas.

### Objetivo
Incentivar a prática de atividade física permitindo que os usuários **monitorem seus movimentos**, **compartilhem seus resultados**, **comparem seu desempenho** com outros participantes e **formem grupos** para classificação e interação, fomentando um ambiente de desafio saudável e colaborativo.

### Público-Alvo
Pessoas de todas as idades interessadas em registrar sua atividade física, participar de desafios, interagir com outros usuários com interesses semelhantes e buscar uma forma motivadora de se manter ativo utilizando um smartphone Android.

---

## ✅ Requisitos do Software

- ✅ Permitir apenas o acesso ao software por meio de login utilizando uma rede social (**Google**, **Facebook**, etc.).
- ✅ Após o login, o usuário deve ter a opção de **iniciar o monitoramento**, que realizará a leitura do **sensor de acelerômetro**, identificando o nível de movimento do dispositivo e, consequentemente, da pessoa.
- ✅ As informações coletadas devem ser **enviadas a um servidor remoto**, juntamente com o **nome do usuário** e a **data/hora** correspondente.
- ✅ O aplicativo deve apresentar uma **listagem individual** do nível de atividade das pessoas que utilizam o sistema, formando um **ranking**.
- ✅ Permitir a criação de **grupos** para **classificação** e interação entre os usuários.

---

## 🚀 Funcionalidades Detalhadas

### 1. Tela de Login
- Opção de login **exclusivamente via redes sociais** (ex: Google, Facebook) utilizando Firebase Authentication.
- Ao autenticar, obter nome do usuário (ou um identificador único) para o perfil e ranking.

### 2. Tela Principal (Pós-Login)
- **Botão "Iniciar Monitoramento"**:
    - Ativa a leitura do sensor acelerômetro.
    - Indica visualmente que o monitoramento está ativo.
- **Botão "Parar Monitoramento"**:
    - Interrompe a leitura do sensor.
- **Acesso à Tela de Ranking**.
- **Acesso à Tela de Gerenciamento de Grupos**.

### 3. Monitoramento de Atividade (Serviço em Primeiro Plano)
- Ao iniciar o monitoramento:
    - **Leitura contínua do sensor acelerômetro**.
    - **Processamento dos dados do acelerômetro** para classificar o nível de atividade.
    - **Envio periódico dos dados para o Cloud Firestore**:
        - Nome do usuário (ou ID).
        - Nível de atividade detectado.
        - Data/Hora da medição.
- O monitoramento será eficiente para otimizar o consumo de bateria.

### 4. Tela: Ranking de Atividade
- **Listagem individual**:
    - Exibe o nome do usuário (ou apelido).
    - Nível de atividade acumulado ou pontuação.
    - Classificação geral.
- **Filtro/Visualização por Grupos** (se o usuário pertencer a algum grupo).
- Atualização dos dados em tempo real a partir do Cloud Firestore.

### 5. Tela: Gerenciamento de Grupos
- **Botão "Criar Grupo"**:
    - Campo para nome do grupo.
- **Listar Grupos Existentes** para o usuário entrar.
- **Visualizar Membros do Grupo**.
- **Ranking do Grupo** (soma ou média da atividade dos membros).

---

## ⚙️ Tecnologias Utilizadas

| Recurso                        | API/Ferramenta/Linguagem                                                              |
| :----------------------------- | :------------------------------------------------------------------------------------ |
| Linguagem de Programação       | **Kotlin** |
| UI Toolkit                     | **Jetpack Compose** — para construção de interfaces modernas, declarativas e responsivas. |
| Banco de Dados NoSQL           | **Cloud Firestore** — para armazenar e sincronizar dados entre os usuários em tempo real. |
| Autenticação                   | **Firebase Authentication** (Google Sign-In, Facebook Login, etc.)                    |
| Sensor de Movimento            | Android SensorManager (TYPE_ACCELEROMETER)                                            |
| Comunicação com Backend        | SDK do Firebase para Android                                                          |
| Gerenciamento de Dependências  | Gradle                                                                                |
| IDE                            | Android Studio                                                                        |

---

## 📁 Estrutura do Fluxo de Trabalho do Git

Para manter um desenvolvimento organizado e colaborativo, adotamos um fluxo de trabalho baseado em branches com as seguintes diretrizes:

### 🔀 Branches principais

- **`prd`**: Contém a versão estável e pronta para produção. Nenhum commit direto deve ser feito nesta branch.
- **`qa`**: Branch intermediária entre `dev` e `prd`, usada para testes e ajustes finais antes de uma nova versão ser publicada.
- **`dev`**: Branch de desenvolvimento contínuo. Todas as novas funcionalidades e correções partem daqui.

> O ciclo típico é: `dev` → `qa` → `prd`

---

### 🌿 Branches secundárias

- **`[id]-feature/nome-da-feature`**: Para o desenvolvimento de novas funcionalidades.
- **`[id]-fix/nome-do-fix`**: Para correções de bugs ou melhorias pontuais.
- **`[id]-hotfix/nome-do-hotfix`**: Para correções urgentes aplicadas diretamente na `prd`.
- **`[id]-docs/nome-da-documentacao`**: Para alterações relacionadas à documentação do projeto.

> 🔤 **Padrão de nomeação:** Utilizamos o padrão `kebab-case` (palavras separadas por hífens) com prefixo numérico identificando o ID da task, facilitando a rastreabilidade com ferramentas de gerenciamento de tarefas.

> 🔧 Exemplos de nome de branch:
> - `102-feature/adicionar-feed`
> - `115-fix/erro-login`
> - `120-release/v1-0-0`

---

### ✅ Pull Requests

- Toda mudança deve ser enviada via **Pull Request** para a branch apropriada.
- **Features e fixes** são integrados em `dev`.
- Após validação e testes, `dev` é mesclada na `qa`.
- Após testes finais, `qa` é integrada na `prd` (e em `dev` novamente, se necessário).
- PRs devem ser revisados pelo **Scrum Master** (ou um colega designado).

---

### 📝 Commits

- Commits devem ser claros, concisos e utilizar prefixos padronizados:
  - `feat:` nova funcionalidade
  - `fix:` correção de bug
  - `docs:` mudanças na documentação
  - `refactor:` melhoria/refatoração sem alterar funcionalidades
  - `test:` inclusão ou alteração de testes
  - `chore:` tarefas de manutenção/configuração

> ✍️ Exemplo: `feat: implementa sistema de cadastro de usuários`

---

Seguindo esse fluxo, garantimos organização, testes adequados e uma entrega estável e previsível para produção.
