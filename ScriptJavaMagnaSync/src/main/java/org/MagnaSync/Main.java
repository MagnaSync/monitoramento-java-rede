package org.MagnaSync;

import org.MagnaSync.Entities.Leitura;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class Main {

    static void main() {

        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        connection.execute("""
                CREATE TABLE leitura (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    fkEquipamento INT NOT NULL,
                    fkComponente INT NOT NULL,
                    fkMetrica INT NOT NULL,
                    valor DOUBLE NOT NULL,
                    data_hora DATETIME NOT NULL default now()
                );
                """);





        List<Leitura> todasAsLeituras = connection.query("select * from leitura;", new BeanPropertyRowMapper<>(Leitura.class));

        for (Leitura leitura : todasAsLeituras) {
            System.out.println(leitura.toString());
        }




    }

}
