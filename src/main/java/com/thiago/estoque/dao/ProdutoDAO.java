package com.thiago.estoque.dao;

import com.thiago.estoque.db.Conexao;
import com.thiago.estoque.model.Produto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Operacoes de CRUD e movimentacao de estoque sobre dbo.Produto.
 *
 * Toda consulta usa PreparedStatement com parametros, nunca concatenacao
 * de string, para evitar SQL injection.
 */
public class ProdutoDAO {

    private static final String SELECT_BASE = """
            SELECT  p.id, p.nome, p.categoria_id, c.nome AS categoria,
                    p.preco, p.quantidade, p.estoque_min
            FROM    dbo.Produto   p
            JOIN    dbo.Categoria c ON c.id = p.categoria_id
            """;

    /* ------------------------------ Consultas ------------------------------ */

    public List<Produto> listarTodos() throws SQLException {
        String sql = SELECT_BASE + " ORDER BY p.nome";

        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                produtos.add(mapear(rs));
            }
        }
        return produtos;
    }

    public List<Produto> buscarPorNome(String termo) throws SQLException {
        String sql = SELECT_BASE + " WHERE p.nome LIKE ? ESCAPE '\\' ORDER BY p.nome";

        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + escaparLike(termo) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    produtos.add(mapear(rs));
                }
            }
        }
        return produtos;
    }

    public Optional<Produto> buscarPorId(int id) throws SQLException {
        String sql = SELECT_BASE + " WHERE p.id = ?";

        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Produtos cuja quantidade esta igual ou abaixo do minimo, via view do banco. */
    public List<Produto> listarEstoqueBaixo() throws SQLException {
        String sql = """
                SELECT id, nome, categoria, quantidade, estoque_min
                FROM   dbo.vw_EstoqueBaixo
                ORDER BY quantidade ASC
                """;

        List<Produto> produtos = new ArrayList<>();
        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Produto p = new Produto();
                p.setId(rs.getInt("id"));
                p.setNome(rs.getString("nome"));
                p.setCategoriaNome(rs.getString("categoria"));
                p.setQuantidade(rs.getInt("quantidade"));
                p.setEstoqueMin(rs.getInt("estoque_min"));
                p.setPreco(BigDecimal.ZERO);
                produtos.add(p);
            }
        }
        return produtos;
    }

    /** Valor total do estoque (soma de preco x quantidade). */
    public BigDecimal valorTotalEstoque() throws SQLException {
        String sql = "SELECT COALESCE(SUM(preco * quantidade), 0) AS total FROM dbo.Produto";

        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getBigDecimal("total") : BigDecimal.ZERO;
        }
    }

    /* ------------------------------- Escrita ------------------------------- */

    /** Insere o produto e devolve o id gerado pelo banco. */
    public int inserir(Produto p) throws SQLException {
        String sql = """
                INSERT INTO dbo.Produto (nome, categoria_id, preco, quantidade, estoque_min)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, p.getNome());
            ps.setInt(2, p.getCategoriaId());
            ps.setBigDecimal(3, p.getPreco());
            ps.setInt(4, p.getQuantidade());
            ps.setInt(5, p.getEstoqueMin());
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    p.setId(chaves.getInt(1));
                }
            }
        }
        return p.getId();
    }

    public boolean atualizar(Produto p) throws SQLException {
        String sql = """
                UPDATE dbo.Produto
                SET    nome = ?, categoria_id = ?, preco = ?, estoque_min = ?
                WHERE  id = ?
                """;

        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getNome());
            ps.setInt(2, p.getCategoriaId());
            ps.setBigDecimal(3, p.getPreco());
            ps.setInt(4, p.getEstoqueMin());
            ps.setInt(5, p.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean excluir(int id) throws SQLException {
        String sql = "DELETE FROM dbo.Produto WHERE id = ?";

        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Registra entrada ('E') ou saida ('S') de estoque.
     *
     * A atualizacao da quantidade e o registro da movimentacao acontecem na
     * mesma transacao: ou os dois comandos sao gravados, ou nenhum deles.
     * Na saida, o proprio UPDATE exige quantidade suficiente, de modo que
     * duas saidas simultaneas nao conseguem derrubar o estoque para negativo.
     *
     * @return quantidade em estoque depois da movimentacao
     * @throws SQLException se o produto nao existir ou faltar saldo
     */
    public int movimentar(int produtoId, char tipo, int quantidade) throws SQLException {
        if (tipo != 'E' && tipo != 'S') {
            throw new IllegalArgumentException("Tipo deve ser 'E' (entrada) ou 'S' (saida).");
        }
        if (quantidade <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero.");
        }

        String sqlUpdate = (tipo == 'E')
                ? "UPDATE dbo.Produto SET quantidade = quantidade + ? WHERE id = ?"
                : "UPDATE dbo.Produto SET quantidade = quantidade - ? WHERE id = ? AND quantidade >= ?";

        String sqlMov = """
                INSERT INTO dbo.Movimentacao (produto_id, tipo, quantidade)
                VALUES (?, ?, ?)
                """;

        String sqlSaldo = "SELECT quantidade FROM dbo.Produto WHERE id = ?";

        Connection conn = null;
        try {
            conn = Conexao.abrir();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(sqlUpdate)) {
                ps.setInt(1, quantidade);
                ps.setInt(2, produtoId);
                if (tipo == 'S') {
                    ps.setInt(3, quantidade);
                }

                if (ps.executeUpdate() == 0) {
                    conn.rollback();
                    throw new SQLException(tipo == 'S'
                            ? "Saida negada: produto inexistente ou saldo insuficiente."
                            : "Entrada negada: produto inexistente.");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlMov)) {
                ps.setInt(1, produtoId);
                ps.setString(2, String.valueOf(tipo));
                ps.setInt(3, quantidade);
                ps.executeUpdate();
            }

            int saldo;
            try (PreparedStatement ps = conn.prepareStatement(sqlSaldo)) {
                ps.setInt(1, produtoId);
                try (ResultSet rs = ps.executeQuery()) {
                    saldo = rs.next() ? rs.getInt(1) : 0;
                }
            }

            conn.commit();
            return saldo;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignorado) {
                    // rollback de melhor esforco; o erro original e o que importa
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignorado) {
                    // conexao ja esta sendo descartada
                }
            }
        }
    }

    /* ------------------------------ Auxiliar ------------------------------- */

    /**
     * No LIKE do SQL Server, %, _ e [ sao curingas. Sem escapar, buscar "50%"
     * traria qualquer nome com "50", e "a_b" casaria "aXb". O \ escapa o
     * proprio escape, depois cada curinga.
     */
    static String escaparLike(String termo) {
        return termo.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_")
                    .replace("[", "\\[");
    }

    private Produto mapear(ResultSet rs) throws SQLException {
        Produto p = new Produto();
        p.setId(rs.getInt("id"));
        p.setNome(rs.getString("nome"));
        p.setCategoriaId(rs.getInt("categoria_id"));
        p.setCategoriaNome(rs.getString("categoria"));
        p.setPreco(rs.getBigDecimal("preco"));
        p.setQuantidade(rs.getInt("quantidade"));
        p.setEstoqueMin(rs.getInt("estoque_min"));
        return p;
    }
}
