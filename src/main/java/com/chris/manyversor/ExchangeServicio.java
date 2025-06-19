package main.java.com.chris.manyversor; 

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ExchangeServicio {
    private static final String API_BASE_URL = "https://v6.exchangerate-api.com/v6/";
    private final String apiKey;
    private final Gson gson;
    private final HttpClient httpClient;

    // POJO para mapear la respuesta JSON
    private static class ExchangeApiResponse {
        String result; // "success" o "error"

        @SerializedName("error-type")
        String errorType;

        @SerializedName("conversion_result")
        Double conversionResult; // Para /pair/FROM/TO/AMOUNT

        @SerializedName("conversion_rate")
        Double conversionRate;   // Para /pair/FROM/TO
    }

    public ExchangeServicio(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API Key no puede ser nula o vacía.");
        }
        this.apiKey = apiKey;
        this.gson = new Gson();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)) // Timeout para conexión
                .build();
    }

    private ExchangeApiResponse makeApiRequest(String urlString) throws IOException, InterruptedException, JsonSyntaxException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                .timeout(Duration.ofSeconds(10)) // Timeout para la solicitud completa
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String responseBody = response.body();

        if (response.statusCode() == 200) {
            ExchangeApiResponse apiResponse = gson.fromJson(responseBody, ExchangeApiResponse.class);
            if (apiResponse == null) { // GSON podría devolver null si el JSON está completamente vacío
                throw new JsonSyntaxException("La respuesta JSON está vacía o no es válida.");
            }
            return apiResponse;
        } else {
            System.err.printf("Error en la solicitud HTTP: %d%nRespuesta: %s%n", response.statusCode(), responseBody);
            /// Intentar parsear si es un error JSON de la API
            try {
                ExchangeApiResponse errorResponse = gson.fromJson(responseBody, ExchangeApiResponse.class);
                if (errorResponse != null && "error".equals(errorResponse.result)) {
                    return errorResponse; // Devolver el objeto de error parseado
                }
            } catch (JsonSyntaxException e) {
                // NNo era un JSON de error válido de la API, el mensaje de error HTTP es suficiente
            }
            throw new IOException("Respuesta HTTP no exitosa: " + response.statusCode());
        }
    }

    public double convertCurrency(String fromCurrency, String toCurrency, double amount) {
        if (amount <= 0) {
            System.err.println("La cantidad a convertir debe ser positiva.");
            return -1.0;
        }
        String urlString = API_BASE_URL + apiKey + "/pair/" + fromCurrency + "/" + toCurrency + "/" + amount;

        try {
            ExchangeApiResponse apiResponse = makeApiRequest(urlString);

            if ("success".equals(apiResponse.result)) {
                if (apiResponse.conversionResult != null) {
                    return apiResponse.conversionResult;
                } else {
                    System.err.println("Error: La API devolvió 'success' pero 'conversion_result' es nulo.");
                    return -1.0;
                }
            } else {
                handleApiError(apiResponse, fromCurrency, toCurrency);
                return -1.0;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error de red o interrupción al convertir moneda: " + e.getMessage());
            return -1.0;
        } catch (JsonSyntaxException e) {
            System.err.println("Error al parsear la respuesta JSON (GSON) para conversión: " + e.getMessage());
            return -1.0;
        }
    }

    // getExchangeRate no se usa en el menú actual, pero lo mantenemos por si se necesita
    public double getExchangeRate(String fromCurrency, String toCurrency) {
        String urlString = API_BASE_URL + apiKey + "/pair/" + fromCurrency + "/" + toCurrency;
        try {
            ExchangeApiResponse apiResponse = makeApiRequest(urlString);

            if ("success".equals(apiResponse.result)) {
                if (apiResponse.conversionRate != null) {
                    return apiResponse.conversionRate;
                } else {
                    System.err.println("Error: La API devolvió 'success' pero 'conversion_rate' es nulo.");
                    return -1.0;
                }
            } else {
                handleApiError(apiResponse, fromCurrency, toCurrency);
                return -1.0;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error de red o interrupción al obtener tasa: " + e.getMessage());
            return -1.0;
        } catch (JsonSyntaxException e) {
            System.err.println("Error al parsear la respuesta JSON (GSON) para tasa: " + e.getMessage());
            return -1.0;
        }
    }

    private void handleApiError(ExchangeApiResponse apiResponse, String from, String to) {
        String errorType = (apiResponse != null && apiResponse.errorType != null) ? apiResponse.errorType : "desconocido";
        System.err.println("Error de la API: " + errorType);
        switch (errorType) {
            case "unsupported-code":
                System.err.println("Una o ambas monedas (" + from + ", " + to + ") no son soportadas por la API.");
                break;
            case "malformed-request":
                System.err.println("La solicitud a la API está malformada. Revise los códigos de moneda.");
                break;
            case "invalid-key":
                System.err.println("API Key inválida. Revise su archivo 'api_key.txt'.");
                break;
            case "inactive-account":
                System.err.println("La cuenta de la API Key está inactiva.");
                break;
            case "quota-reached":
                System.err.println("Se ha alcanzado la cuota de solicitudes para esta API Key.");
                break;
            default:
                System.err.println("Error desconocido de la API. Código de error: " + errorType);
                break;
        }
    }
}