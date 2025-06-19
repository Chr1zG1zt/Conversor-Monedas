package main.java.com.chris.manyversor;

import com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import javax.print.DocFlavor.URL;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

public class ConversorGUI extends JFrame {
    private JComboBox<String> comboMonedaOrigen;
    private JComboBox<String> comboMonedaDestino;
    private JTextField txtCantidad;
    private JLabel lblResultado;
    private JButton btnConvertir;
    private JTextArea consoleTextArea;

    private ExchangeServicio exchangeService;
    private final List<String> MONEDAS_COMUNES = Arrays.asList(
            "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY",
            "ARS", "BRL", "COP", "MXN", "CLP", "PEN", "UYU");
    private final String apiKey;

    public ConversorGUI() {
        // Recupera la clave API de la clase ApiKeyManejo
        this.apiKey = ApiKeyManejo.getApiKey();

        if (this.apiKey == null) {
            // Mostrar error 'crítico' si la clave API no se carga, porque no es posible
            // realizar la conversión!
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                    "CRÍTICO: No se pudo cargar la API Key.\n" +
                            "Asegúrese de que 'api_key.txt' exista en 'src/main/resources/' y contenga una clave válida.\n"
                            +
                            "La funcionalidad de conversión estará deshabilitada.",
                    "Error de API Key", JOptionPane.ERROR_MESSAGE));
        } else {
            try {
                // Inicializa el servicio de intercambio con la clave API y mostrar mensaje de
                // inicio
                this.exchangeService = new ExchangeServicio(this.apiKey);
            } catch (IllegalArgumentException e) {
                // Maneja errores de inicialización para el servicio de intercambio
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Error al inicializar servicio de cambio: " + e.getMessage(),
                        "Error de Servicio", JOptionPane.ERROR_MESSAGE));
            }
        }

        setTitle("Conversor Gráfico de Moneda 💰");
        // establece la operación de cierre predeterminada para salir de la aplicación
        // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 550);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        initListeners(); // oyentes para los botones

        redirectSystemStreams(); // Redirigir System.out y System.err al área de texto de la consola

        // confirmación de salida ---
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showOptionDialog(
                        ConversorGUI.this,
                        "¿Estás seguro de que quieres salir del conversor?",
                        "Confirmar Salida",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, null, null);
                if (confirm == JOptionPane.YES_OPTION) {
                    System.out.println("Saliendo de la aplicación...");
                    dispose(); // Cierra la ventana
                    System.exit(0); // Termina la aplicación
                } else {
                    System.out.println("Cancelado el cierre.");
                    // No hacemos nada, la ventana sigue abierta
                }
            }
        });// fin bloque cierre ---

        System.out.println("GUI del conversor iniciada.");
        if (exchangeService == null) {
            System.err.println(
                    "ADVERTENCIA: ExchangeServicio no pudo ser inicializado en la GUI. Las conversiones fallarán.");
        }
    }

    private void initComponents() {
        // Panel para controles de conversión (selección de moneda, cantidad, botón de
        // convertir, etiqueta de resultado)
        JPanel panelControlesSuperiores = new JPanel(new GridBagLayout());
        panelControlesSuperiores.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        /// Etiqueta y cuadro combinado "Convertir de:"
        gbc.gridx = 0;
        gbc.gridy = 0;
        panelControlesSuperiores.add(new JLabel("Convertir de:"), gbc);
        comboMonedaOrigen = new JComboBox<>(MONEDAS_COMUNES.toArray(new String[0]));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panelControlesSuperiores.add(comboMonedaOrigen, gbc);
        gbc.weightx = 0.0;

        gbc.gridx = 0;
        gbc.gridy = 1;
        panelControlesSuperiores.add(new JLabel("A:"), gbc);
        comboMonedaDestino = new JComboBox<>(MONEDAS_COMUNES.toArray(new String[0]));
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panelControlesSuperiores.add(comboMonedaDestino, gbc);
        gbc.weightx = 0.0;

        // Etiqueta y campo de texto "Cantidad:"
        gbc.gridx = 0;
        gbc.gridy = 2;
        panelControlesSuperiores.add(new JLabel("Cantidad:"), gbc);
        txtCantidad = new JTextField(10);
        gbc.gridx = 1;
        panelControlesSuperiores.add(txtCantidad, gbc);

        // Botón "Convertir"
        btnConvertir = new JButton("Convertir");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        panelControlesSuperiores.add(btnConvertir, gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        btnConvertir.setBackground(Color.decode("#121418"));

        // Etiqueta de resultado.
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        lblResultado = new JLabel("0.00", SwingConstants.CENTER);
        lblResultado.setFont(new Font("Arial", Font.BOLD, 20));
        lblResultado.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        panelControlesSuperiores.add(lblResultado, gbc);
        gbc.gridwidth = 1;

        add(panelControlesSuperiores, BorderLayout.NORTH);

        JPanel panelContenidoInferior = new JPanel(new BorderLayout(10, 10));

        JPanel panelBotonesDescarga = new JPanel();
        panelBotonesDescarga.setLayout(new BoxLayout(panelBotonesDescarga, BoxLayout.Y_AXIS));
        panelBotonesDescarga.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnDescargarTxt = new JButton("Descargar .txt");
        JButton btnDescargarJson = new JButton("Descargar .json");
        JButton btnDescargarPdf = new JButton("Descargar PDF");
        JButton btnLimpiarTodo = new JButton("Limpiar Todo");

        Dimension buttonSize = new Dimension(150, 30);
        btnDescargarTxt.setMaximumSize(buttonSize);
        btnDescargarJson.setMaximumSize(buttonSize);
        btnDescargarPdf.setMaximumSize(buttonSize);
        btnLimpiarTodo.setMaximumSize(buttonSize);

        btnDescargarTxt.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDescargarJson.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDescargarPdf.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnLimpiarTodo.setAlignmentX(Component.LEFT_ALIGNMENT);

        panelBotonesDescarga.add(Box.createVerticalGlue());
        panelBotonesDescarga.add(btnDescargarTxt);
        panelBotonesDescarga.add(Box.createVerticalGlue());
        panelBotonesDescarga.add(btnDescargarJson);
        panelBotonesDescarga.add(Box.createVerticalGlue());
        panelBotonesDescarga.add(btnDescargarPdf);
        panelBotonesDescarga.add(Box.createVerticalGlue());
        panelBotonesDescarga.add(btnLimpiarTodo);
        panelBotonesDescarga.add(Box.createVerticalGlue());

        btnDescargarTxt.setBackground(Color.decode("#121418"));
        btnDescargarJson.setBackground(Color.decode("#121418"));
        btnDescargarPdf.setBackground(Color.decode("#121418"));
        btnLimpiarTodo.setBackground(Color.decode("#121418"));

        panelContenidoInferior.add(panelBotonesDescarga, BorderLayout.WEST);

        consoleTextArea = new JTextArea(10, 30);
        consoleTextArea.setEditable(false);
        consoleTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        consoleTextArea.setLineWrap(true);
        consoleTextArea.setWrapStyleWord(true);
        consoleTextArea.setBackground(Color.decode("#121418"));
        consoleTextArea.setForeground(Color.ORANGE);
        JScrollPane scrollPane = new JScrollPane(consoleTextArea);

        panelContenidoInferior.add(scrollPane, BorderLayout.CENTER);

        add(panelContenidoInferior, BorderLayout.CENTER);

        comboMonedaOrigen.setSelectedItem("USD");
        comboMonedaDestino.setSelectedItem("ARS");
        txtCantidad.setText("1");

        // Deshabilita controles de conversión si Exchange seervicio no está disponible
        if (exchangeService == null) {
            btnConvertir.setEnabled(false);
            txtCantidad.setEnabled(false);
            comboMonedaOrigen.setEnabled(false);
            comboMonedaDestino.setEnabled(false);
            lblResultado.setText("Servicio no disponible");
            System.err.println("GUI: Controles de conversión deshabilitados porque ExchangeServicio es nulo.");
        }

        // oyentes para los botones de descarga y limpieza
        btnDescargarTxt.addActionListener(e -> descargarTxt());
        btnDescargarJson.addActionListener(e -> descargarJson());
        btnDescargarPdf.addActionListener(e -> descargarPdf());
        btnLimpiarTodo.addActionListener(e -> limpiarTodo());

        // icono de la aplicación si no usara flatlaf
        // try {
        // // Usa una ruta absoluta dentro del classpath
        // java.net.URL iconUrl = getClass().getResource("/img/icon-app.png");
        // if (iconUrl != null) {
        // ImageIcon appIcon = new ImageIcon(iconUrl);
        // setIconImage(appIcon.getImage());
        // System.out.println("Icono de la ventana establecido con éxito desde el
        // classpath.");
        // } else {
        // System.err.println("No se encontró el icono en el classpath: " + iconUrl);
        // }
        // } catch (Exception e) {
        // System.err.println("Error al cargar o establecer el icono de la ventana: " +
        // e.getMessage());
        // }

    }

    private void initListeners() {
        btnConvertir.addActionListener(e -> convertirMoneda());
        txtCantidad.addActionListener(e -> convertirMoneda());

    }

    private void convertirMoneda() {
        // Comprobar si el servicio de intercambio está disponible
        if (exchangeService == null) {
            JOptionPane.showMessageDialog(this,
                    "El servicio de conversión no está disponible.\n" +
                            "Verifique la configuración de la API Key y los mensajes de la consola.",
                    "Servicio No Disponible", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Obtener las monedas seleccionadas y la cantidad
        String origen = (String) comboMonedaOrigen.getSelectedItem();
        String destino = (String) comboMonedaDestino.getSelectedItem();
        String cantidadStr = txtCantidad.getText().trim().replace(",", "."); // Maneja la coma como separador decimal
                                                                             // porque

        // Valida campos de entrada
        if (origen == null || destino == null || cantidadStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione las monedas y ingrese una cantidad.",
                    "Entrada Inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (origen.equals(destino)) {
            JOptionPane.showMessageDialog(this, "Las monedas de origen y destino no pueden ser iguales.",
                    "Entrada Inválida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double cantidad;
        try {
            cantidad = Double.parseDouble(cantidadStr);
            if (cantidad <= 0) {
                JOptionPane.showMessageDialog(this, "La cantidad debe ser un número positivo.", "Cantidad Inválida",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "La cantidad ingresada ('" + cantidadStr + "') no es un número válido.",
                    "Formato de Número Inválido", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Deshabilita el botón de convertir y mostrar mensaje de procesamiento durante
        // la conversión
        btnConvertir.setEnabled(false);
        lblResultado.setText("Procesando...");
        System.out.printf("GUI: Solicitando conversión de %.2f %s a %s...%n", cantidad, origen, destino);

        // Usa SwingWorker para la conversión en segundo plano y mantener la
        // GUIresponsiva
        SwingWorker<Double, Void> worker = new SwingWorker<>() {
            @Override
            protected Double doInBackground() throws Exception {
                // Realiza la conversión en ootro hilo en segundo plano
                return exchangeService.convertCurrency(origen, destino, cantidad);
            }

            @Override
            protected void done() {
                try {
                    double resultadoConversion = get();
                    if (resultadoConversion != -1.0) {
                        DecimalFormat df = new DecimalFormat("#,##0.00##"); // Formatea el resultado
                        lblResultado.setText(df.format(resultadoConversion) + " " + destino);
                        System.out.printf("GUI: Conversión exitosa: %.2f %s = %s %s%n", cantidad, origen,
                                df.format(resultadoConversion), destino);
                    } else {
                        lblResultado.setText("Error de conversión");
                        JOptionPane.showMessageDialog(ConversorGUI.this,
                                "No se pudo realizar la conversión.\nRevise la consola integrada (abajo) para más detalles.",
                                "Error de Conversión", JOptionPane.ERROR_MESSAGE);
                        System.err.println("GUI: Falló la conversión desde la API (retornó -1.0).");
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    System.err.println("GUI: Conversión interrumpida: " + ex.getMessage());
                    lblResultado.setText("Conversión cancelada");
                } catch (java.util.concurrent.ExecutionException ex) {
                    Throwable cause = ex.getCause();
                    System.err.println("GUI: Error durante la ejecución de la conversión: "
                            + (cause != null ? cause.getMessage() : ex.getMessage()));
                    if (cause != null)
                        cause.printStackTrace(System.err);
                    else
                        ex.printStackTrace(System.err);
                    lblResultado.setText("Error en ejecución");
                    JOptionPane.showMessageDialog(ConversorGUI.this,
                            "Ocurrió un error inesperado durante la conversión: \n"
                                    + (cause != null ? cause.getMessage() : ex.getMessage()),
                            "Error Interno", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnConvertir.setEnabled(true); // Volver a habilitar el botón de convertir luego de la conversión
                }
            }
        };
        worker.execute();
    }

    // Métodos para descargar datos de la consola
    private void descargarTxt() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar como archivo de texto");

        // Establecer el directorio predeterminado para guardar archivos
        // File downloadDir = new File(System.getProperty("user.home"),
        // "Descarga-archivos"); //si se quiere usar la ruta del usuario descomentar
        // esta linea y comentar la linea de abajo
        File downloadDir = new File("Descarga-archivos");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs(); // Crea si no existe
        }
        fileChooser.setCurrentDirectory(downloadDir);

        fileChooser
                .setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos de texto (*.txt)", "txt"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt"); // Asegurar la extensión .txt
            }
            try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
                writer.println("Resultado de la conversión:");
                writer.println(lblResultado.getText());
                writer.println("\nDetalles de la consola:");
                writer.println(consoleTextArea.getText());
                JOptionPane.showMessageDialog(this, "Archivo guardado exitosamente.", "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void descargarJson() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar como archivo JSON");

        // File downloadDir = new File(System.getProperty("user.home"),
        // "Descarga-archivos");
        File downloadDir = new File("Descarga-archivos");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        fileChooser.setCurrentDirectory(downloadDir);

        fileChooser
                .setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos JSON (*.json)", "json"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".json")) {
                file = new File(file.getAbsolutePath() + ".json");
            }
            try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
                Gson gson = new Gson(); /// Gson para la serialización JSON
                JsonObject json = new JsonObject();
                json.addProperty("resultado", lblResultado.getText());
                json.addProperty("consola", consoleTextArea.getText());
                writer.println(gson.toJson(json));
                JOptionPane.showMessageDialog(this, "Archivo guardado exitosamente.", "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void descargarPdf() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar como archivo PDF");

        // File downloadDir = new File(System.getProperty("user.home"),
        // "Descarga-archivos");
        File downloadDir = new File("Descarga-archivos");

        if (!downloadDir.exists()) {
            downloadDir.mkdirs();
        }
        fileChooser.setCurrentDirectory(downloadDir);

        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos PDF (*.pdf)", "pdf"));
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();
                document.add(new Paragraph("Resultado de la conversión: " + lblResultado.getText()));
                document.add(new Paragraph("Detalles de la consola:"));
                document.add(new Paragraph(consoleTextArea.getText()));
                document.close();
                JOptionPane.showMessageDialog(this, "Archivo guardado exitosamente.", "Éxito",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (DocumentException | IOException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Método para limpiar la consola, la cantidad y el resultado
    private void limpiarTodo() {
        consoleTextArea.setText("");
        txtCantidad.setText("1");
        lblResultado.setText("0.00");
    }

    // Redirige System.out y System.err al JTextArea
    private void redirectSystemStreams() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        // OutputStream personalizado para System.out
        OutputStream outGui = new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    consoleTextArea.append(String.valueOf((char) b));
                    consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength()); // Desplazarse al final
                });
            }

            @Override
            public void write(byte[] b, int off, int len) {
                final String message = new String(b, off, len, StandardCharsets.UTF_8);
                SwingUtilities.invokeLater(() -> {
                    consoleTextArea.append(message);
                    consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength()); // Desplazarse al final
                });
            }
        };

        // OutputStream personalizado para System.err (simil a outGui)
        OutputStream errGui = new OutputStream() {
            @Override
            public void write(int b) {
                SwingUtilities.invokeLater(() -> {
                    consoleTextArea.append(String.valueOf((char) b));
                    consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
                });
            }

            @Override
            public void write(byte[] b, int off, int len) {
                final String message = new String(b, off, len, StandardCharsets.UTF_8);
                SwingUtilities.invokeLater(() -> {
                    consoleTextArea.append(message);
                    consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
                });
            }
        };

        PrintStream multiOut = new PrintStream(new TeeOutputStream(originalOut, outGui), true, StandardCharsets.UTF_8);
        PrintStream multiErr = new PrintStream(new TeeOutputStream(originalErr, errGui), true, StandardCharsets.UTF_8);

        System.setOut(multiOut);
        System.setErr(multiErr);
    }

    private static class TeeOutputStream extends OutputStream {
        private final OutputStream out1;
        private final OutputStream out2;

        public TeeOutputStream(OutputStream out1, OutputStream out2) {
            this.out1 = out1;
            this.out2 = out2;
        }

        @Override
        public void write(int b) throws IOException {
            out1.write(b);
            out2.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out1.write(b, off, len);
            out2.write(b, off, len);
        }

        @Override
        public void flush() throws IOException {
            out1.flush();
            out2.flush();
        }

        @Override
        public void close() throws IOException {
            try {
                out1.close();
            } finally {
                out2.close();
            }
        }
    }

    public static void main(String[] args) {
        // Configurar el tema FlatLaf oscuro para un aspecto mas moderno
        FlatArcDarkOrangeIJTheme.setup();
        // Crear y mostrar la GUI en un Hilo aparte
        try {
            SwingUtilities.invokeAndWait(() -> new ConversorGUI().setVisible(true));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
