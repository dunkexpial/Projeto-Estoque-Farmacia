import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Medicamento {
    private final StringProperty nome;
    private final IntegerProperty quantidade;
    private final StringProperty validade;

    public Medicamento(String nome, int quantidade, String validade) {
        this.nome = new SimpleStringProperty(nome);
        this.quantidade = new SimpleIntegerProperty(quantidade);
        this.validade = new SimpleStringProperty(validade);
    }

    // --- Nome ---
    public String getNome() { return nome.get(); }
    public void setNome(String value) { nome.set(value); }
    public StringProperty nomeProperty() { return nome; }

    // --- Quantidade ---
    public int getQuantidade() { return quantidade.get(); }
    public void setQuantidade(int value) { quantidade.set(value); }
    public IntegerProperty quantidadeProperty() { return quantidade; }

    // --- Validade ---
    public String getValidade() { return validade.get(); }
    public void setValidade(String value) { validade.set(value); }
    public StringProperty validadeProperty() { return validade; }
}
