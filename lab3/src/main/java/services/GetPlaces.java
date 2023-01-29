package services;

import location.Location;
import org.json.JSONArray;
import org.json.JSONObject;
import result.Place;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GetPlaces{
    final private String API_KEY = "5ae2e3f221c38a28845f05b601625fcca6476d47971455a7cef5ebc1";

    public CompletableFuture<List<Place>> getInfo(Location location) {
        double lat = location.getLat();
        double lon = location.getLon();

        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.opentripmap.com/0.1/en/places/radius?radius=" + 1000 + "&lon=" +
                        lon + "&lat=" + lat + "&apikey=" + API_KEY))
                .build();

        CompletableFuture<String> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);

        return response.thenApply(result -> {
            JSONObject jsonObject = new JSONObject(result);
            JSONArray jsonArray = jsonObject.getJSONArray("features");

            List<Place> places = new ArrayList<>();
            for (Object object : jsonArray) {
                JSONObject jsonsObject = (JSONObject) object; //одно место
                JSONObject jsonObjectPlace = jsonsObject.getJSONObject("properties");

                Place place = new Place(jsonObjectPlace.getString("name"), jsonObjectPlace.getString("xid"));
                places.add(place);
            }
            return places;
        });
    }
}

