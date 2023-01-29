package services;

import location.Location;
import org.json.JSONObject;
import result.Weather;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GetWeather{
    final private String API_KEY = "14d86c7ef347ee2819279d9a2e802dca";

    public CompletableFuture<Weather> getInfo(Location location) {
        double lat = location.getLat();
        double lon = location.getLon();

        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.openweathermap.org/data/2.5/weather?lat=" + lat +
                        "&lon=" + lon +
                        "&units=metric" +
                        "&appid=" + API_KEY))
                .build();

        CompletableFuture<String> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);

        return response.thenApply(result -> {
            JSONObject jsonObject = new JSONObject(result).getJSONObject("main");
            return new Weather(jsonObject.getDouble("temp"));
        });
    }
}
