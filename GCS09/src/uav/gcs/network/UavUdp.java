package uav.gcs.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

public class UavUdp extends Uav {
    private static Logger logger = LoggerFactory.getLogger(UavUdp.class);
    private String udpLocalPort;
    private DatagramSocket datagramSocket;
    private String sendIp;
    private int sendPort;

    public UavUdp(String udpLocalPort) {
        this.udpLocalPort = udpLocalPort;
    }

    @Override
    public void connect() {
        try {
            //통신 방식에 따라 연결 코드
            datagramSocket = new DatagramSocket(Integer.parseInt(udpLocalPort));
            //연결 후 공통 실행 코드
            super.connect();
        } catch(Exception e) {
            logger.error(e.toString());
        }
    }

    @Override
    public void disconnect() {
        //연결 끊기 전 공통 실행 코드
        super.disconnect();
        //통신 방식에 따라 연결 끊기 코드
        datagramSocket.close();
    }

    @Override
    public void receiveMessage() throws Exception {
        byte[] buffer = new byte[263];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while(connected) {
            datagramSocket.receive(packet);
            int readNum = packet.getLength();
            sendIp = packet.getAddress().getHostAddress();
            sendPort = packet.getPort();
            for(int i=0; i<readNum; i++) {
                parsingMAVLinkMessage(buffer[i]);
            }
        }
    }

    @Override
    public void sendMessage(byte[] bytes) throws Exception {
        if(sendIp != null) {
            DatagramPacket packet = new DatagramPacket(
                    bytes, bytes.length, new InetSocketAddress(sendIp, sendPort));
            datagramSocket.send(packet);
        }
    }
}
