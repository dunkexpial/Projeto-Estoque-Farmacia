import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.time.LocalDate;

public class Medicamento {
    private final StringProperty nome;
    private final StringProperty batch;
    private final IntegerProperty quantidade;
    private final ObjectProperty<LocalDate> validade; // <-- LocalDate
    private final IntegerProperty alertThreshold;

    public Medicamento(String nome, String batch, int quantidade, LocalDate validade, int alertThreshold) {
        this.nome = new SimpleStringProperty(nome);
        this.batch = new SimpleStringProperty(batch);
        this.quantidade = new SimpleIntegerProperty(quantidade);
        this.validade = new SimpleObjectProperty<>(validade);
        this.alertThreshold = new SimpleIntegerProperty(alertThreshold);
    }

    // --- Nome ---
    public String getNome() { return nome.get(); }
    public void setNome(String value) { nome.set(value); }
    public StringProperty nomeProperty() { return nome; }

    // --- Batch ---
    public String getBatch() { return batch.get(); }
    public void setBatch(String value) { batch.set(value); }
    public StringProperty batchProperty() { return batch; }

    // --- Quantidade ---
    public int getQuantidade() { return quantidade.get(); }
    public void setQuantidade(int value) { quantidade.set(value); }
    public IntegerProperty quantidadeProperty() { return quantidade; }

    // --- Validade ---
    public LocalDate getValidade() { return validade.get(); }
    public void setValidade(LocalDate date) { validade.set(date); }
    public ObjectProperty<LocalDate> validadeProperty() { return validade; }

    // --- Alert Threshold ---
    public int getAlertThreshold() { return alertThreshold.get(); }
    public void setAlertThreshold(int value) { alertThreshold.set(value); }
    public IntegerProperty alertThresholdProperty() { return alertThreshold; }
}
