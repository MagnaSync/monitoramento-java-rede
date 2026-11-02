package org.MagnaSync;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

public class DBConnectionProvider {

    private final DataSource dataSource;

public DBConnectionProvider() {
    String host = System.getenv().getOrDefault("DB_HOST", "localhost");
    String porta = System.getenv().getOrDefault("DB_PORT", "3306");
    String banco = System.getenv().getOrDefault("DB_DATABASE", "magnasync");

    BasicDataSource basicDataSource = new BasicDataSource();
    basicDataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    basicDataSource.setUrl("jdbc:mysql://" + host + ":" + porta + "/" + banco + "?allowPublicKeyRetrieval=true");
    basicDataSource.setUsername(System.getenv().getOrDefault("DB_USER", "aluno2"));
    basicDataSource.setPassword(System.getenv().getOrDefault("DB_PASSWORD", ""));

    this.dataSource = basicDataSource;
}

    public JdbcTemplate getConnection() {
        return new JdbcTemplate(dataSource);
    }

}
