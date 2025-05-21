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

O sistema foi projetado com o objetivo de promover a interação entre usuários praticantes de atividades físicas, permitindo o monitoramento de atividades em tempo real e a formação de grupos para incentivo mútuo através de uma rede social focada em bem-estar e saúde.

### ✅ Requisitos do Software

- ✅ Permitir apenas o acesso ao software por meio de login utilizando uma rede social (**Google**, **Facebook**, etc.).  
- ✅ Após o login, o usuário deve ter a opção de **iniciar o monitoramento**, que realizará a leitura do **sensor de acelerômetro**, identificando o nível de movimento do dispositivo e, consequentemente, da pessoa.  
- ✅ As informações coletadas devem ser **enviadas a um servidor remoto**, juntamente com o **nome do usuário** e a **data/hora** correspondente.  
- ✅ O aplicativo deve apresentar uma **listagem individual** do nível de atividade das pessoas que utilizam o sistema, formando um **ranking**.  
- ✅ Permitir a criação de **grupos** para **classificação** e interação entre os usuários.

---

## ⚙️ Tecnologias Utilizadas

### 📱 Mobile

- **Kotlin** com **Jetpack Compose** — para construção de interfaces modernas, declarativas e responsivas.
- **Cloud Firestore** — banco de dados NoSQL em tempo real, utilizado para armazenar e sincronizar dados entre os usuários.

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
- PRs devem ser revisados pelo **Scrum Master**.

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
