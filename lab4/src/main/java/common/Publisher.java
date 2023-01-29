package common;

import java.util.ArrayList;
import java.util.List;

public class Publisher {
    public List<Subscriber> subscribers = new ArrayList<>();

    public void addSubscriber(Subscriber subscriber){
        subscribers.add(subscriber);
    }

    public void notifySubscribers(){
        for(Subscriber subscriber : subscribers){
            subscriber.update();
        }
    }
}
