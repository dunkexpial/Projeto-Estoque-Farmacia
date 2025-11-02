import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FornecedorDAO {
    
    public void inserir(Fornecedor fornecedor) throws SQLException {
        String sql = "INSERT INTO Fornecedor (nome, cnpj, telefone, email) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, fornecedor.getNome());
            stmt.setString(2, fornecedor.getCnpj());
            stmt.setString(3, fornecedor.getTelefone());
            stmt.setString(4, fornecedor.getEmail());
            
            stmt.executeUpdate();
            
            // Pega o ID gerado
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                fornecedor.setIdFornecedor(rs.getInt(1));
            }
        }
    }
    
    public void atualizar(Fornecedor fornecedor) throws SQLException {
        String sql = "UPDATE Fornecedor SET nome = ?, cnpj = ?, telefone = ?, email = ? WHERE idFornecedor = ?";
        
        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, fornecedor.getNome());
            stmt.setString(2, fornecedor.getCnpj());
            stmt.setString(3, fornecedor.getTelefone());
            stmt.setString(4, fornecedor.getEmail());
            stmt.setInt(5, fornecedor.getIdFornecedor());
            
            stmt.executeUpdate();
        }
    }
    
    public void excluir(int idFornecedor) throws SQLException {
        String sql = "DELETE FROM Fornecedor WHERE idFornecedor = ?";
        
        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idFornecedor);
            stmt.executeUpdate();
        }
    }
    
    public List<Fornecedor> listarTodos() throws SQLException {
        List<Fornecedor> lista = new ArrayList<>();
        String sql = "SELECT * FROM Fornecedor ORDER BY nome";
        
        try (Connection conn = DataBase.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                Fornecedor f = new Fornecedor(
                    rs.getInt("idFornecedor"),
                    rs.getString("nome"),
                    rs.getString("cnpj"),
                    rs.getString("telefone"),
                    rs.getString("email")
                );
                lista.add(f);
            }
        }
        
        return lista;
    }
    
    public boolean cnpjJaExiste(String cnpj, int idFornecedorAtual) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Fornecedor WHERE cnpj = ? AND idFornecedor != ?";
        
        try (Connection conn = DataBase.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, cnpj);
            stmt.setInt(2, idFornecedorAtual);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        }
        
        return false;
    }
}