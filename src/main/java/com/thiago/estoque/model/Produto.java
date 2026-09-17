package com.thiago.estoque.model;

import java.math.BigDecimal;

/** Representa um produto do estoque. */
public class Produto {

    private int id;
    private String nome;
    private int categoriaId;
    private String categoriaNome;
    private BigDecimal preco;
    private int quantidade;
    private int estoqueMin;

    public Produto() {
    }

    public Produto(String nome, int categoriaId, BigDecimal preco, int quantidade, int estoqueMin) {
        this.nome = nome;
        this.categoriaId = categoriaId;
        this.preco = preco;
        this.quantidade = quantidade;
        this.estoqueMin = estoqueMin;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public int getCategoriaId() { return categoriaId; }
    public void setCategoriaId(int categoriaId) { this.categoriaId = categoriaId; }

    public String getCategoriaNome() { return categoriaNome; }
    public void setCategoriaNome(String categoriaNome) { this.categoriaNome = categoriaNome; }

    public BigDecimal getPreco() { return preco; }
    public void setPreco(BigDecimal preco) { this.preco = preco; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public int getEstoqueMin() { return estoqueMin; }
    public void setEstoqueMin(int estoqueMin) { this.estoqueMin = estoqueMin; }

    /** Valor total imobilizado neste item (preco x quantidade). */
    public BigDecimal getValorTotal() {
        return preco.multiply(BigDecimal.valueOf(quantidade));
    }

    public boolean isEstoqueBaixo() {
        return quantidade <= estoqueMin;
    }
}
