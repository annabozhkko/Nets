import location.Location;
import result.Place;
import result.Result;
import result.Weather;
import services.GetDescription;
import services.GetLocation;
import services.GetPlaces;
import services.GetWeather;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        GetLocation getLocation = new GetLocation() ;

        String loc = readLocation();
        CompletableFuture<List<Location>> locF = getLocation.getListLocations(loc);
        List<Location> locations = locF.get();
        printLocations(locations);
        int locIndex = readLocIndex();
        CompletableFuture<Result> result = getResult(locations, locIndex);
        result.join().print();
    }

    private static int readLocIndex() {
        System.out.println("\nSelect location:");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextInt() - 1;
    }

    private static void printLocations(List<Location> locations) {
        int idx = 1;
        for(Location location : locations){
            System.out.println(idx + ". " + location.getName() + " " + location.getCountry());
            System.out.println(location.getLon()+ " " + location.getLat());
            idx++;
        }
    }

    private static String readLocation() {
        System.out.println("Enter location\n");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    private static CompletableFuture<Result> getResult(List<Location> locations, int locIndex) {
        GetWeather getWeather = new GetWeather();
        GetPlaces getPlaces = new GetPlaces();
        GetDescription getDescription = new GetDescription();

        CompletableFuture<Weather> weatherF = getWeather.getInfo(locations.get(locIndex));
        CompletableFuture<List<Place>> placesF = getPlaces.getInfo(locations.get(locIndex));

        CompletableFuture<List<String>> descriptionsF = placesF.thenCompose(places -> {
            List<CompletableFuture<String>> f = new ArrayList<>();
            for(Place place : places) {
                CompletableFuture<String> descriptionF = getDescription.getInfo(place.getXid());
                f.add(descriptionF);
            }

            return CompletableFuture.allOf(f.toArray(new CompletableFuture[f.size()])).thenApply(result ->{
                List<String> descriptions = new ArrayList<>();
                for(CompletableFuture<String> string : f){
                    descriptions.add(string.join());
                }
                return descriptions;
            });
        });

        return CompletableFuture.allOf(weatherF, placesF, descriptionsF).thenApply(f -> {
            Result result = new Result();

            result.setDescriptions(descriptionsF.join());
            result.setPlaces(placesF.join());
            result.setWeather(weatherF.join());

            return result;
        });
    }
}
