package ya.weather;

import com.google.gson.*;
import io.github.cdimascio.dotenv.Dotenv;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class Informer {

    // 55.890999, 37.725437
    private static final double latitude = 55.890999;
    private static final double longitude = 37.725437;
    private static final int forecastsLimit = 3;
    private static final String URL = "https://api.weather.yandex.ru/v2/forecast";
    private static String API_KEY;

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        API_KEY = dotenv.get("API_KEY");
        URI uri = getURI();
        HttpResponse<String> response = getResponse(uri); // 1, 2
        JsonObject weatherData = getWeatherData(response);
        printWeatherMsg(weatherData);
    }

    private static void printWeatherMsg(JsonObject data) {
        JsonObject info = data.get("info").getAsJsonObject();
        JsonObject fact = data.get("fact").getAsJsonObject();
        JsonArray forecasts = data.get("forecasts").getAsJsonArray();
        System.out.println(data); // 3.1

        String eol = System.lineSeparator();
        String today = getToday();
        String cityUrl = info.get("url").getAsString();
        String temperature = fact.get("temp").getAsString(); // 3.2
        String season = fact.get("season").getAsString();
        String condition = fact.get("condition").getAsString();

        printDelimiter('-', 40);
        System.out.print("Today " + today + eol);
        System.out.printf("In the N-city (%f, %f) %s" + eol, latitude, longitude, season);
        System.out.printf("Temperature: %s°C, %s" + eol, temperature, condition);
        System.out.printf("(more info here: %s)" + eol, cityUrl);

        printDelimiter('-', 40);
        double avgTemperature = getAvgTemperature(forecasts); // 4
        System.out.printf("Average temperature for the next %d days: %.2f°C" + eol, forecasts.size(), avgTemperature);
        printDelimiter('-', 40);
    }

    private static URI getURI() {
        String path = "?lat=" + latitude + "&lon=" + longitude + "&lang=ru_RU";
        if (forecastsLimit > 0 && forecastsLimit < 8) path += "&limit=" + forecastsLimit;
        return URI.create(URL + path);
    }

    private static HttpResponse<String> getResponse(URI uri) {
        HttpResponse<String> response = null;
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(uri)
                            .GET()
                            .header("X-Yandex-Weather-Key", API_KEY)
                            .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.println("Something went wrong!\nStatus code: " + response.statusCode() + "\n" + response.headers().toString());
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return response;
    }

    private static JsonObject getWeatherData(HttpResponse<String> response) {
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private static String getToday() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        return dateFormat.format(calendar.getTime());
    }

    private static void printDelimiter(char delimiter, int length) {
        System.out.println("\n" + String.valueOf(delimiter).repeat(length));
    }

    public static double getAvgTemperature(JsonArray forecasts) {
        int result = 0;
        for (JsonElement day : forecasts) {
            result += day.getAsJsonObject().get("parts")
                    .getAsJsonObject().get("day_short")
                    .getAsJsonObject().get("temp").getAsInt();
        }
        return forecasts.isEmpty() ? result : (double) result / forecasts.size();
    }
}
