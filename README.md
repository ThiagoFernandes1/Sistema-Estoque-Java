# ☕ Sistema de Estoque — Java + SQL Server

Sistema de controle de estoque em **Java 17** com **Microsoft SQL Server**, usando **JDBC puro** (sem ORM). O objetivo é mostrar os fundamentos de acesso a dados bem feitos: `PreparedStatement` em toda consulta, transações com `commit`/`rollback`, chaves geradas pelo banco e SQL com restrições de integridade de verdade.

![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=openjdk&logoColor=white)
![SQL Server](https://img.shields.io/badge/SQL%20Server-CC2927?style=flat-square&logo=microsoftsqlserver&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat-square&logo=apachemaven&logoColor=white)

---

## Funcionalidades

| # | Operação | O que faz |
|---|---|---|
| 1 | Listar produtos | Tabela com id, categoria, preço, quantidade e alerta de estoque baixo |
| 2 | Buscar por nome | Busca parcial com `LIKE` parametrizado |
| 3 | Cadastrar produto | `INSERT` com recuperação do id gerado (`RETURN_GENERATED_KEYS`) |
| 4 | Atualizar produto | `UPDATE` campo a campo, mantendo o valor atual quando deixado em branco |
| 5 | Excluir produto | `DELETE` com confirmação; movimentações caem em cascata |
| 6 | Registrar entrada | Soma ao estoque e grava a movimentação na mesma transação |
| 7 | Registrar saída | Idem, e **recusa** a saída se não houver saldo suficiente |
| 8 | Relatório | Total de itens, unidades, valor imobilizado e lista de estoque baixo |

---

## Estrutura

```
Sistema-Estoque-Java/
├── database/
│   └── schema.sql                  # Criação do banco, tabelas, view e dados de exemplo
├── src/main/java/com/thiago/estoque/
│   ├── Main.java                   # Ponto de entrada: testa a conexão e abre o menu
│   ├── db/Conexao.java             # Abertura de conexões + leitura das credenciais
│   ├── model/                      # Produto, Categoria
│   ├── dao/                        # ProdutoDAO, CategoriaDAO — todo o SQL vive aqui
│   └── ui/MenuConsole.java         # Menu de texto
├── src/main/resources/db.properties
└── pom.xml
```

A separação é a clássica em três camadas: a **UI** não conhece SQL, os **DAOs** não imprimem nada, e o **model** não sabe que existe um banco.

---

## Banco de dados

Três tabelas e uma view:

- **Categoria** — categorias dos produtos, nome único.
- **Produto** — FK para categoria, `CHECK` garantindo preço e quantidade não negativos, estoque mínimo com default.
- **Movimentacao** — histórico de entradas e saídas, `CHECK (tipo IN ('E','S'))`, `ON DELETE CASCADE`.
- **vw_EstoqueBaixo** — view com os produtos no ou abaixo do mínimo.

As regras que protegem os dados estão no banco, não só no Java — é o banco que tem a última palavra.

---

## Como rodar

### Pré-requisitos

- JDK 17 ou superior
- Maven 3.8+
- SQL Server (Express serve) com TCP/IP habilitado na porta 1433

### 1. Criar o banco

Abra `database/schema.sql` no SQL Server Management Studio e execute, ou pela linha de comando:

```bash
sqlcmd -S localhost -U sa -P SuaSenha -i database/schema.sql
```

O script cria o banco `EstoqueDB`, as tabelas, a view e insere 6 produtos de exemplo.

### 2. Configurar as credenciais

Edite `src/main/resources/db.properties`:

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=EstoqueDB;encrypt=true;trustServerCertificate=true
db.user=sa
db.password=SuaSenhaAqui
```

Para autenticação integrada do Windows, troque a URL por:

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=EstoqueDB;integratedSecurity=true;encrypt=true;trustServerCertificate=true
```

> As variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD` têm prioridade sobre o arquivo — use-as para não versionar senha nenhuma.

### 3. Compilar e executar

```bash
mvn clean package
java -jar target/sistema-estoque-1.0.0.jar
```

Ou, direto pelo Maven, sem gerar o jar:

```bash
mvn compile exec:java
```

---

## Exemplo de uso

```
================ SISTEMA DE ESTOQUE ================
 1 - Listar produtos          5 - Excluir produto
 2 - Buscar por nome          6 - Registrar entrada
 3 - Cadastrar produto        7 - Registrar saida
 4 - Atualizar produto        8 - Relatorio
 0 - Sair
====================================================
Opcao: 1

ID   PRODUTO                          CATEGORIA            PRECO    QTD
--------------------------------------------------------------------------------
5    Headset Surround 7.1             Acessórios          349,00      2 <- estoque baixo
1    Teclado Mecânico RGB             Periféricos         289,90     24
3    Notebook Ryzen 7 16GB            Notebooks          4599,00      3
```

---

## Detalhes técnicos

**Transação na movimentação de estoque.** Atualizar a quantidade e gravar o histórico são duas escritas que precisam acontecer juntas. `ProdutoDAO.movimentar` desliga o auto-commit, executa as duas e só então dá `commit`; qualquer falha no caminho dispara `rollback`.

**Saída sem saldo negativo.** A saída não faz "lê, confere, grava" — o próprio `UPDATE` carrega a condição `AND quantidade >= ?`. Se duas saídas simultâneas disputarem o mesmo produto, uma delas afeta zero linhas e é recusada, em vez das duas lerem o mesmo saldo e derrubarem o estoque para negativo.

**SQL injection.** Todo parâmetro passa por `PreparedStatement`, inclusive a busca com `LIKE`. Nenhuma entrada do usuário é concatenada em SQL.

**Recursos fechados.** Todas as consultas usam `try-with-resources`, de modo que `Connection`, `PreparedStatement` e `ResultSet` fecham mesmo em caso de exceção.

---

## Autor

**Thiago Fernandes**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-0A66C2?style=flat-square&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/thiago-fernandes-almeida-03228228a/)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/ThiagoFernandes1)
