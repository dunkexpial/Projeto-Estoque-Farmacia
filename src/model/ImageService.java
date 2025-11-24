package model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class ImageService {
    private static final String APP_NAME = "FarmaciaEstoque";
    private static final String IMAGES_FOLDER = "lote_images";
    private static Path imagesDirectory;

    static {
        try {
            // Obtém o diretório AppData ou equivalente do sistema
            String appData = System.getenv("APPDATA"); // Windows
            if (appData == null) {
                appData = System.getProperty("user.home"); // Linux/Mac
            }

            // Cria o caminho completo: AppData/FarmaciaEstoque/lote_images
            imagesDirectory = Paths.get(appData, APP_NAME, IMAGES_FOLDER);

            // Cria os diretórios se não existirem
            Files.createDirectories(imagesDirectory);

            System.out.println("Diretório de imagens: " + imagesDirectory.toString());
        } catch (IOException e) {
            System.err.println("Erro ao criar diretório de imagens: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Salva uma imagem no diretório local e retorna o nome do arquivo
     * @param sourceFile Arquivo de origem selecionado pelo usuário
     * @return Nome do arquivo salvo (UUID + extensão)
     * @throws IOException Se houver erro ao copiar o arquivo
     */
    public static String salvarImagem(File sourceFile) throws IOException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IOException("Arquivo de imagem inválido");
        }

        // Obtém a extensão do arquivo
        String originalName = sourceFile.getName();
        String extension = "";
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalName.substring(lastDot);
        }

        // Gera um nome único para o arquivo
        String uniqueFileName = UUID.randomUUID().toString() + extension;
        Path destinationPath = imagesDirectory.resolve(uniqueFileName);

        // Copia o arquivo para o diretório local
        Files.copy(sourceFile.toPath(), destinationPath, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    /**
     * Obtém o caminho completo de uma imagem pelo nome do arquivo
     * @param fileName Nome do arquivo da imagem
     * @return Caminho completo do arquivo
     */
    public static String getImagePath(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return getDefaultImagePath();
        }

        Path imagePath = imagesDirectory.resolve(fileName);
        if (Files.exists(imagePath)) {
            return imagePath.toUri().toString();
        }

        return getDefaultImagePath();
    }

    /**
     * Retorna o caminho da imagem padrão
     */
    public static String getDefaultImagePath() {
        return "file:med.png";
    }

    /**
     * Exclui uma imagem do diretório local
     * @param fileName Nome do arquivo a ser excluído
     * @return true se o arquivo foi excluído, false caso contrário
     */
    public static boolean excluirImagem(String fileName) {
        if (fileName == null || fileName.isEmpty() || fileName.equals("med.png")) {
            return false;
        }

        try {
            Path imagePath = imagesDirectory.resolve(fileName);
            if (Files.exists(imagePath)) {
                Files.delete(imagePath);
                return true;
            }
        } catch (IOException e) {
            System.err.println("Erro ao excluir imagem: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Verifica se uma imagem existe
     * @param fileName Nome do arquivo
     * @return true se o arquivo existe
     */
    public static boolean imagemExiste(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        Path imagePath = imagesDirectory.resolve(fileName);
        return Files.exists(imagePath);
    }

    /**
     * Obtém o diretório de imagens
     * @return Path do diretório de imagens
     */
    public static Path getImagesDirectory() {
        return imagesDirectory;
    }
}