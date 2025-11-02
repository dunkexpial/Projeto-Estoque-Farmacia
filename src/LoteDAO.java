import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoteDAO {
    
    public void inserir(LoteEstoque lote, String nomeProduto) throws SQLException {
        String sql = "INSERT INTO Lote (idFornecedor, nome, num_lote, quantidade, data_entrada, validade, alerta_min) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, lote.getIdFornecedor());
            stmt.setString(2, nomeProduto);
            stmt.setString(3, lote.getNumeroLote());
            stmt.setInt(4, lote.getQuantidadeAtual());
            stmt.setDate(5, Date.valueOf(lote.getDataEntrada()));
            stmt.setDate(6, Date.valueOf(lote.getDataValidade()));
            stmt.setInt(7, lote.getAlertThreshold());
            
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                lote.setIdLote(rs.getInt(1));
            }
        }
    }
    
    public void atualizar(LoteEstoque lote, String nomeProduto) throws SQLException {
        String sql = "UPDATE Lote SET idFornecedor = ?, nome = ?, num_lote = ?, quantidade = ?, " +
                     "data_entrada = ?, validade = ?, alerta_min = ? WHERE idLote = ?";
        
        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, lote.getIdFornecedor());
            stmt.setString(2, nomeProduto);
            stmt.setString(3, lote.getNumeroLote());
            stmt.setInt(4, lote.getQuantidadeAtual());
            stmt.setDate(5, Date.valueOf(lote.getDataEntrada()));
            stmt.setDate(6, Date.valueOf(lote.getDataValidade()));
            stmt.setInt(7, lote.getAlertThreshold());
            stmt.setInt(8, lote.getIdLote());
            
            stmt.executeUpdate();
        }
    }
    
    public void excluir(int idLote) throws SQLException {
        String sql = "DELETE FROM Lote WHERE idLote = ?";
        
        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idLote);
            stmt.executeUpdate();
        }
    }
    
    public List<Object[]> listarTodosComNome() throws SQLException {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT * FROM Lote ORDER BY validade";
        
        try (Connection conn = DataBase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                LoteEstoque lote = new LoteEstoque(
                    rs.getInt("idLote"),
                    rs.getString("num_lote"),
                    rs.getInt("quantidade"),
                    rs.getDate("data_entrada").toLocalDate(),
                    rs.getDate("validade").toLocalDate(),
                    0, // idProduto (não existe na tabela)
                    rs.getInt("idFornecedor"),
                    rs.getInt("alerta_min")
                );
                
                String nomeProduto = rs.getString("nome");
                lista.add(new Object[]{lote, nomeProduto});
            }
        }
        
        return lista;
    }
    
    public List<LoteEstoque> listarPorFornecedor(int idFornecedor) throws SQLException {
        List<LoteEstoque> lista = new ArrayList<>();
        String sql = "SELECT * FROM Lote WHERE idFornecedor = ? ORDER BY validade";
        
        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idFornecedor);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                LoteEstoque lote = new LoteEstoque(
                    rs.getInt("idLote"),
                    rs.getString("num_lote"),
                    rs.getInt("quantidade"),
                    rs.getDate("data_entrada").toLocalDate(),
                    rs.getDate("validade").toLocalDate(),
                    0,
                    rs.getInt("idFornecedor"),
                    rs.getInt("alerta_min")
                );
                
                lista.add(lote);
            }
        }
        
        return lista;
    }
    
    public void atualizarQuantidade(int idLote, int novaQuantidade) throws SQLException {
        String sql = "UPDATE Lote SET quantidade = ? WHERE idLote = ?";
        
        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, novaQuantidade);
            stmt.setInt(2, idLote);
            stmt.executeUpdate();
        }
    }
}