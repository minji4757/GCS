package uav.gcs.hud;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import uav.gcs.main.GcsMainController;
import uav.gcs.network.Network;
import uav.gcs.network.Uav;

import java.net.URL;
import java.util.ResourceBundle;

public class HudController implements Initializable, Hud {
    public static HudController instance;

    //도화지 객체를 저장할 필드
    @FXML private Canvas canvas1;
    @FXML private Canvas canvas2;
    //붓 객체를 저장할 필드
    private GraphicsContext ctx1;
    private GraphicsContext ctx2;
    //HUD의 전체 폭과 높이
    private double width;
    private double height;
    //원점 이동 좌표
    private double translateX;
    private double translateY;
    private double roll;
    private double tempRoll;
    private double pitch;
    private double tempPitch;
    private double yaw;
    private double tempYaw;
    private double pitchDistance;
    private double yawDistance;
    //배경 그라디언트
    private LinearGradient skyLinearGradient;
    private LinearGradient groundLinearGradient;
    //Text View를 위한 필드
    @FXML private AnchorPane anchorPane;
    @FXML private Label lblSystemStatus;
    @FXML private Label lblAltitude;
    @FXML private Label lblAirSpeed;
    @FXML private Label lblGroundSpeed;
    @FXML private Label lblArmed;
    @FXML private Label lblMode;
    @FXML private Label lblBattery;
    @FXML private Label lblGpsFixed;
    @FXML private Label lblInfo;
    private Thread infoThread;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HudController.instance = this;

        initCanvasLayer1();
        initCanvasLayer2();

        ViewLoop viewLoop = new ViewLoop();
        viewLoop.start();

        Uav.addArmStatusLisener(new Uav.ArmStatusLisener() {
            @Override
            public void statusChange(boolean armed) {
                if (armed) {
                    setInfo("ARMED");
                } else {
                    setInfo("DISARMED");
                }
            }
        });
    }

    private void initCanvasLayer1() {
        //도화지에 그림을 그리는 붓 초기화
        ctx1 = canvas1.getGraphicsContext2D();
        //HUD의 폭과 높이를 초기화
        width = canvas1.getWidth();
        height = canvas1.getHeight();
        //배경 그라디언트 초기화
        skyLinearGradient = new LinearGradient(
                0, 0, 0, 1,         //수직 아래 방향으로 색상 변환
                true,                           //0~1을 전체 길이로 비율적으로 계산
                CycleMethod.NO_CYCLE,                       //색상 변환를 반복하지 마라
                new Stop(0.3, Color.rgb(0x00, 0x00, 0xFF)),  //위쪽 색상을 중간까지만 변환
                new Stop(1.0, Color.rgb(0x87, 0xCE, 0xFA))            //아래쪽 색상을 끝까지 변환
        );
        groundLinearGradient = new LinearGradient(
                0, 0, 0, 1,
                true,
                CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.rgb(0xBC, 0xE5, 0x5C)),
                new Stop(0.5, Color.rgb(0x66, 0x4B, 0x00))
        );
        //피치 눈금 단위 길이
        pitchDistance = height/2/45;  //45도를 최대 pitch 값으로 설정
    }

    private void layer1Draw() {
        //원점 회전 복귀
        if(tempRoll != 0) {
            ctx1.rotate(-tempRoll);
        }

        //원점 이동 복귀
        if(translateX != 0) {
            ctx1.translate(-translateX, -translateY);
        }

        //뷰 지우기
        ctx1.clearRect(0, 0, width, height);

        //원점 이동
        tempPitch = pitch;
        translateX = width/2;
        translateY = height/2 + pitchDistance*tempPitch;
        ctx1.translate(translateX, translateY);

        //원점 회전
        tempRoll = roll;
        ctx1.rotate(tempRoll);

        //SKY 배경을 드로잉
        ctx1.setFill(skyLinearGradient);
        ctx1.fillRect(-500, -500, 1000, 500);

        //Ground 배경을 드로잉
        ctx1.setFill(groundLinearGradient);
        ctx1.fillRect(-500, 0, 1000, 500);

        //pitch 0 선 그리기
        ctx1.setStroke(Color.GREEN);
        ctx1.setLineWidth(1.5);
        ctx1.strokeLine(-50, 0, 50, 0);
        ctx1.setTextBaseline(VPos.CENTER);
        ctx1.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        ctx1.setFill(Color.WHITE);
        ctx1.fillText("0", -70, 0);

        //pitch SKY 선 그리기
        ctx1.setStroke(Color.WHITE);
        ctx1.setFill(Color.WHITE);
        ctx1.setTextBaseline(VPos.CENTER);
        ctx1.setFont(Font.font("Arial", FontWeight.BOLD, 15));
        for(int i=5; i<=(25+tempPitch); i+=5) {
            ctx1.strokeLine((i%2==0)?-50:-25, -(pitchDistance*i), (i%2==0)?50:25, -(pitchDistance*i));
            if(i%2==0) {
                ctx1.fillText(String.valueOf(i), -80, -(pitchDistance*i));
            }
        }
        //pitch Ground 선 그리기
        for(int i=5; i<=(25-tempPitch); i+=5) {
            ctx1.strokeLine((i%2==0)?-50:-25, (pitchDistance*i), (i%2==0)?50:25, (pitchDistance*i));
            if(i%2==0) {
                ctx1.fillText(String.valueOf(-i), -80, (pitchDistance*i));
            }
        }
    }

    private void initCanvasLayer2() {
        ctx2 = canvas2.getGraphicsContext2D();
        yawDistance = width/2/60;
    }

    private void layer2Draw() {
        //뷰 지우기
        ctx2.clearRect(0, 0, width, height);

        //원점 이동
        ctx2.translate(width/2, height/2);

        //UAV 선 그리기
        ctx2.setStroke(Color.RED);
        ctx2.setLineWidth(2);
        ctx2.strokeLine(0,0,-50,20);
        ctx2.strokeLine(0,0,50,20);
        ctx2.strokeLine(-80,0,-120,0);
        ctx2.strokeLine(80,0,120,0);

        //UAV 호 그리기
        ctx2.setStroke(Color.BLACK);
        ctx2.setLineWidth(1);
        ctx2.setLineDashes(1, 5);
        ctx2.strokeArc(-120, -120, 240, 240, 0, 180, ArcType.OPEN);

        //Yaw 수평선 그리기
        ctx2.setStroke(Color.WHITE);
        ctx2.setLineDashes(0, 0);
        ctx2.strokeLine(-175, -130, 175, -130);
        //Yaw 중앙 눈금 그리기
        ctx2.setStroke(Color.RED);
        ctx2.setLineWidth(3);
        ctx2.strokeLine(0, -130, 0, -135);
        //Yaw 눈금 그리기
        ctx2.setLineWidth(1);
        ctx2.setStroke(Color.WHITE);
        for(int i=5; i<=60; i+=5) {
            ctx2.strokeLine((yawDistance*i), -130, (yawDistance*i), -135);
        }
        for(int i=5; i<=60; i+=5) {
            ctx2.strokeLine(-(yawDistance*i), -130, -(yawDistance*i), -135);
        }
        //숫자 그리기
        tempYaw = yaw;
        ctx2.setFill(Color.RED);
        ctx2.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        ctx2.setTextAlign(TextAlignment.CENTER);
        ctx2.fillText(String.valueOf((int)tempYaw), 0, -140);

        ctx2.setFill(Color.WHITE);
        ctx2.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        ctx2.setTextAlign(TextAlignment.CENTER);
        for(int i=15; i<=60; i+=15) {
            double value = tempYaw+i;
            if(value >= 360) {
                value -= 360;
            }
            ctx2.fillText(String.valueOf((int)value), (yawDistance*i), -140);
        }
        for(int i=15; i<=60; i+=15) {
            double value = tempYaw-i;
            if(value<0) {
                value += 360;
            }
            ctx2.fillText(String.valueOf((int)value), -(yawDistance*i), -140);
        }

        //원점 이동 복귀
        ctx2.translate(-width/2, -height/2);
    }

    private class ViewLoop extends AnimationTimer {
        @Override
        public void handle(long now) {
            layer1Draw();
            layer2Draw();
        }
    }

    @Override
    public void setRoll(double roll) {
        this.roll = roll;
    }

    @Override
    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    @Override
    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    @Override
    public void setSystemStatus(String systemStatus) {
        Platform.runLater(()->{
            this.lblSystemStatus.setText(systemStatus);
        });
    }

    @Override
    public void setArmed(boolean armed) {
        Platform.runLater(()->{
            if(armed) {
                this.lblArmed.setText("ARMED");
            } else {
                this.lblArmed.setText("DISARMED");
            }
        });
    }

    @Override
    public void setAltitude(double altitude) {
        Platform.runLater(()->{
            this.lblAltitude.setText(altitude + " m");
        });
    }

    @Override
    public void setBattery(double voltage, double current, double level) {
        Platform.runLater(()->{
            this.lblBattery.setText("BAT: " + voltage + "V " + current + "A " + level + "%");
        });
    }

    @Override
    public void setGpsFixed(boolean gpsFixed) {
        Platform.runLater(()->{
            if(gpsFixed) {
                this.lblGpsFixed.setText("GPS Fixed");
            } else {
                this.lblGpsFixed.setText("");
            }
        });
    }

    @Override
    public void setMode(String mode) {
        Platform.runLater(()->{
            this.lblMode.setText(mode);
        });
    }

    @Override
    public void setAirSpeed(double airSpeed) {
        Platform.runLater(()->{
            this.lblAirSpeed.setText("AS: " + airSpeed + "m/s");
        });
    }

    @Override
    public void setGroudSpeed(double groundSpeed) {
        Platform.runLater(()->{
            this.lblGroundSpeed.setText("GS: " + groundSpeed + "m/s");
        });
    }

    @Override
    public void setInfo(String info) { //3초 보였다가 사라지는거
        if(infoThread != null) {
            infoThread.interrupt();
        }
        infoThread = new Thread() {
            @Override
            public void run() {
                try {
                    Platform.runLater(()->{
                        HudController.this.lblInfo.setText(info);
                    });
                    Thread.sleep(3000);
                    Platform.runLater(()->{
                        HudController.this.lblInfo.setText("");
                    });
                } catch(Exception e) {}
            }
        };
        infoThread.setDaemon(true);
        infoThread.start();
    }

    public void changeStatus() {
        Uav uav = Network.getUav();
        setArmed(uav.armed);
        setSystemStatus(uav.systemStatus);
        setMode(uav.mode);
        setRoll(-uav.roll);
        setPitch(uav.pitch);
        setYaw(uav.heading);
        setAltitude((int)(uav.alt*10) / 10.0);
        setBattery(uav.voltage_battery, uav.current_battery, uav.battery_remaining);
        if(uav.gps_fix_type == 6) {
            setGpsFixed(true);
        } else {
            setGpsFixed(false);
        }
        setAirSpeed(uav.airspeed);
        setGroudSpeed(uav.groundspeed);
    }

    @Override
    public void setTime(String time) {
    }
}
