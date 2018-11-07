package uav.gcs.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class UavTcp extends Uav {
    private static Logger logger = LoggerFactory.getLogger(UavTcp.class);
    private String tcpServerIP;
    private String tcpServerPort;
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    public UavTcp(String tcpServerIP, String tcpServerPort) {
        this.tcpServerIP = tcpServerIP;
        this.tcpServerPort = tcpServerPort;
    }

    @Override
    public void connect() {
        try {
            //통신 방식에 따라 연결 코드
            socket = new Socket(tcpServerIP, Integer.parseInt(tcpServerPort));
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
            //연결 후 공통 실행 코드
            super.connect();
        } catch(Exception e) {
            logger.error(e.toString());
        }
    }

    @Override
    public void disconnect() {
        try {
            //연결 끊기 전 공통 실행 코드
            super.disconnect();
            //통신 방식에 따라 연결 끊기 코드
            socket.close();
        } catch(Exception e) {
            logger.error(e.toString());
        }
    }

    @Override
    public void receiveMessage() throws Exception {
        byte[] buffer = new byte[1024];
        int readNum = -1;
        while(connected) {
            readNum = inputStream.read(buffer);
            if(readNum == -1) {
                throw new Exception("read returned -1");
            }
            for(int i=0; i<readNum; i++) {
                parsingMAVLinkMessage(buffer[i]);
            }
        }
    }

    @Override
    public void sendMessage(byte[] bytes) throws Exception {
        outputStream.write(bytes);
        outputStream.flush();
    }
}
