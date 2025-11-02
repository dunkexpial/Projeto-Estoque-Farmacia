package model;

import javafx.beans.property.*;

public class Fornecedor {
    private final IntegerProperty idFornecedor;
    private final StringProperty nome;
    private final StringProperty cnpj;
    private final StringProperty telefone;
    private final StringProperty email;

    public Fornecedor(int idFornecedor, String nome, String cnpj, String telefone, String email) {
        this.idFornecedor = new SimpleIntegerProperty(idFornecedor);
        this.nome = new SimpleStringProperty(nome);
        this.cnpj = new SimpleStringProperty(cnpj);
        this.telefone = new SimpleStringProperty(telefone);
        this.email = new SimpleStringProperty(email);
    }

    // idFornecedor
    public int getIdFornecedor() { return idFornecedor.get(); }
    public void setIdFornecedor(int value) { idFornecedor.set(value); }
    public IntegerProperty idFornecedorProperty() { return idFornecedor; }

    // nome
    public String getNome() { return nome.get(); }
    public void setNome(String value) { nome.set(value); }
    public StringProperty nomeProperty() { return nome; }

    // cnpj
    public String getCnpj() { return cnpj.get(); }
    public void setCnpj(String value) { cnpj.set(value); }
    public StringProperty cnpjProperty() { return cnpj; }

    // telefone
    public String getTelefone() { return telefone.get(); }
    public void setTelefone(String value) { telefone.set(value); }
    public StringProperty telefoneProperty() { return telefone; }

    // email
    public String getEmail() { return email.get(); }
    public void setEmail(String value) { email.set(value); }
    public StringProperty emailProperty() { return email; }

    @Override
    public String toString() {
        return nome.get();
    }
}