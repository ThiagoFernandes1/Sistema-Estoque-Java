package com.thiago.estoque;

import com.thiago.estoque.db.Conexao;
import com.thiago.estoque.ui.MenuConsole;

import java.sql.SQLException;

/** Ponto de entrada da aplicacao. */
public class Main {

    public static void main(String[] args) {
        System.out.println("Conectando ao SQL Server...");

        try {
            Conexao.testar();
        } catch (SQLException e) {
            System.err.println("""

                    Nao foi possivel conectar ao banco.

                    Verifique se:
                      - o SQL Server esta em execucao e aceitando TCP/IP na porta 1433;
                      - o script database/schema.sql ja foi executado;
                      - as credenciais em src/main/resources/db.properties estao corretas
                        (ou as variaveis DB_URL, DB_USER e DB_PASSWORD).
                    """);
            System.err.println("Detalhe: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("Conectado.");
        new MenuConsole().iniciar();
    }
}
