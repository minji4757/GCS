package uav.gcs.mission;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_mission_ack;
import com.MAVLink.common.msg_mission_item_int;
import com.MAVLink.enums.*;
import common.AlertDialog;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.omg.PortableServer.THREAD_POLICY_ID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uav.gcs.hud.HudController;
import uav.gcs.network.Network;
import uav.gcs.network.Uav;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MissionController implements Initializable {
    private static Logger logger = LoggerFactory.getLogger(MissionController.class);
    public static MissionController instance;

    @FXML
    private WebView webView;
    private WebEngine webEngine;
    private JSObject jsproxy;

    @FXML
    private Slider zoomSlider;

    @FXML
    private CheckBox chkManualMove;
    @FXML
    private CheckBox chkManualAlt;
    @FXML
    private TextField txtManualAlt;

    private Thread infoLableThread;
    private Thread infoAlertThread;
    @FXML private Label lblInfo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        MissionController.instance = this;
        initWebView();
        zoomSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                int value = newValue.intValue();
                jsproxy.call("setMapZoom", value);
            }
        });
    }

    private void initWebView() {
        webEngine = webView.getEngine();
        webEngine.getLoadWorker().stateProperty().addListener(webEngineLoadStateChangeListener);
        webEngine.load(MissionController.class.getResource("javascript/map.html").toExternalForm());
    }

    private ChangeListener<Worker.State> webEngineLoadStateChangeListener =
            new ChangeListener<Worker.State>() {
                @Override
                public void changed(ObservableValue<? extends Worker.State> observable,
                                    Worker.State oldValue,
                                    Worker.State newValue) {
                    if (newValue == Worker.State.SUCCEEDED) {
                        jsproxy = (JSObject) webEngine.executeScript("jsproxy");
                        jsproxy.setMember("java", MissionController.this);
                    }
                }
            };

    public void javascriptLog(String level, String methodName, String message) {
        if (level.equals("ERROR")) {
            logger.error(methodName + ":" + message);
        } else if (level.equals("INFO")) {
            logger.info(message);
        }
    }

    public void setZoomSliderValue(int zoom) {
        zoomSlider.setValue(zoom);
    }

    public void changeStatus() {
        Platform.runLater(() -> {
            Uav uav = Network.getUav();

            if (uav.homeLat != 0.0) {
                jsproxy.call("setHomePosition", uav.homeLat, uav.homeLng);
            }

            if (uav.currLat != 0.0) {
                jsproxy.call("setCurrLocation", uav.currLat, uav.currLng, uav.heading);
            }
            if (uav.modeNum == COPTER_MODE.COPTER_MODE_GUIDED ||
                    uav.modeNum == COPTER_MODE.COPTER_MODE_AUTO ||
                    uav.modeNum == COPTER_MODE.COPTER_MODE_RTL ||
                    uav.modeNum == COPTER_MODE.COPTER_MODE_LAND) {
                jsproxy.call("setMode", uav.modeNum);
            }
        });
    }

    public void handleBtnManual(ActionEvent e) {
        Uav uav = Network.getUav();

        boolean isMove = chkManualMove.isSelected();
        boolean isAlt = chkManualAlt.isSelected();
        double manualAlt = Double.parseDouble(txtManualAlt.getText());

        if (isMove == true && isAlt == false) {
            jsproxy.call("manualMake", uav.alt);
        } else if (isMove == true && isAlt == true) {
            jsproxy.call("manualMake", manualAlt);
        } else if (isMove == false && isAlt == true) {
            uav.sendSetPositionTargetGlobalInt(uav.currLat, uav.currLng, manualAlt);
        }
    }

    public void handleChkManualAlt(ActionEvent e) {
        if (chkManualAlt.isSelected()) {
            Uav uav = Network.getUav();
            txtManualAlt.setText(String.valueOf(uav.alt));
        }
    }

    public void manualMove(double manualLat, double manualLng, double manualAlt) {
        Uav uav = Network.getUav();
        uav.sendSetPositionTargetGlobalInt(manualLat, manualLng, manualAlt);

    }

    public void handleBtnMissionUpload(ActionEvent e) {
        Uav uav = Network.getUav();
        //미션 아이템을 저장할 Map 자료 구조
        Map<Integer, msg_mission_item_int> items = new HashMap<>();

        //0번 아이템 생성 (홈의 위치)
        msg_mission_item_int msg0 = new msg_mission_item_int();
        msg0.target_system = 1;
        msg0.target_component = MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1; //펌웨어 쪽으로 보낸다
        msg0.seq = 0; // 0부터 시작
        msg0.command = MAV_CMD.MAV_CMD_NAV_WAYPOINT; //미션 내용
        msg0.x = (int) (uav.homeLat * 10000000); //실수는 정밀도가 있는데 정수는 정밀도가 아닌 정확한 값
        msg0.y = (int) (uav.homeLng * 10000000);
        msg0.autocontinue = 1; //다음 미션으로 가기위한문


        //1번 아이템 생성
        msg_mission_item_int msg1 = new msg_mission_item_int();
        msg1.target_system = 1;
        msg1.target_component = MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1; //펌웨어 쪽으로 보낸다
        msg1.seq = 1;
        msg1.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF; //미션 내용
        msg1.z = 10;
        msg1.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
        msg1.autocontinue = 1;

        //2번 아이템 생성
        msg_mission_item_int msg2 = new msg_mission_item_int();
        msg2.target_system = 1;
        msg2.target_component = MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1; //펌웨어 쪽으로 보낸다
        msg2.seq = 2;
        msg2.command = MAV_CMD.MAV_CMD_NAV_WAYPOINT; //미션 내용
        msg2.x = 375482521;
        msg2.y = 1271185541;
        msg2.z = 10;
        msg2.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
        msg2.autocontinue = 1;

        //3번 아이템 생성
        msg_mission_item_int msg3 = new msg_mission_item_int();
        msg3.target_system = 1;
        msg3.target_component = MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1; //펌웨어 쪽으로 보낸다
        msg3.seq = 3;
        msg3.command = MAV_CMD.MAV_CMD_NAV_WAYPOINT; //미션 내용
        msg3.x = 375465168;
        msg3.y = 1271186829;
        msg3.z = 10;
        msg3.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
        msg3.autocontinue = 1;

        //4번 아이템 생성
        msg_mission_item_int msg4 = new msg_mission_item_int();
        msg4.target_system = 1;
        msg4.target_component = MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1; //펌웨어 쪽으로 보낸다
        msg4.seq = 4;
        msg4.command = MAV_CMD.MAV_CMD_NAV_RETURN_TO_LAUNCH; //미션 내용
        msg4.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
        msg4.autocontinue = 1;

        items.put(0, msg0);
        items.put(1, msg1);
        items.put(2, msg2);
        items.put(3, msg3);
        items.put(4, msg4);

        Alert alert = AlertDialog.showNoButton("알림", "미션 업로드 중...");

        Uav.addMAVLinkMessageListener(
                msg_mission_ack.MAVLINK_MSG_ID_MISSION_ACK,
                new Uav.MAVLInkMessageListener() {
            @Override
            public void receive(MAVLinkMessage mavLinkMessage) {
                msg_mission_ack msg = (msg_mission_ack) mavLinkMessage;
                if(msg.type == MAV_MISSION_RESULT.MAV_MISSION_ACCEPTED) {
                    closeInfoAlert(alert);
                    //showInfoLabel("미션 업로드 성공"); //라벨로 할 경우
                    Uav.removeMAVLinkMessageListener(msg_mission_ack.MAVLINK_MSG_ID_MISSION_ACK, this);
                } else {
                    uav.sendMissionCount(items);
                }
            }
        });
        uav.sendMissionCount(items);
    }

    public void showInfoLabel(String info) { //3초 보였다가 사라지는거
        if(infoLableThread != null) {
            infoLableThread.interrupt(); //스레드 멈추는 법 1.stop플래그 2.interrupt 일시정지 상태에서 스레드 예외발생or 매번 인터럽트 되었는지 확인
        }
        infoLableThread = new Thread() {
            @Override
            public void run() {
                try {
                    Platform.runLater(()->{
                        lblInfo.setText(info);
                    });
                    Thread.sleep(3000); //interrupt 넣으면 예외발생
                    Platform.runLater(()->{ //이전 인포를 지우고 새로운 인포 발생
                        lblInfo.setText("");
                    });
                } catch(Exception e) {}
            }
        };
        infoLableThread.setDaemon(true);
        infoLableThread.start();
    }

    public void closeInfoAlert(Alert alert) { //1.5초 보였다가 사라지는거
        if(infoAlertThread != null) {
            infoAlertThread.interrupt(); //스레드 멈추는 법 1.stop플래그 2.interrupt 일시정지 상태에서 스레드 예외발생or 매번 인터럽트 되었는지 확인
        }
        infoAlertThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000); //1초가 되기전에 또다른 Alert 다이얼로그를 띄워줘야함. interrupt 넣으면 예외발생
                } catch(Exception e) {}
                Platform.runLater(()->{ //이전 인포를 지우고 새로운 인포 발생
                    alert.close();
                });
                infoAlertThread = null;
            }
        };
        infoAlertThread.setDaemon(true);
        infoAlertThread.start();
    }
    public void handleBtnMissionMake(ActionEvent e) {
        Uav uav = Network.getUav();

        if(uav == null) {
            AlertDialog.showOkButton("알림", "드론이 연결되어 있지 않습니다.");
            return;
        }

        if(uav.homeLat == 0.0) {
            AlertDialog.showOkButton("알림", "홈이 없습니다.(시동필요)");
            return;
        }

        jsproxy.call("missionMake");
    }
}
