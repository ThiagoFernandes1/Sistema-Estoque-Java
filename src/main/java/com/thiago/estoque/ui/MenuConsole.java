package com.thiago.estoque.ui;

import com.thiago.estoque.dao.CategoriaDAO;
import com.thiago.estoque.dao.ProdutoDAO;
import com.thiago.estoque.model.Categoria;
import com.thiago.estoque.model.Produto;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;

/** Menu de texto que expoe as operacoes do estoque no terminal. */
public class MenuConsole {

    private static final Locale BR = Locale.of("pt", "BR");

    private final Scanner entrada = new Scanner(System.in);
    private final ProdutoDAO produtoDAO = new ProdutoDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    public void iniciar() {
        boolean executando = true;

        while (executando) {
            exibirMenu();
            switch (lerTexto("Opcao: ")) {
                case "1" -> listarProdutos();
                case "2" -> buscarProduto();
                case "3" -> cadastrarProduto();
                case "4" -> atualizarProduto();
                case "5" -> excluirProduto();
                case "6" -> movimentar('E');
                case "7" -> movimentar('S');
                case "8" -> relatorio();
                case "0" -> executando = false;
                default  -> System.out.println("Opcao invalida.");
            }
        }
        System.out.println("\nAte logo!");
    }

    private void exibirMenu() {
        System.out.println("""

                ================ SISTEMA DE ESTOQUE ================
                 1 - Listar produtos          5 - Excluir produto
                 2 - Buscar por nome          6 - Registrar entrada
                 3 - Cadastrar produto        7 - Registrar saida
                 4 - Atualizar produto        8 - Relatorio
                 0 - Sair
                ====================================================""");
    }

    /* ------------------------------ Operacoes ------------------------------ */

    private void listarProdutos() {
        try {
            imprimirTabela(produtoDAO.listarTodos());
        } catch (SQLException e) {
            erro(e);
        }
    }

    private void buscarProduto() {
        String termo = lerTexto("Nome (ou parte dele): ");
        try {
            List<Produto> encontrados = produtoDAO.buscarPorNome(termo);
            if (encontrados.isEmpty()) {
                System.out.println("Nenhum produto encontrado para \"" + termo + "\".");
            } else {
                imprimirTabela(encontrados);
            }
        } catch (SQLException e) {
            erro(e);
        }
    }

    private void cadastrarProduto() {
        try {
            String nome = lerTexto("Nome: ");
            if (nome.isBlank()) {
                System.out.println("Nome nao pode ser vazio.");
                return;
            }

            int categoriaId = escolherCategoria();
            if (categoriaId < 0) {
                return;
            }

            BigDecimal preco = lerDecimal("Preco: ");
            int quantidade = lerInteiro("Quantidade inicial: ");
            int estoqueMin = lerInteiro("Estoque minimo: ");

            if (preco.signum() < 0 || quantidade < 0 || estoqueMin < 0) {
                System.out.println("Preco, quantidade e minimo nao podem ser negativos.");
                return;
            }

            Produto p = new Produto(nome, categoriaId, preco, quantidade, estoqueMin);
            int id = produtoDAO.inserir(p);
            System.out.println("Produto cadastrado com o id " + id + ".");

        } catch (SQLException e) {
            erro(e);
        }
    }

    private void atualizarProduto() {
        try {
            int id = lerInteiro("Id do produto: ");
            Optional<Produto> encontrado = produtoDAO.buscarPorId(id);

            if (encontrado.isEmpty()) {
                System.out.println("Produto " + id + " nao encontrado.");
                return;
            }

            Produto p = encontrado.get();
            System.out.println("Editando: " + p.getNome() + " (deixe em branco para manter)");

            String nome = lerTexto("Nome [" + p.getNome() + "]: ");
            if (!nome.isBlank()) {
                p.setNome(nome);
            }

            String precoTexto = lerTexto("Preco [" + p.getPreco() + "]: ");
            if (!precoTexto.isBlank()) {
                p.setPreco(new BigDecimal(precoTexto.replace(",", ".")));
            }

            String minTexto = lerTexto("Estoque minimo [" + p.getEstoqueMin() + "]: ");
            if (!minTexto.isBlank()) {
                p.setEstoqueMin(Integer.parseInt(minTexto));
            }

            String trocarCategoria = lerTexto("Trocar categoria? (s/N): ");
            if (trocarCategoria.equalsIgnoreCase("s")) {
                int categoriaId = escolherCategoria();
                if (categoriaId < 0) {
                    return;
                }
                p.setCategoriaId(categoriaId);
            }

            System.out.println(produtoDAO.atualizar(p)
                    ? "Produto atualizado."
                    : "Nada foi alterado.");

        } catch (NumberFormatException e) {
            System.out.println("Valor numerico invalido.");
        } catch (SQLException e) {
            erro(e);
        }
    }

    private void excluirProduto() {
        try {
            int id = lerInteiro("Id do produto: ");
            Optional<Produto> encontrado = produtoDAO.buscarPorId(id);

            if (encontrado.isEmpty()) {
                System.out.println("Produto " + id + " nao encontrado.");
                return;
            }

            String confirmacao = lerTexto("Excluir \"" + encontrado.get().getNome() + "\"? (s/N): ");
            if (!confirmacao.equalsIgnoreCase("s")) {
                System.out.println("Operacao cancelada.");
                return;
            }

            System.out.println(produtoDAO.excluir(id)
                    ? "Produto excluido."
                    : "Nao foi possivel excluir.");

        } catch (SQLException e) {
            erro(e);
        }
    }

    private void movimentar(char tipo) {
        String rotulo = (tipo == 'E') ? "entrada" : "saida";
        try {
            int id = lerInteiro("Id do produto: ");
            int quantidade = lerInteiro("Quantidade de " + rotulo + ": ");

            if (quantidade <= 0) {
                System.out.println("Quantidade deve ser maior que zero.");
                return;
            }

            int saldo = produtoDAO.movimentar(id, tipo, quantidade);
            System.out.printf("Movimentacao registrada. Estoque atual: %d unidade(s).%n", saldo);

        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        } catch (SQLException e) {
            erro(e);
        }
    }

    private void relatorio() {
        try {
            List<Produto> produtos = produtoDAO.listarTodos();
            List<Produto> baixos = produtoDAO.listarEstoqueBaixo();
            BigDecimal total = produtoDAO.valorTotalEstoque();

            int unidades = produtos.stream().mapToInt(Produto::getQuantidade).sum();

            System.out.println("\n--------------- RELATORIO ---------------");
            System.out.printf("Produtos cadastrados : %d%n", produtos.size());
            System.out.printf("Unidades em estoque  : %d%n", unidades);
            System.out.printf(BR, "Valor total          : R$ %,.2f%n", total);

            if (baixos.isEmpty()) {
                System.out.println("\nNenhum produto com estoque baixo.");
            } else {
                System.out.println("\nProdutos com estoque baixo:");
                for (Produto p : baixos) {
                    System.out.printf("  [%d] %-32s %3d un. (min. %d)%n",
                            p.getId(), p.getNome(), p.getQuantidade(), p.getEstoqueMin());
                }
            }
            System.out.println("-----------------------------------------");

        } catch (SQLException e) {
            erro(e);
        }
    }

    /* ------------------------------ Auxiliares ----------------------------- */

    /** Lista as categorias e devolve o id escolhido, ou -1 se a escolha for invalida. */
    private int escolherCategoria() throws SQLException {
        List<Categoria> categorias = categoriaDAO.listar();

        if (categorias.isEmpty()) {
            System.out.println("Nenhuma categoria cadastrada. Rode o script database/schema.sql.");
            return -1;
        }

        System.out.println("Categorias disponiveis:");
        categorias.forEach(c -> System.out.println("  " + c));

        int id = lerInteiro("Id da categoria: ");
        if (!categoriaDAO.existe(id)) {
            System.out.println("Categoria " + id + " nao existe.");
            return -1;
        }
        return id;
    }

    private void imprimirTabela(List<Produto> produtos) {
        if (produtos.isEmpty()) {
            System.out.println("Nenhum produto cadastrado.");
            return;
        }

        System.out.printf("%n%-4s %-32s %-14s %12s %6s %s%n",
                "ID", "PRODUTO", "CATEGORIA", "PRECO", "QTD", "");
        System.out.println("-".repeat(80));

        for (Produto p : produtos) {
            System.out.printf(BR, "%-4d %-32s %-14s %12.2f %6d %s%n",
                    p.getId(),
                    truncar(p.getNome(), 32),
                    truncar(p.getCategoriaNome(), 14),
                    p.getPreco(),
                    p.getQuantidade(),
                    p.isEstoqueBaixo() ? "<- estoque baixo" : "");
        }
    }

    private String truncar(String texto, int limite) {
        if (texto == null) {
            return "";
        }
        return texto.length() <= limite ? texto : texto.substring(0, limite - 1) + "…";
    }

    private String lerTexto(String rotulo) {
        System.out.print(rotulo);
        return entrada.hasNextLine() ? entrada.nextLine().trim() : "";
    }

    private int lerInteiro(String rotulo) {
        while (true) {
            try {
                return Integer.parseInt(lerTexto(rotulo));
            } catch (NumberFormatException e) {
                System.out.println("Digite um numero inteiro valido.");
            }
        }
    }

    private BigDecimal lerDecimal(String rotulo) {
        while (true) {
            try {
                return new BigDecimal(lerTexto(rotulo).replace(",", "."));
            } catch (NumberFormatException e) {
                System.out.println("Digite um valor valido (ex.: 199.90).");
            }
        }
    }

    private void erro(SQLException e) {
        System.out.println("Erro de banco de dados: " + e.getMessage());
    }
}
