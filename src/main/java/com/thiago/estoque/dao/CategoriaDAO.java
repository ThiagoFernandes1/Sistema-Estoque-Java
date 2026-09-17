package com.thiago.estoque.dao;

import com.thiago.estoque.db.Conexao;
import com.thiago.estoque.model.Categoria;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Acesso a tabela dbo.Categoria. */
public class CategoriaDAO {

    public List<Categoria> listar() throws SQLException {
        String sql = "SELECT id, nome FROM dbo.Categoria ORDER BY nome";

        List<Categoria> categorias = new ArrayList<>();
        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categorias.add(new Categoria(rs.getInt("id"), rs.getString("nome")));
            }
        }
        return categorias;
    }

    public boolean existe(int id) throws SQLException {
        String sql = "SELECT 1 FROM dbo.Categoria WHERE id = ?";

        try (Connection conn = Conexao.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
