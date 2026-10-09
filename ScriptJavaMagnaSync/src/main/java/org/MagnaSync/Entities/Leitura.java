package org.MagnaSync.Entities;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

public class Leitura {
    private static int contadorGeral = 0;

    private int id;
    private Double valor;
    private LocalDateTime data_hora;
    private int fkEquipamento;
    private int fkComponente;
    private int fkMetrica;

    public Leitura(Double valor) {
        this.id = contadorGeral++;
        this.valor = valor;
        this.data_hora = LocalDateTime.now();
        this.fkEquipamento = ThreadLocalRandom.current().nextInt(1, 3);
        this.fkComponente = ThreadLocalRandom.current().nextInt(1, 3);
        this.fkMetrica = ThreadLocalRandom.current().nextInt(1, 3);
    }

    public Leitura(){

    }

    public int getId() {
        return id;
    }

    public Double getValor() {
        return valor;
    }

    public LocalDateTime getData_hora() {
        return data_hora;
    }

    public int getFkEquipamento() {
        return fkEquipamento;
    }

    public int getFkComponente() {
        return fkComponente;
    }

    public int getFkMetrica() {
        return fkMetrica;
    }

    public static void setContadorGeral(int contadorGeral) {
        Leitura.contadorGeral = contadorGeral;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public void setData_hora(LocalDateTime data_hora) {
        this.data_hora = data_hora;
    }

    public void setFkEquipamento(int fkEquipamento) {
        this.fkEquipamento = fkEquipamento;
    }

    public void setFkComponente(int fkComponente) {
        this.fkComponente = fkComponente;
    }

    public void setFkMetrica(int fkMetrica) {
        this.fkMetrica = fkMetrica;
    }

    @Override
    public String toString() {
        return "Leitura{" +
                "id=" + id +
                ", valor=" + valor +
                ", data_hora=" + data_hora +
                ", fkEquipamento=" + fkEquipamento +
                ", fkComponente=" + fkComponente +
                ", fkMetrica=" + fkMetrica +
                '}';
    }
}
