package result;

import java.util.ArrayList;
import java.util.List;

public class Result {
    private Weather weather;
    private List<Place> places = new ArrayList<>();
    private List<String> descriptions;

    public void setDescriptions(List<String> descriptions) {
        this.descriptions = descriptions;
    }

    public void setPlaces(List<Place> places) {
        this.places = places;
    }

    public void setWeather(Weather weather) {
        this.weather = weather;
    }

    public void print(){
        System.out.println("\nTemperature: " + weather.getTemp() + " °C");
        System.out.println("\nInteresting places:");
        for(int i = 0; i < places.size(); ++i){
            System.out.println((i + 1) + ". " + places.get(i).getName());
            if(descriptions.get(i) != null) {
                System.out.println("Description:");
                System.out.println(descriptions.get(i) + "\n");
            }
        }
    }
}
