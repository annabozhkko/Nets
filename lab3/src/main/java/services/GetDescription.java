package services;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class GetDescription {
    final private String API_KEY = "5ae2e3f221c38a28845f05b601625fcca6476d47971455a7cef5ebc1";

    public CompletableFuture<String> getInfo(String xid){
        HttpClient httpClient = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://api.opentripmap.com/0.1/en/places/xid/" + xid + "?apikey=" + API_KEY))
                .build();

        CompletableFuture<String> response = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);

        return response.thenApply(result -> {
            JSONObject jsonObject = new JSONObject(result);

            if (jsonObject.has("kinds")) {
                return jsonObject.getString("kinds");
            }
            return null;
        });
    }
}
