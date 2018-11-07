package uav.gcs.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.PrintWriter;
import java.util.Properties;

public class Network {
    private static Logger logger = LoggerFactory.getLogger(Network.class);
    public static String networkType;
    public static String udpLocalPort;
    public static String tcpServerIP;
    public static String tcpServerPort;
    private static Uav uav;

    static {
        try {
            Properties properties = new Properties();
            FileReader reader = new FileReader(Network.class.getResource("network.properties").getPath());
            properties.load(reader);
            networkType = properties.getProperty("networkType");
            udpLocalPort = properties.getProperty("udpLocalPort");
            tcpServerIP = properties.getProperty("tcpServerIP");
            tcpServerPort = properties.getProperty("tcpServerPort");
        } catch(Exception e) {
            logger.error(e.toString());
        }
    }

    public static void save() {
        try {
            PrintWriter pw = new PrintWriter(Network.class.getResource("network.properties").getPath());
            pw.println("networkType=" + networkType);
            pw.println("udpLocalPort=" + udpLocalPort);
            pw.println("tcpServerIP=" + tcpServerIP);
            pw.println("tcpServerPort=" + tcpServerPort);
            pw.flush();
            pw.close();
        } catch(Exception e) {
            logger.error(e.toString());
        }
    }

    public static Uav createUav() {
        if(networkType.equals("UDP")) {
            uav = new UavUdp(udpLocalPort);
        } else if(networkType.equals("TCP")) {
            uav = new UavTcp(tcpServerIP, tcpServerPort);
        }
        return uav;
    }

    public static Uav getUav() {
        return uav;
    }

    public static void distroyUav() {
        if(uav != null) {
            uav.disconnect();
            uav = null;
        }
    }
}
