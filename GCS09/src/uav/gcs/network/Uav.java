package uav.gcs.network;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Parser;
import com.MAVLink.common.*;
import com.MAVLink.enums.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Uav {
    //Interface:Connection 이벤트 감시-------------------------------------------------------------------
    public static interface ConnectionListener {
        void connect(Uav uav);
        void disconnect(Uav uav);
    }

    //ConnectionListener 저장을 위한 자료구조 선언
    private static List<ConnectionListener> connectionListeners = new ArrayList<>();

    //ConnectionListener를 저장
    public static void addConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.add(connectionListener);
    }

    //ConnectionListener를 제거
    public static void removeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.remove(connectionListener);
    }

    //Interface:MAVLinkMessage 수신 이벤트 감시------------------------------------------------------------
    public static interface MAVLInkMessageListener {
        void receive(MAVLinkMessage mavLinkMessage);
    }

    //MAVLinkMessageListener 저장을 위한 자료구조 선언
    private static Map<Integer, List<MAVLInkMessageListener>> mavlinkMessageListeners = new HashMap<>();

    //MAVLinkMessageListener를 저장
    public static void addMAVLinkMessageListener(int msgid, MAVLInkMessageListener listener) {
        boolean isMsgid = mavlinkMessageListeners.containsKey(msgid);
        if (isMsgid) {
            List<MAVLInkMessageListener> list = mavlinkMessageListeners.get(msgid);
            list.add(listener);
        } else {
            List<MAVLInkMessageListener> list = new ArrayList<>();
            list.add(listener);
            mavlinkMessageListeners.put(msgid, list);
        }
    }

    //MAVLinkMessageListener를 제거
    public static void removeMAVLinkMessageListener(int msgid, MAVLInkMessageListener listener) {
        boolean isMsgid = mavlinkMessageListeners.containsKey(msgid);
        if (isMsgid) {
            List<MAVLInkMessageListener> list = mavlinkMessageListeners.get(msgid);
            list.remove(listener);
        }
    }

    //Interface:Armed 또는 Disarmed 상태 변화 감시--------------------------------------------------------
    public static interface ArmStatusLisener {
        void statusChange(boolean armed);
    }

    //ArmStatusLisener 저장을 위한 자료구조 선언
    private static List<ArmStatusLisener> armStatusLiseners = new ArrayList<>();

    //ArmStatusLisener를 저장
    public static void addArmStatusLisener(ArmStatusLisener armStatusLisener) {
        armStatusLiseners.add(armStatusLisener);
    }

    //ArmStatusLisener를 제거
    public static void removeArmStatusLisener(ArmStatusLisener armStatusLisener) {
        armStatusLiseners.remove(armStatusLisener);
    }

    //Field-----------------------------------------------------------------------------------------------
    private static Logger logger = LoggerFactory.getLogger(Uav.class);
    //연결 상태 선언
    public boolean connected;
    //MAVLink 파서 선언
    private Parser parser = new Parser();
    //드론의 정보를 저장하는 필드 선언
    public String type;
    public String autopilot;
    public String systemStatus;
    public String mode;
    public int modeNum;
    public boolean armed;
    public double roll;
    public double pitch;
    public double yaw;
    public double alt;
    public double heading;
    public double voltage_battery;
    public double current_battery;
    public double battery_remaining;
    public int gps_fix_type;
    public double airspeed;
    public double groundspeed;
    public double homeLat;
    public double homeLng;
    public double currLat;
    public double currLng;
    private Map<Integer, msg_mission_item_int> missionItems;


    //Method--------------------------------------------------------------
    //연결 후에 실행
    public void connect() {
        connected = true;

        List<ConnectionListener> copy = new ArrayList<>(connectionListeners);
        for (ConnectionListener listener : copy) {
            listener.connect(this);
        }

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    //MAVLinkMessage 수신
                    receiveMessage();
                } catch (Exception e) {
                    disconnect();
                }
            }
        };
        thread.setName("MAVLink Receive Thread");
        thread.setDaemon(true);
        thread.start();

        //드론이 보낸 MAVLink 메시지에서 정보 얻기
        receiveHeartBeat();
        receiveAttitude();
        receiveGlobalPositionInt();
        receiveSysStatus();
        receiveGpsRawInt();
        receiveVfrHud();
        receiveHomePosition();
        receiveMissionRequst();

        //드론에게 명령 보내기
        sendHeartBeat();
        sendRequestDataStream();
        sendCmdGetHomePosition();
    }

    //연결 끊기 전에 실행
    public void disconnect() {
        connected = false;
        List<ConnectionListener> copy = new ArrayList<>(connectionListeners);
        for (ConnectionListener listener : copy) {
            listener.disconnect(this);
        }
    }

    //통신 방법에 따른 데이터 받기
    public abstract void receiveMessage() throws Exception;
    //통신 방법에 따른 데이터 보내기
    public abstract void sendMessage(byte[] bytes) throws Exception;

    //MAVLink 메시지 파싱
    public void parsingMAVLinkMessage(byte signed) {
        int unsigned = signed & 0xFF;
        MAVLinkPacket mavLinkPacket = parser.mavlink_parse_char(unsigned);
        if (mavLinkPacket != null) {
            MAVLinkMessage mavLinkMessage = mavLinkPacket.unpack();
            List<MAVLInkMessageListener> list = mavlinkMessageListeners.get(mavLinkMessage.msgid);
            if (list == null) return;
            //MAVLinkMessageListener로 MAVLink 메시지 처리
            //1회성 MAVLinkMessageListener가 제거되더라도
            //for문이 안정적으로 반복 실행이 되도록 List 복사
            List<MAVLInkMessageListener> copy = new ArrayList<>(list);
            for (MAVLInkMessageListener listener : copy) {
                listener.receive(mavLinkMessage);
            }
        }
    }

    //드론이 보내는 MAVLink 메시지 처리######################################################
    //드론 기체 타입, 펌웨어 종류, 비행 모드, 시동 여부 얻기
    public void receiveHeartBeat() {
        addMAVLinkMessageListener(msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT, new MAVLInkMessageListener() {
            @Override
            public void receive(MAVLinkMessage mavLinkMessage) {
                msg_heartbeat msg = (msg_heartbeat) mavLinkMessage;
                //드론의 기체 타입 얻기
                switch (msg.type) {
                    case MAV_TYPE.MAV_TYPE_QUADROTOR:
                        type = "QUADROTOR";
                        break;
                    case MAV_TYPE.MAV_TYPE_HEXAROTOR:
                        type = "HEXAROTOR";
                        break;
                    case MAV_TYPE.MAV_TYPE_OCTOROTOR:
                        type = "OCTOROTOR";
                        break;
                }
                //드론 FC의 펌웨어(운영체제) 이름 얻기
                switch (msg.autopilot) {
                    case MAV_AUTOPILOT.MAV_AUTOPILOT_ARDUPILOTMEGA:
                        autopilot = "ARDUPILOTMEGA";
                        break;
                    case MAV_AUTOPILOT.MAV_AUTOPILOT_GENERIC:
                        autopilot = "GENERIC";
                        break;
                }
                //FC의 상태 얻기
                switch (msg.system_status) {
                    case MAV_STATE.MAV_STATE_BOOT:
                        systemStatus = "BOOT";
                        break;
                    case MAV_STATE.MAV_STATE_STANDBY:
                        systemStatus = "STANDBY";
                        break;
                }
                //비행 모드 얻기
                modeNum = (int) msg.custom_mode;
                switch ((int) msg.custom_mode) {
                    case COPTER_MODE.COPTER_MODE_STABILIZE:
                        mode = "STABILIZE";
                        break;
                    case COPTER_MODE.COPTER_MODE_ALT_HOLD:
                        mode = "ALT_HOLD";
                        break;
                    case COPTER_MODE.COPTER_MODE_LOITER:
                        mode = "LOITER";
                        break;
                    case COPTER_MODE.COPTER_MODE_POSHOLD:
                        mode = "POSHOLD";
                        break;
                    case COPTER_MODE.COPTER_MODE_LAND:
                        mode = "LAND";
                        break;
                    case COPTER_MODE.COPTER_MODE_RTL:
                        mode = "RTL";
                        break;
                    case COPTER_MODE.COPTER_MODE_AUTO:
                        mode = "AUTO";
                        break;
                    case COPTER_MODE.COPTER_MODE_GUIDED:
                        mode = "GUIDED";
                        break;
                }

                //시동 여부 얻기
                boolean currArmed = ((msg.base_mode & MAV_MODE_FLAG.MAV_MODE_FLAG_SAFETY_ARMED) != 0) ? true : false;
                if (armed != currArmed) {
                    armed = currArmed;
                    List<ArmStatusLisener> copy = new ArrayList<>(armStatusLiseners);
                    for (ArmStatusLisener lisener : copy) {
                        lisener.statusChange(armed);
                    }
                }

            }
        });
    }

    //Roll, Pitch, Yaw 값 얻기
    public void receiveAttitude() {
        addMAVLinkMessageListener(
                msg_attitude.MAVLINK_MSG_ID_ATTITUDE,
                new MAVLInkMessageListener() {
                    @Override
                    public void receive(MAVLinkMessage mavLinkMessage) {
                        msg_attitude msg = (msg_attitude) mavLinkMessage;
                        roll = msg.roll * 180 / Math.PI;
                        pitch = msg.pitch * 180 / Math.PI;
                        yaw = msg.yaw * 180 / Math.PI;
                    }
                }
        );
    }

    //Alt 값 얻기
    public void receiveGlobalPositionInt() {
        addMAVLinkMessageListener(
                msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT,
                new MAVLInkMessageListener() {
                    @Override
                    public void receive(MAVLinkMessage mavLinkMessage) {
                        msg_global_position_int msg = (msg_global_position_int) mavLinkMessage;
                        currLat = msg.lat / 10000000.0;
                        currLng = msg.lon / 10000000.0;
                        alt = msg.relative_alt / 1000.0; //절대고도가 아닌 상대고도를 사용해야 함.
                        heading = msg.hdg / 100;

                    }
                });
    }

    //배터리 정보 얻기
    public void receiveSysStatus() {
        addMAVLinkMessageListener(
                msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS,
                new MAVLInkMessageListener() {
                    @Override
                    public void receive(MAVLinkMessage mavLinkMessage) {
                        msg_sys_status msg = (msg_sys_status) mavLinkMessage;
                        voltage_battery = ((int) (msg.voltage_battery / 1000 * 10)) / 10.0;
                        current_battery = ((int) (msg.current_battery / 100 * 10)) / 10.0;
                        battery_remaining = msg.battery_remaining;
                    }
                }
        );
    }

    //GPS 수신 여부 얻기
    public void receiveGpsRawInt() {
        addMAVLinkMessageListener(
                msg_gps_raw_int.MAVLINK_MSG_ID_GPS_RAW_INT,
                new MAVLInkMessageListener() {
                    @Override
                    public void receive(MAVLinkMessage mavLinkMessage) {
                        msg_gps_raw_int msg = (msg_gps_raw_int) mavLinkMessage;
                        gps_fix_type = msg.fix_type;

                    }
                }
        );
    }

    //속도 정보 얻기
    public void receiveVfrHud() {
        addMAVLinkMessageListener(
                msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD,
                new MAVLInkMessageListener() {
                    @Override
                    public void receive(MAVLinkMessage mavLinkMessage) {
                        msg_vfr_hud msg = (msg_vfr_hud) mavLinkMessage;
                        airspeed = ((int) (msg.airspeed * 10)) / 10.0;
                        groundspeed = ((int) (msg.groundspeed * 10)) / 10.0;
                    }
                }
        );
    }

    //홈 위치 정보 얻기
    public void receiveHomePosition() {
        addMAVLinkMessageListener(
                msg_home_position.MAVLINK_MSG_ID_HOME_POSITION,
                new MAVLInkMessageListener() {
                    @Override
                    public void receive(MAVLinkMessage mavLinkMessage) {
                        msg_home_position msg = (msg_home_position) mavLinkMessage;
                        homeLat = msg.latitude / 10000000.0;
                        homeLng = msg.longitude / 10000000.0;
                    }
                }
        );
    }
    public void receiveMissionRequst() {
        addMAVLinkMessageListener(
                msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST,
                new MAVLInkMessageListener() {
                    @Override
                    public void receive(MAVLinkMessage mavLinkMessage) {
                        try {
                            msg_mission_request msg = (msg_mission_request) mavLinkMessage;
                            int seq = msg.seq;
                            MAVLinkPacket packet = missionItems.get(seq).pack();
                            byte[] bytes = packet.encodePacket();
                            sendMessage(bytes);
                        } catch(Exception e) {
                            logger.error(e.toString());
                        }
                    }
                });
    }

    //드론에게 보내는 MAVLink ##############################################################
    //모든 데이터를 보내도록 명령 보내기
    public void sendRequestDataStream() {
        try {
            for (int i = 0; i < 2; i++) {
                msg_request_data_stream msg = new msg_request_data_stream();
                msg.target_system = (short) i;
                msg.target_component = (short) i;
                msg.req_message_rate = 4;
                msg.req_stream_id = MAV_DATA_STREAM.MAV_DATA_STREAM_ALL; //0을 넣자
                msg.start_stop = 1;
                MAVLinkPacket packet = msg.pack();
                byte[] bytes = packet.encodePacket();
                sendMessage(bytes);
            }
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    //시동걸기 및 시동끄기 명령 보내기
    public void sendCmdComponentArmDisarm(boolean value) {
        try {
            sendSetMode(COPTER_MODE.COPTER_MODE_GUIDED);

            msg_command_long msg = new msg_command_long();
            msg.target_system = 1;
            msg.target_component = MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1;
            msg.command = MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM;
            if (value) {
                msg.param1 = 1;
            } else {
                msg.param1 = 0;
            }
            MAVLinkPacket packet = msg.pack();
            byte[] bytes = packet.encodePacket();
            sendMessage(bytes);
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    //이륙 명령 보내기
    public void sendCmdMavTakeoff(float alt) {
        try {
            sendSetMode(COPTER_MODE.COPTER_MODE_GUIDED);
            msg_command_long msg = new msg_command_long();
            msg.target_system = 1;
            msg.target_component = MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1;
            msg.param7 = alt;
            msg.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF;
            MAVLinkPacket packet = msg.pack();
            byte[] bytes = packet.encodePacket();
            sendMessage(bytes);
        } catch (Exception ex) {
            logger.error(ex.toString());
        }
    }

    //비행 모드를 변경하는 명령 보내기
    public void sendSetMode(int mode) {
        try {
            msg_set_mode msg = new msg_set_mode();
            msg.target_system = 1;
            msg.base_mode = MAV_MODE_FLAG.MAV_MODE_FLAG_CUSTOM_MODE_ENABLED;
            msg.custom_mode = mode;
            MAVLinkPacket packet = msg.pack();
            byte[] bytes = packet.encodePacket();
            sendMessage(bytes);
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    //이륙 명령 보내기
    public void sendCmdGetHomePosition() {
        try {
            msg_command_long msg = new msg_command_long();
            msg.target_system = 1;
            msg.target_component = MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1;
            msg.command = MAV_CMD.MAV_CMD_GET_HOME_POSITION;
            MAVLinkPacket packet = msg.pack();
            byte[] bytes = packet.encodePacket();
            sendMessage(bytes);
        } catch (Exception ex) {
            logger.error(ex.toString());
        }
    }

    //목표 지점 바로가기 명령 보내기
    public void sendSetPositionTargetGlobalInt(double lat, double lng, double alt) {
        try {
            sendSetMode(COPTER_MODE.COPTER_MODE_GUIDED);
            msg_set_position_target_global_int msg = new msg_set_position_target_global_int();
            msg.target_system = 1;
            msg.target_component = MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1; //펌웨어로 명령을 내리겠다.
            msg.lat_int = (int) (lat * 10000000);
            msg.lon_int = (int) (lng * 10000000);
            msg.alt = (float) alt; //단위는 미터 그대로
            msg.coordinate_frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT_INT;
            msg.type_mask = 65528; //0아닌 것만, 0은 이미 0으로 디폴트 값.
            MAVLinkPacket packet = msg.pack();
            byte[] bytes = packet.encodePacket();
            sendMessage(bytes);
        } catch (Exception ex) {
            logger.error(ex.toString());
        }
    }

    //GCS의 HeartBeat를 보내기 (매 1초마다 보내기)
    public void sendHeartBeat() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                msg_heartbeat msg = new msg_heartbeat();
                msg.type = MAV_TYPE.MAV_TYPE_GCS;
                msg.autopilot = MAV_AUTOPILOT.MAV_AUTOPILOT_INVALID;
                msg.mavlink_version = 3;
                MAVLinkPacket packet = msg.pack();
                byte[] bytes = packet.encodePacket();
                while (Network.getUav() != null && Network.getUav().connected) {
                    try {
                        Network.getUav().sendMessage(bytes);
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
            }
        };
        thread.setName("HeartBeatThread");
        thread.setDaemon(true);
        thread.start();
    }

    //미션 정보를 드론에게 보내는 메시지
    public void sendMissionCount(Map<Integer, msg_mission_item_int> items) {
        try {
            missionItems = items;
            //일회성 메세지 성격상 이게 맞지만, 영구성이 더 쉬워 밑에꺼를 썼음.
            /*addMAVLinkMessageListener(msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST,
                    new MAVLInkMessageListener() {
                        @Override
                        public void receive(MAVLinkMessage mavLinkMessage) {
                            try {
                                msg_mission_request msg = (msg_mission_request) mavLinkMessage;
                                int seq = msg.seq;
                                logger.info("seq: " +seq);
                                MAVLinkPacket packet = items.get(seq).pack();
                                byte[] bytes = packet.encodePacket();
                                sendMessage(bytes);
                                if(seq == (items.size()-1)) {
                                    removeMAVLinkMessageListener(msg_mission_request.MAVLINK_MSG_ID_MISSION_REQUEST,
                                this);
                                }
                            } catch (Exception e) {
                                logger.error(e.toString());
                            }
                        }
                    });*/
            //영구성 메세지지
           msg_mission_count msg = new msg_mission_count();
            msg.target_system = 1;
            msg.target_component = MAV_COMPONENT.MAV_COMP_ID_AUTOPILOT1;
            msg.count = items.size();
            MAVLinkPacket packet = msg.pack();
            byte[] bytes = packet.encodePacket();
            sendMessage(bytes);
        } catch (Exception e) {
            logger.error(e.toString());
        }
    }
}