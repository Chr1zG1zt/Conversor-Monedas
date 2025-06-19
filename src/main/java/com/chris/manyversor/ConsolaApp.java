package main.java.com.chris.manyversor;

import java.util.InputMismatchException;
import java.util.Scanner;

public class ConsolaApp {
    private static ExchangeServicio exchangeService;
    private static final Scanner scanner = new Scanner(System.in);

    // Colores
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String YELLOW = "\u001B[33m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String BLUE = "\u001B[34m";
    public static final String WHITE = "\u001B[97m";

    // Método para inicializar y ejecutar la aplicación de consola
    public static void run(String apiKey) {
        if (apiKey == null) {
            System.err.println("No se pudo iniciar la aplicación de consola: API Key no proporcionada.");
            return;
        }
        try {
            exchangeService = new ExchangeServicio(apiKey);
        } catch (IllegalArgumentException e) {
            System.err.println("Error al inicializar el servicio de cambio: " + e.getMessage());
            return;
        }

        System.out.println(GREEN + "\n Bienvenido/a al Conversor de Moneda 💰" + RESET);
        boolean salir = false;

        while (!salir) {
            mostrarMenu();
            System.out.print(YELLOW + "Elija una opción válida: " + RESET);
            try {
                int opcion = -1;
                if (scanner.hasNextInt()) {
                    opcion = scanner.nextInt();
                }
                scanner.nextLine();

                switch (opcion) {
                    case 1:
                        realizarConversion("USD", "ARS");
                        break;
                    case 2:
                        realizarConversion("ARS", "USD");
                        break;
                    case 3:
                        realizarConversion("USD", "BRL");
                        break;
                    case 4:
                        realizarConversion("BRL", "USD");
                        break;
                    case 5:
                        realizarConversion("USD", "COP");
                        break;
                    case 6:
                        realizarConversion("COP", "USD");
                        break;
                    case 7:
                        salir = true;
                        System.out.println(BLUE + "Gracias por usar el Conversor de Moneda. ¡Hasta luego!" + RESET);
                        break;
                    case 8:
                        System.out.println(BLUE + "Iniciando modo gráfico..." + RESET);
                        javax.swing.SwingUtilities.invokeLater(() -> {
                            // La GUI creará su propia instancia de ExchangeServicio si es necesario,
                            // o se le puede pasar la apiKey.
                            // Por simplicidad aquí, la GUI se encargará de su propia apiKey.
                            new ConversorGUI().setVisible(true);
                        });
                        System.out.println(
                                BLUE + "Modo gráfico solicitado. La consola puede seguir mostrando mensajes." + RESET);
                        // Nota: La consola no se detiene aquí, sigue activa.
                        // Si se quisiera que la consola espere a que la GUI cierre, se necesitaría más
                        // lógica.
                        break;
                    default:
                        System.out.println(RED + "Opción no válida. Por favor, intente de nuevo." + RESET);
                }
            } catch (InputMismatchException e) {
                System.out.println(RED + "Entrada inválida. Por favor, ingrese un número." + RESET);
                // scanner.nextLine(); // Ya consumido arriba
            }
            if (!salir) {
                System.out
                        .println(CYAN + "********************************************************************" + RESET);
            }
        }
        // No cerramos el scanner aquí si la GUI podría, teóricamente,
        // querer interactuar con System.in, aunque no es el caso en este diseño.
        // Si la aplicación siempre termina aquí, scanner.close() sería mejor xd.
    }

    // MENÚ con estilo "bandera argentina"
    private static void mostrarMenu() {

        // Banner MENÚ
        System.out.println(RED +
                " _   _  ___  _  _  _ _  \n" +
                "| \\_/ || __|| \\| || | |\n" +
                "| \\_/ || _| | \\\\ || U |\n" +
                "|_| |_||___||_|\\_||___|\n" +
                "                       " + RESET);

        // Tabla de opciones
        System.out.println(GREEN + "+--------------------------------------------------------+" + RESET);
        System.out.printf(GREEN + "| %-64s |\n", YELLOW + "1) Dólar (USD) => Peso argentino (ARS)" + GREEN);
        System.out.println("|--------------------------------------------------------|");
        System.out.printf("| %-64s |\n", YELLOW + "2) Peso argentino (ARS) => Dólar (USD)" + GREEN);
        System.out.println("|--------------------------------------------------------|");
        System.out.printf("| %-64s |\n", YELLOW + "3) Dólar (USD) => Real brasileño (BRL)" + GREEN);
        System.out.println("|--------------------------------------------------------|");
        System.out.printf("| %-64s |\n", YELLOW + "4) Real brasileño (BRL) => Dólar (USD)" + GREEN);
        System.out.println("|--------------------------------------------------------|");
        System.out.printf("| %-64s |\n", YELLOW + "5) Dólar (USD) => Peso colombiano (COP)" + GREEN);
        System.out.println("|--------------------------------------------------------|");
        System.out.printf("| %-64s |\n", YELLOW + "6) Peso colombiano (COP) => Dólar (USD)" + GREEN);
        System.out.println("|--------------------------------------------------------|");
        System.out.printf("| %-64s |\n", YELLOW + "7) Salir" + GREEN);
        System.out.println("|--------------------------------------------------------|");
        System.out.printf("| %-64s |\n", YELLOW + "8) Iniciar con interfaz gráfica (GUI)" + GREEN);
        System.out.println("+--------------------------------------------------------+" + RESET);
    }

    private static void realizarConversion(String monedaOrigen, String monedaDestino) {
        System.out.println(BLUE + "------------------------------------------------------------" + RESET);
        System.out.printf("💱  %sConvertir de%s %s%s%s a %s%s%s.%n", GREEN, RESET, CYAN, monedaOrigen, RESET, CYAN,
                monedaDestino, RESET);
        System.out.print("✍️  Ingrese el valor que deseas convertir: ");

        double cantidad;
        try {
            String linea = scanner.nextLine();
            cantidad = Double.parseDouble(linea.replace(",", ".")); // Aceptar coma como decimal
            if (cantidad <= 0) {
                System.out.println(RED + "⚠️  La cantidad debe ser un valor positivo." + RESET);
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println(RED + "❌ Cantidad inválida. Por favor, ingrese un número." + RESET);
            return;
        }

        System.out.printf("⏳ Procesando conversión de %s%.2f%s %s a %s...%n",
                GREEN, cantidad, RESET, monedaOrigen, monedaDestino);

        double resultado = exchangeService.convertCurrency(monedaOrigen, monedaDestino, cantidad);

        if (resultado != -1.0) {
            System.out.printf(GREEN + "✅ El valor %.2f [%s] equivale a =>>> %.2f [%s]%n" + RESET,
                    cantidad, monedaOrigen, resultado, monedaDestino);
        } else {
            System.out.println(RED + "❌ No se pudo realizar la conversión. Revise los mensajes anteriores." + RESET);
        }
        System.out.println(BLUE + "------------------------------------------------------------" + RESET);
    }
}
