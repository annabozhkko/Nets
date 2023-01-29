import sun.nio.ch.Net;

import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;

public class NetworkInterfaceExample {

    public static void main(String[] args) throws SocketException {

        // NetworkInterface implements a static method that returns all the interfaces on the PC,
        // which we add on a list in order to iterate over them.
        ArrayList interfaces = Collections
                .list(NetworkInterface.getNetworkInterfaces());

        System.out.println("Printing information about the available interfaces...\n");
        for (Object iface : interfaces) {
            NetworkInterface networkInterface = (NetworkInterface) iface;
            // Due to the amount of the interfaces, we will only print info
            // about the interfaces that are actually online.
            if (networkInterface.isUp()) {

                // Display name
                System.out.println("Interface name: " + networkInterface.getDisplayName());

                // Interface addresses of the network interface
                System.out.println("\tInterface addresses: ");
                for (InterfaceAddress addr : networkInterface.getInterfaceAddresses()) {
                    System.out.println("\t\t" + addr.getAddress().toString());
                }

                // MTU (Maximum Transmission Unit)
                System.out.println("\tMTU: " + networkInterface.getMTU());

                // Subinterfaces
                System.out.println("\tSubinterfaces: " + Collections.list(networkInterface.getSubInterfaces()));

                // Check other information regarding the interface
                System.out.println("\tis loopback: " + networkInterface.isLoopback());
                System.out.println("\tis virtual: " + networkInterface.isVirtual());
                System.out.println("\tis point to point: " + networkInterface.isPointToPoint());
            }
        }
    }
}
