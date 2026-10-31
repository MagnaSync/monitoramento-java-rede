package org.MagnaSync;

import org.MagnaSync.Entities.Leitura;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import oshi.hardware.NetworkIF;
import oshi.spi.SystemInfoFactory;
import oshi.spi.SystemInfoProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) throws InterruptedException {

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




//        O Script vai capturar velocidade de download, upload e latência

        SystemInfoProvider si = SystemInfoFactory.create();
        List<NetworkIF> placas = si.getHardware().getNetworkIFs();
        List<Leitura> leiturasDoBanco = connection.query("select * from leitura;", new BeanPropertyRowMapper<>(Leitura.class));

        long recebidoAntes = totalRecebido(placas);
        long enviadoAntes = totalEnviado(placas);

        while (true) {
            Thread.sleep(1000);
            leiturasDoBanco = connection.query("select * from leitura;", new BeanPropertyRowMapper<>(Leitura.class));

//            if (leiturasDoBanco.size() >= 3) {
//                System.out.println(leiturasDoBanco.reversed().get(2));
//                System.out.println(leiturasDoBanco.reversed().get(1));
//                System.out.println(leiturasDoBanco.getLast());
//            } else if (!leiturasDoBanco.isEmpty()) {
//                System.out.println(leiturasDoBanco.getLast());
//            }


            long recebidoAgora = totalRecebido(placas);
            long enviadoAgora = totalEnviado(placas);

            double downloadKB = (recebidoAgora - recebidoAntes) / 1024.0;
            double uploadKB = (enviadoAgora - enviadoAntes) / 1024.0;
            double latenciaMs = obterLatencia("8.8.8.8");

            System.out.printf("Download: %.1f KB/s | Upload: %.1f KB/s | Latência: %.1f ms%n", downloadKB, uploadKB, latenciaMs);

//            inserindo no banco
            connection.update("insert into leitura (valor) values (?);", downloadKB);
            connection.update("insert into leitura (valor) values (?);", uploadKB);
            connection.update("insert into leitura (valor) values (?);", latenciaMs);

            recebidoAntes = recebidoAgora;
            enviadoAntes = enviadoAgora;

        }
    }

    private static double obterLatencia(String host) {
        long inicio = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            int porta = (host.equals("8.8.8.8") || host.equals("1.1.1.1")) ? 53 : 80;
            socket.connect(new InetSocketAddress(host, porta), 1000);
            return (double) (System.currentTimeMillis() - inicio);
        } catch (IOException e) {
            return obterLatenciaPing(host);
        }
    }

    private static double obterLatenciaPing(String host) {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            ProcessBuilder pb = isWindows
                    ? new ProcessBuilder("ping", "-n", "1", "-w", "1000", host)
                    : new ProcessBuilder("ping", "-c", "1", "-W", "1", host);

            Process processo = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(processo.getInputStream()))) {
                String linha;
                Pattern pattern = Pattern.compile("(?:tempo|time)[=<]([0-9]+(?:\\.[0-9]+)?)\\s*ms", Pattern.CASE_INSENSITIVE);
                while ((linha = reader.readLine()) != null) {
                    Matcher matcher = pattern.matcher(linha);
                    if (matcher.find()) {
                        return Double.parseDouble(matcher.group(1));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return -1.0;
    }

    private static long totalRecebido(List<NetworkIF> placas) {
        long total = 0;
        for (NetworkIF placa : placas) {
            placa.updateAttributes();
            total += placa.getBytesRecv();
        }
        return total;
    }

    private static long totalEnviado(List<NetworkIF> placas) {
        long total = 0;
        for (NetworkIF placa : placas) {
            placa.updateAttributes();
            total += placa.getBytesSent();
        }
        return total;
    }

}
