package model;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RelatorioService {

    public static void gerarRelatorioCSV(List<LogEntry> logs, File arquivo) throws Exception {
        try (PrintWriter writer = new PrintWriter(arquivo, StandardCharsets.UTF_8)) {
            writer.write('\ufeff');

            writer.println("ID;Data/Hora;Operacao;Entidade;Usuario;Descricao");

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            for (LogEntry log : logs) {
                String descricaoLimpa = log.getDescricao()
                        .replace(";", ",")
                        .replace("\n", " ")
                        .replace("\r", " ");

                writer.println(String.format("%d;%s;%s;%s;%s;%s",
                        log.getIdLog(),
                        log.getDataHora().format(fmt),
                        log.getTipoOperacao(),
                        log.getEntidade(),
                        log.getUsuarioResponsavel(),
                        descricaoLimpa
                ));
            }
        }
    }
}