package main.java.com.chris.manyversor;

import javax.swing.*;
import java.io.*;

public class ApiKeyManejo {

    public static String getApiKey() {
        // Primero intento cargar desde recursos (funciona en el JAR)
        try (InputStream inputStream = ApiKeyManejo.class.getClassLoader().getResourceAsStream("api_key.txt")) {
            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    return reader.readLine().trim();
                }
            }
        } catch (Exception e) {
            System.err.println("Error leyendo desde recurso embebido: " + e.getMessage());
        }

        // Si falla, intento como archivo externo (funciona en VSCode)
        try {
            File file = new File("src/main/resources/api_key.txt");
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line = reader.readLine();
                    if (line != null && !line.trim().isEmpty()) {
                        return line.trim();
                    } else {
                        System.err.println("Archivo api_key.txt está vacío.");
                    }
                }
            } else {
                System.err.println("Archivo api_key.txt no encontrado en src/main/resources");
            }

            // Si el archivo no existe o está vacío, pedir al usuario
            String nuevaApiKey = JOptionPane.showInputDialog(null, "Ingrese su API Key:", "API Key requerida",
                    JOptionPane.QUESTION_MESSAGE);
            if (nuevaApiKey != null && !nuevaApiKey.trim().isEmpty()) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(nuevaApiKey.trim());
                    return nuevaApiKey.trim();
                } catch (IOException e) {
                    System.err.println("No se pudo guardar la API Key: " + e.getMessage());
                }
            } else {
                System.err.println("No se ingresó una API Key válida.");
            }

        } catch (Exception e) {
            System.err.println("Error leyendo archivo externo: " + e.getMessage());
        }

        return null;
    }
}
