package model;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SimpleDotEnv {
    private final Map<String, String> variables = new HashMap<>();

    public SimpleDotEnv(String filePath) {
        load(filePath);
    }

    private void load(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int idx = line.indexOf('=');
                if (idx != -1) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();

                    if ((value.startsWith("\"") && value.endsWith("\"")) ||
                        (value.startsWith("'") && value.endsWith("'"))) {
                        value = value.substring(1, value.length() - 1);
                    }

                    variables.put(key, value);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo .env: " + filePath, e);
        }
    }

    public String get(String key) {
        return variables.get(key);
    }
}
