package model;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LogDAO {
    private Connection getConnection() throws SQLException {
        return DataBase.getConnection();
    }

    public void inserir(LogEntry log) throws SQLException {
        String sql = "INSERT INTO log_alteracoes (data_hora, tipo_operacao, entidade, descricao, usuario_responsavel) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setTimestamp(1, Timestamp.valueOf(log.getDataHora()));
            stmt.setString(2, log.getTipoOperacao());
            stmt.setString(3, log.getEntidade());
            stmt.setString(4, log.getDescricao());
            stmt.setString(5, log.getUsuarioResponsavel());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                log.setIdLog(rs.getInt(1));
            }
        }
    }

    /**
     * Lista todos os logs ordenados por data (mais recentes primeiro)
     */
    public List<LogEntry> listarTodos() throws SQLException {
        String sql = "SELECT * FROM log_alteracoes ORDER BY data_hora DESC";
        List<LogEntry> logs = new ArrayList<>();

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                LogEntry log = new LogEntry(
                        rs.getInt("id_log"),
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        rs.getString("tipo_operacao"),
                        rs.getString("entidade"),
                        rs.getString("descricao"),
                        rs.getString("usuario_responsavel")
                );
                logs.add(log);
            }
        }

        return logs;
    }

    /**
     * NOVO MÉTODO: Lista logs filtrados por Mês e Ano específicos
     */
    public List<LogEntry> listarPorMes(int mes, int ano) throws SQLException {
        // SQL compatível com MySQL para extrair mês e ano
        String sql = "SELECT * FROM log_alteracoes WHERE MONTH(data_hora) = ? AND YEAR(data_hora) = ? ORDER BY data_hora DESC";
        List<LogEntry> logs = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, mes);
            stmt.setInt(2, ano);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LogEntry log = new LogEntry(
                        rs.getInt("id_log"),
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        rs.getString("tipo_operacao"),
                        rs.getString("entidade"),
                        rs.getString("descricao"),
                        rs.getString("usuario_responsavel")
                );
                logs.add(log);
            }
        }

        return logs;
    }

    /**
     * Lista logs filtrados por tipo de operação e/ou entidade
     */
    public List<LogEntry> listarPorFiltro(String tipoOperacao, String entidade) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM log_alteracoes WHERE 1=1");

        if (tipoOperacao != null && !tipoOperacao.equals("Todas")) {
            sql.append(" AND tipo_operacao = ?");
        }
        if (entidade != null && !entidade.equals("Todas")) {
            sql.append(" AND entidade = ?");
        }

        sql.append(" ORDER BY data_hora DESC");

        List<LogEntry> logs = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (tipoOperacao != null && !tipoOperacao.equals("Todas")) {
                stmt.setString(paramIndex++, tipoOperacao);
            }
            if (entidade != null && !entidade.equals("Todas")) {
                stmt.setString(paramIndex++, entidade);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LogEntry log = new LogEntry(
                        rs.getInt("id_log"),
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        rs.getString("tipo_operacao"),
                        rs.getString("entidade"),
                        rs.getString("descricao"),
                        rs.getString("usuario_responsavel")
                );
                logs.add(log);
            }
        }

        return logs;
    }

    // Métodos auxiliares mantidos
    public List<LogEntry> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        String sql = "SELECT * FROM log_alteracoes WHERE data_hora BETWEEN ? AND ? ORDER BY data_hora DESC";
        List<LogEntry> logs = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(inicio));
            stmt.setTimestamp(2, Timestamp.valueOf(fim));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LogEntry log = new LogEntry(
                        rs.getInt("id_log"),
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        rs.getString("tipo_operacao"),
                        rs.getString("entidade"),
                        rs.getString("descricao"),
                        rs.getString("usuario_responsavel")
                );
                logs.add(log);
            }
        }
        return logs;
    }

    public List<LogEntry> buscarPorDescricao(String palavraChave) throws SQLException {
        String sql = "SELECT * FROM log_alteracoes WHERE descricao LIKE ? ORDER BY data_hora DESC";
        List<LogEntry> logs = new ArrayList<>();

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + palavraChave + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LogEntry log = new LogEntry(
                        rs.getInt("id_log"),
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        rs.getString("tipo_operacao"),
                        rs.getString("entidade"),
                        rs.getString("descricao"),
                        rs.getString("usuario_responsavel")
                );
                logs.add(log);
            }
        }
        return logs;
    }

    public int contarTotal() throws SQLException {
        String sql = "SELECT COUNT(*) as total FROM log_alteracoes";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("total");
        }
        return 0;
    }

    public int limparLogsAntigos(int diasAntigos) throws SQLException {
        String sql = "DELETE FROM log_alteracoes WHERE data_hora < DATE_SUB(NOW(), INTERVAL ? DAY)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, diasAntigos);
            return stmt.executeUpdate();
        }
    }

    public String obterEstatisticas() throws SQLException {
        String sql = "SELECT tipo_operacao, COUNT(*) as total FROM log_alteracoes GROUP BY tipo_operacao";
        StringBuilder estatisticas = new StringBuilder();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                estatisticas.append(rs.getString("tipo_operacao"))
                        .append(": ")
                        .append(rs.getInt("total"))
                        .append("\n");
            }
        }
        return estatisticas.toString();
    }
}