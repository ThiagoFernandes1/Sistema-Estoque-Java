package com.thiago.estoque.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Centraliza a abertura de conexoes com o SQL Server.
 *
 * As credenciais vem de src/main/resources/db.properties. Se existir um
 * db.local.properties ao lado dele (ignorado pelo git), os valores dele
 * prevalecem; e as variaveis de ambiente (DB_URL, DB_USER, DB_PASSWORD)
 * prevalecem sobre os dois. Assim nenhuma senha real precisa ser versionada.
 */
public final class Conexao {

    private static final String ARQUIVO = "/db.properties";
    private static final String ARQUIVO_LOCAL = "/db.local.properties";

    private static final String url;
    private static final String usuario;
    private static final String senha;

    static {
        Properties props = new Properties();
        carregar(props, ARQUIVO);
        // carregado depois para sobrescrever as chaves do arquivo versionado
        carregar(props, ARQUIVO_LOCAL);

        url     = valor("DB_URL",      props.getProperty("db.url"));
        usuario = valor("DB_USER",     props.getProperty("db.user"));
        senha   = valor("DB_PASSWORD", props.getProperty("db.password"));
    }

    private Conexao() {
    }

    private static void carregar(Properties props, String arquivo) {
        try (InputStream in = Conexao.class.getResourceAsStream(arquivo)) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError("Falha ao ler " + arquivo + ": " + e.getMessage());
        }
    }

    private static String valor(String variavelAmbiente, String padrao) {
        String doAmbiente = System.getenv(variavelAmbiente);
        return (doAmbiente != null && !doAmbiente.isBlank()) ? doAmbiente : padrao;
    }

    /** Abre uma nova conexao. O chamador e responsavel por fecha-la. */
    public static Connection abrir() throws SQLException {
        if (url == null || url.isBlank()) {
            throw new SQLException("URL do banco nao configurada (db.properties ou DB_URL).");
        }
        return DriverManager.getConnection(url, usuario, senha);
    }

    /** Testa a conectividade, usado na inicializacao da aplicacao. */
    public static void testar() throws SQLException {
        try (Connection c = abrir()) {
            if (!c.isValid(5)) {
                throw new SQLException("Conexao aberta, mas invalida.");
            }
        }
    }
}
