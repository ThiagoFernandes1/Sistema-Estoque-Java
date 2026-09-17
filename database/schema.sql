/* =====================================================================
   Sistema de Estoque - criacao do banco (Microsoft SQL Server)
   Execute este script no SSMS ou via sqlcmd antes de rodar a aplicacao.
   ===================================================================== */

IF DB_ID('EstoqueDB') IS NULL
    CREATE DATABASE EstoqueDB;
GO

USE EstoqueDB;
GO

/* ------------------------------- Tabelas ---------------------------- */

IF OBJECT_ID('dbo.Movimentacao', 'U') IS NOT NULL DROP TABLE dbo.Movimentacao;
IF OBJECT_ID('dbo.Produto', 'U')      IS NOT NULL DROP TABLE dbo.Produto;
IF OBJECT_ID('dbo.Categoria', 'U')    IS NOT NULL DROP TABLE dbo.Categoria;
GO

CREATE TABLE dbo.Categoria (
    id      INT IDENTITY(1,1) PRIMARY KEY,
    nome    NVARCHAR(60) NOT NULL UNIQUE
);
GO

CREATE TABLE dbo.Produto (
    id           INT IDENTITY(1,1) PRIMARY KEY,
    nome         NVARCHAR(120)  NOT NULL,
    categoria_id INT            NOT NULL,
    preco        DECIMAL(10,2)  NOT NULL CONSTRAINT CK_Produto_Preco    CHECK (preco >= 0),
    quantidade   INT            NOT NULL CONSTRAINT CK_Produto_Qtd      CHECK (quantidade >= 0),
    estoque_min  INT            NOT NULL CONSTRAINT DF_Produto_Min      DEFAULT (5),
    criado_em    DATETIME2      NOT NULL CONSTRAINT DF_Produto_Criado   DEFAULT (SYSDATETIME()),
    CONSTRAINT FK_Produto_Categoria FOREIGN KEY (categoria_id)
        REFERENCES dbo.Categoria(id)
);
GO

CREATE TABLE dbo.Movimentacao (
    id          INT IDENTITY(1,1) PRIMARY KEY,
    produto_id  INT           NOT NULL,
    tipo        CHAR(1)       NOT NULL CONSTRAINT CK_Mov_Tipo CHECK (tipo IN ('E','S')), -- E=entrada, S=saida
    quantidade  INT           NOT NULL CONSTRAINT CK_Mov_Qtd  CHECK (quantidade > 0),
    data_hora   DATETIME2     NOT NULL CONSTRAINT DF_Mov_Data DEFAULT (SYSDATETIME()),
    CONSTRAINT FK_Mov_Produto FOREIGN KEY (produto_id)
        REFERENCES dbo.Produto(id) ON DELETE CASCADE
);
GO

CREATE INDEX IX_Produto_Nome      ON dbo.Produto(nome);
CREATE INDEX IX_Mov_Produto_Data  ON dbo.Movimentacao(produto_id, data_hora DESC);
GO

/* ------------------------------ View ------------------------------- */

CREATE OR ALTER VIEW dbo.vw_EstoqueBaixo AS
SELECT  p.id,
        p.nome,
        c.nome AS categoria,
        p.quantidade,
        p.estoque_min
FROM    dbo.Produto  p
JOIN    dbo.Categoria c ON c.id = p.categoria_id
WHERE   p.quantidade <= p.estoque_min;
GO

/* --------------------------- Dados iniciais ------------------------ */

INSERT INTO dbo.Categoria (nome) VALUES
    (N'Periféricos'), (N'Notebooks'), (N'Monitores'), (N'Acessórios');
GO

INSERT INTO dbo.Produto (nome, categoria_id, preco, quantidade, estoque_min) VALUES
    (N'Teclado Mecânico RGB',      1,  289.90, 24,  5),
    (N'Mouse Gamer 16000 DPI',     1,  199.00, 12,  5),
    (N'Notebook Ryzen 7 16GB',     2, 4599.00,  3,  2),
    (N'Monitor 27" 144Hz',         3, 1399.90,  7,  3),
    (N'Headset Surround 7.1',      4,  349.00,  2,  5),
    (N'Webcam Full HD',            4,  179.90, 18,  4);
GO

PRINT 'Banco EstoqueDB criado com sucesso.';
GO
