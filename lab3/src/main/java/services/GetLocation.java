package services;

import location.Location;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GetLocation{
    final private String API_KEY = "3788da31-27e8-40c4-ba55-e6a07b7b280c";

    public CompletableFuture<List<Location>> getListLocations(String locationName) {
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://graphhopper.com/api/1/geocode?q=" + locationName + "&key=" + API_KEY))
                .build();


       CompletableFuture<String> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
               .thenApply(HttpResponse::body);

        CompletableFuture<List<Location>> locationList = response.thenApply(result -> {
            JSONObject jsonObject = new JSONObject(result);
            JSONArray jsonArray = jsonObject.getJSONArray("hits");

            List<Location> locations = new ArrayList<>();

            for (Object object : jsonArray) {
                JSONObject jsonsObject = (JSONObject) object;

                Location location = new Location();

                double lon = jsonsObject.getJSONObject("point").getDouble("lng");
                double lat = jsonsObject.getJSONObject("point").getDouble("lat");
                String name = jsonsObject.getString("name");
                String country = jsonsObject.getString("country");

                location.setLat(lat);
                location.setLon(lon);
                location.setCountry(country);
                location.setName(name);

                locations.add(location);
            }
            return locations;
        });

        return locationList;
    }
}

