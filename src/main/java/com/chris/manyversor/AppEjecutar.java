package main.java.com.chris.manyversor;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
// import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;

public class AppEjecutar {
    public static void main(String[] args) {
        // configura el tema oscuro antes de crear cualquier componente de la interfaz
        // gráfica
        // FlatDarkLaf.setup();
        FlatArcDarkOrangeIJTheme.setup();

        // lee la API Key una vez al inicio
        final String apiKey = ApiKeyManejo.getApiKey();

        if (apiKey == null) {
            // Si no hay API key, la aplicación no puede funcionara correctamente.
            // informa al usuario y salir, o mostrar GUI con funcionalidad limitada.
            String mensajeError = """
                    CRÍTICO: No se pudo cargar la API Key.
                    Asegúrese de que 'src/main/resources/api_key.txt' exista y contenga una clave válida.
                    La aplicación no puede realizar conversiones sin una API Key.""";
            System.err.println(mensajeError);

            // Si el argumento "gui" está presente o no hay argumentos y se espera GUI,
            // mostrar diálogo
            if ((args.length > 0 && args[0].equalsIgnoreCase("gui")) || isGuiEnvironment()) {
                final String finalMensajeError = mensajeError; // Guarda mensaje de error
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, finalMensajeError,
                        "Error de API Key", JOptionPane.ERROR_MESSAGE));
            }
            // TODO: definir si se debe salir o continuar con funcionalidad limitada cuando
            // API key es nula
            // Por ahora, si es modo consola, ConsolaApp manejará el apiKey nulo.
            // Ssi es modo GUI, ConversorGUI manejará el apiKey nulo.
            // Considera System.exit(1); si es inaceptable continuar.
        }

        // si se pasa "gui" como argumento o si no hay consola (doble clic en jar),
        // iniciar GUI
        if ((args.length > 0 && args[0].equalsIgnoreCase("gui")) || System.console() == null) {
            System.out.println("Iniciando Conversor de Moneda en modo GUI directamente...");
            SwingUtilities.invokeLater(() -> {

                new ConversorGUI().setVisible(true);
            });
        } else {
            System.out.println("Iniciando Conversor de Moneda en modo Consola...");
            ConsolaApp.run(apiKey); // Pasar la API key leída a la app de consola
        }
    }

    //
    // trata detectar entorno que puede mostrar GUI
    private static boolean isGuiEnvironment() {

        return !java.awt.GraphicsEnvironment.isHeadless();
    }
}
