package org.MagnaSync;

import org.MagnaSync.Entities.Leitura;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import oshi.hardware.NetworkIF;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;

import java.util.List;

public class Main {

    static void main() throws InterruptedException {

        DBConnectionProvider dbConnectionProvider = new DBConnectionProvider();
        JdbcTemplate connection = dbConnectionProvider.getConnection();

        connection.execute("""
                CREATE TABLE leitura (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    fkEquipamento INT NOT NULL default 1,
                    fkComponente INT NOT NULL default 1,
                    fkMetrica INT NOT NULL default 1,
                    valor DOUBLE NOT NULL,
                    data_hora DATETIME NOT NULL default now()
                );
                """);


//        O Script vai capturar apenas a velocidade de download, blz:?

        SystemInfoProvider si = SystemInfoFactory.create();
        List<NetworkIF> placas = si.getHardware().getNetworkIFs();
        List<Leitura> leiturasDoBanco = connection.query("select * from leitura;", new BeanPropertyRowMapper<>(Leitura.class));

        long recebidoAntes = totalRecebido(placas);
//        long enviadoAntes = totalEnviado(placas);

        while (true) {
            Thread.sleep(1000);
            leiturasDoBanco = connection.query("select * from leitura;", new BeanPropertyRowMapper<>(Leitura.class));

            if(!leiturasDoBanco.isEmpty()){
                System.out.println(leiturasDoBanco.getLast());
            }


            long recebidoAgora = totalRecebido(placas);
//            long enviadoAgora = totalEnviado(placas);

            double downloadKB = (recebidoAgora - recebidoAntes) / 1024.0;
//            double uploadKB = (enviadoAgora - enviadoAntes) / 1024.0;

//            System.out.printf("Download: %.1f KB/s | Upload: %.1f KB/s%n", downloadKB/*, uploadKB*/);

//           inserindo no banco
            connection.update("insert into leitura (valor) values (?);", downloadKB);

            recebidoAntes = recebidoAgora;
//            enviadoAntes = enviadoAgora;

        }
    }

    private static long totalRecebido(List<NetworkIF> placas) {
        long total = 0;
        for (NetworkIF placa : placas) {
            placa.updateAttributes();
            total += placa.getBytesRecv();
        }
        return total;
    }

//    private static long totalEnviado(List<NetworkIF> placas) {
//        long total = 0;
//        for (NetworkIF placa : placas) {
//            placa.updateAttributes();
//            total += placa.getBytesSent();
//        }
//        return total;
//    }

}
