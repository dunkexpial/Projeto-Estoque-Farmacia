package model;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MovimentacaoDAO {

    /**
     * Registra uma nova movimentação de estoque
     */
    public void inserir(MovimentacaoEstoque movimentacao) throws SQLException {
        String sql = "INSERT INTO MovimentacaoEstoque (quantidade, data_hora, tipo, observacao, idLote) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, movimentacao.getQuantidade());
            stmt.setTimestamp(2, Timestamp.valueOf(movimentacao.getDataHora()));
            stmt.setString(3, movimentacao.getTipo());
            stmt.setString(4, movimentacao.getObservacao());
            stmt.setInt(5, movimentacao.getIdLote());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                movimentacao.setIdMovimentacao(rs.getInt(1));
            }
        }
    }

    /**
     * Realiza o estorno de uma movimentação (cria movimentação inversa)
     */
    public void estornar(int idMovimentacao, String motivoEstorno) throws SQLException {
        Connection conn = null;
        try {
            conn = DataBase.getConnection();
            conn.setAutoCommit(false);

            // Busca a movimentação original
            String sqlBusca = "SELECT * FROM MovimentacaoEstoque WHERE idMovimentacao = ?";
            MovimentacaoEstoque movOriginal = null;

            try (PreparedStatement stmt = conn.prepareStatement(sqlBusca)) {
                stmt.setInt(1, idMovimentacao);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    movOriginal = new MovimentacaoEstoque(
                            rs.getInt("idMovimentacao"),
                            rs.getInt("quantidade"),
                            rs.getTimestamp("data_hora").toLocalDateTime(),
                            rs.getString("tipo"),
                            rs.getString("observacao"),
                            rs.getInt("idLote")
                    );
                }
            }

            if (movOriginal == null) {
                throw new SQLException("Movimentação não encontrada");
            }

            // Verifica se já foi estornada
            String sqlVerifica = "SELECT COUNT(*) FROM MovimentacaoEstoque WHERE observacao LIKE ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlVerifica)) {
                stmt.setString(1, "%ESTORNO da movimentação #" + idMovimentacao + "%");
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new SQLException("Esta movimentação já foi estornada");
                }
            }

            // Cria movimentação inversa
            String tipoInverso = movOriginal.getTipo().equals("ENTRADA") ? "SAIDA" : "ENTRADA";
            String observacaoEstorno = "ESTORNO da movimentação #" + idMovimentacao +
                    " - Motivo: " + motivoEstorno;

            MovimentacaoEstoque estorno = new MovimentacaoEstoque(
                    0,
                    movOriginal.getQuantidade(),
                    LocalDateTime.now(),
                    tipoInverso,
                    observacaoEstorno,
                    movOriginal.getIdLote()
            );

            String sqlInsert = "INSERT INTO MovimentacaoEstoque (quantidade, data_hora, tipo, observacao, idLote) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                stmt.setInt(1, estorno.getQuantidade());
                stmt.setTimestamp(2, Timestamp.valueOf(estorno.getDataHora()));
                stmt.setString(3, estorno.getTipo());
                stmt.setString(4, estorno.getObservacao());
                stmt.setInt(5, estorno.getIdLote());
                stmt.executeUpdate();
            }

            // Atualiza quantidade do lote
            LoteDAO loteDAO = new LoteDAO();
            String sqlLote = "SELECT quantidade FROM Lote WHERE idLote = ?";
            int quantidadeAtual = 0;

            try (PreparedStatement stmt = conn.prepareStatement(sqlLote)) {
                stmt.setInt(1, movOriginal.getIdLote());
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    quantidadeAtual = rs.getInt("quantidade");
                }
            }

            int novaQuantidade = tipoInverso.equals("ENTRADA")
                    ? quantidadeAtual + estorno.getQuantidade()
                    : quantidadeAtual - estorno.getQuantidade();

            if (novaQuantidade < 0) {
                throw new SQLException("Estorno resultaria em quantidade negativa no estoque");
            }

            String sqlUpdateLote = "UPDATE Lote SET quantidade = ? WHERE idLote = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sqlUpdateLote)) {
                stmt.setInt(1, novaQuantidade);
                stmt.setInt(2, movOriginal.getIdLote());
                stmt.executeUpdate();
            }

            conn.commit();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Lista todas as movimentações
     */
    public List<MovimentacaoEstoque> listarTodas() throws SQLException {
        List<MovimentacaoEstoque> lista = new ArrayList<>();
        String sql = "SELECT * FROM MovimentacaoEstoque ORDER BY data_hora DESC";

        try (Connection conn = DataBase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                MovimentacaoEstoque mov = new MovimentacaoEstoque(
                        rs.getInt("idMovimentacao"),
                        rs.getInt("quantidade"),
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        rs.getString("tipo"),
                        rs.getString("observacao"),
                        rs.getInt("idLote")
                );
                lista.add(mov);
            }
        }

        return lista;
    }

    /**
     * Lista movimentações por lote
     */
    public List<MovimentacaoEstoque> listarPorLote(int idLote) throws SQLException {
        List<MovimentacaoEstoque> lista = new ArrayList<>();
        String sql = "SELECT * FROM MovimentacaoEstoque WHERE idLote = ? ORDER BY data_hora DESC";

        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idLote);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                MovimentacaoEstoque mov = new MovimentacaoEstoque(
                        rs.getInt("idMovimentacao"),
                        rs.getInt("quantidade"),
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        rs.getString("tipo"),
                        rs.getString("observacao"),
                        rs.getInt("idLote")
                );
                lista.add(mov);
            }
        }

        return lista;
    }

    /**
     * Lista movimentações por período
     */
    public List<MovimentacaoEstoque> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) throws SQLException {
        List<MovimentacaoEstoque> lista = new ArrayList<>();
        String sql = "SELECT * FROM MovimentacaoEstoque WHERE data_hora BETWEEN ? AND ? ORDER BY data_hora DESC";

        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(inicio));
            stmt.setTimestamp(2, Timestamp.valueOf(fim));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                MovimentacaoEstoque mov = new MovimentacaoEstoque(
                        rs.getInt("idMovimentacao"),
                        rs.getInt("quantidade"),
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        rs.getString("tipo"),
                        rs.getString("observacao"),
                        rs.getInt("idLote")
                );
                lista.add(mov);
            }
        }

        return lista;
    }

    /**
     * Relatório de movimentações por produto (todos os lotes)
     */
    public List<Object[]> relatorioMovimentacoesPorProduto(String nomeProduto) throws SQLException {
        List<Object[]> resultado = new ArrayList<>();
        String sql = "SELECT m.*, l.nome, l.num_lote " +
                "FROM MovimentacaoEstoque m " +
                "INNER JOIN Lote l ON m.idLote = l.idLote " +
                "WHERE l.nome LIKE ? " +
                "ORDER BY m.data_hora DESC";

        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nomeProduto + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                MovimentacaoEstoque mov = new MovimentacaoEstoque(
                        rs.getInt("idMovimentacao"),
                        rs.getInt("quantidade"),
                        rs.getTimestamp("data_hora").toLocalDateTime(),
                        rs.getString("tipo"),
                        rs.getString("observacao"),
                        rs.getInt("idLote")
                );

                String produto = rs.getString("nome");
                String lote = rs.getString("num_lote");

                resultado.add(new Object[]{mov, produto, lote});
            }
        }

        return resultado;
    }

    /**
     * Calcula saldo total de um produto (soma de todos os lotes)
     */
    public int calcularSaldoProduto(String nomeProduto) throws SQLException {
        String sql = "SELECT SUM(quantidade) as total FROM Lote WHERE nome = ?";

        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nomeProduto);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("total");
            }
        }

        return 0;
    }

    /**
     * Verifica se um lote está vencido
     */
    public boolean loteEstaVencido(int idLote) throws SQLException {
        String sql = "SELECT validade FROM Lote WHERE idLote = ?";

        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idLote);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                java.sql.Date validade = rs.getDate("validade");
                return validade.toLocalDate().isBefore(java.time.LocalDate.now());
            }
        }

        return false;
    }
}