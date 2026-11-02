package org.MagnaSync;

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

//        O Script vai capturar velocidade de download, upload e latência

        SystemInfoProvider si = SystemInfoFactory.create();
        List<NetworkIF> placas = si.getHardware().getNetworkIFs();

//        A máquina precisa estar cadastrada (pelo programa Python) para o Java saber o equipamento
        int equipamentoId = esperarEquipamento(connection, placas);
        System.out.println("Equipamento encontrado: id " + equipamentoId);

//        Cada medida de rede é um componente; o parametro fica sem métrica (NULL)
        int componenteDownload = buscarComponente(connection, "Rede - Download");
        int componenteUpload = buscarComponente(connection, "Rede - Upload");
        int componenteLatencia = buscarComponente(connection, "Rede - Latência");

        int parametroDownload = garantirParametro(connection, equipamentoId, componenteDownload);
        int parametroUpload = garantirParametro(connection, equipamentoId, componenteUpload);
        int parametroLatencia = garantirParametro(connection, equipamentoId, componenteLatencia);

        long recebidoAntes = totalRecebido(placas);
        long enviadoAntes = totalEnviado(placas);

        while (true) {
            Thread.sleep(1000);

            long recebidoAgora = totalRecebido(placas);
            long enviadoAgora = totalEnviado(placas);

            double downloadKB = (recebidoAgora - recebidoAntes) / 1024.0;
            double uploadKB = (enviadoAgora - enviadoAntes) / 1024.0;
            double latenciaMs = obterLatencia("8.8.8.8");

            System.out.printf("Download: %.1f KB/s | Upload: %.1f KB/s | Latência: %.1f ms%n", downloadKB, uploadKB, latenciaMs);

//            inserindo no banco
            inserirLeitura(connection, equipamentoId, componenteDownload, parametroDownload, downloadKB);
            inserirLeitura(connection, equipamentoId, componenteUpload, parametroUpload, uploadKB);
            inserirLeitura(connection, equipamentoId, componenteLatencia, parametroLatencia, latenciaMs);

            recebidoAntes = recebidoAgora;
            enviadoAntes = enviadoAgora;

        }
    }

    private static int esperarEquipamento(JdbcTemplate connection, List<NetworkIF> placas) throws InterruptedException {
        while (true) {
            for (NetworkIF placa : placas) {
                String mac = placa.getMacaddr();
                if (mac == null || mac.isBlank()) {
                    continue;
                }
                List<Integer> ids = connection.queryForList(
                        "SELECT id FROM equipamento WHERE mac_adress = ? LIMIT 1",
                        Integer.class, mac.toUpperCase());
                if (!ids.isEmpty()) {
                    return ids.get(0);
                }
            }
            System.out.println("Máquina ainda não cadastrada no banco. Cadastre pelo programa Python. Nova tentativa em 15s...");
            Thread.sleep(15000);
        }
    }

    private static int buscarComponente(JdbcTemplate connection, String tipo) {
        List<Integer> ids = connection.queryForList(
                "SELECT c.id FROM componente c JOIN tipoComponente t ON t.id = c.fkTipo WHERE t.tipo = ? LIMIT 1",
                Integer.class, tipo);
        if (ids.isEmpty()) {
            throw new IllegalStateException("Componente '" + tipo + "' não existe no banco. Rode o arquivo rede.sql.");
        }
        return ids.get(0);
    }

    private static int garantirParametro(JdbcTemplate connection, int equipamentoId, int componenteId) {
        List<Integer> ids = connection.queryForList(
                "SELECT id FROM parametro WHERE fkEquipamento = ? AND fkComponente = ? LIMIT 1",
                Integer.class, equipamentoId, componenteId);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        Integer novoId = connection.queryForObject("SELECT COALESCE(MAX(id), 0) + 1 FROM parametro", Integer.class);
        connection.update(
                "INSERT INTO parametro (id, fkEquipamento, fkComponente, metrica) VALUES (?, ?, ?, NULL)",
                novoId, equipamentoId, componenteId);
        return novoId;
    }

    private static void inserirLeitura(JdbcTemplate connection, int equipamentoId, int componenteId, int parametroId, double valor) {
        connection.update(
                "INSERT INTO leitura (fkEquipamento, fkComponente, fkMetrica, valor, data_hora) VALUES (?, ?, ?, ?, NOW())",
                equipamentoId, componenteId, parametroId, valor);
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
