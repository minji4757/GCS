package uav.gcs.main;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.common.msg_command_long;
import com.MAVLink.enums.COPTER_MODE;
import com.MAVLink.enums.MAV_CMD;
import com.MAVLink.enums.MAV_COMPONENT;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uav.gcs.hud.HudController;
import uav.gcs.mission.MissionController;
import uav.gcs.network.Network;
import uav.gcs.network.NetworkController;
import uav.gcs.network.Uav;

import java.net.URL;
import java.util.ResourceBundle;

public class GcsMainController implements Initializable {
	private static Logger logger = LoggerFactory.getLogger(GcsMainController.class);
	public static GcsMainController instance;

	@FXML public Button btnConnect;
	@FXML private StackPane hudPane;
	@FXML private StackPane centerPane;
	@FXML private Button btnArm;
	@FXML private TextField txtTakeoffHeight;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		GcsMainController.instance = this;
		Uav.addConnectionListener(new Uav.ConnectionListener() {
			@Override
			public void connect(Uav uav) {
				Platform.runLater(()->{
					btnConnect.setText("연결끊기");
				});
				Thread thread = new Thread() {
					@Override
					public void run() {
						while(uav.connected) {
							HudController.instance.changeStatus();
							GcsMainController.instance.changeStatus();
							MissionController.instance.changeStatus();
							try { Thread.sleep(200); } catch(Exception e) {}
						}
					}
				};
				thread.setDaemon(true);
				thread.start();
			}
			@Override
			public void disconnect(Uav uav) {
				Platform.runLater(()->{
					btnConnect.setText("연결하기");
				});
			}
		});
		initHud();
		initMission();
	}

	public void initHud() {
		try {
			hudPane.getChildren().add(FXMLLoader.load(HudController.class.getResource("Hud.fxml")));
		} catch(Exception e) {
			logger.error(e.toString());
		}
	}

	public void initMission() {
		try {
			centerPane.getChildren().add(FXMLLoader.load(MissionController.class.getResource("Mission.fxml")));
		} catch(Exception e) {
			logger.error(e.toString());
		}
	}

	public void handleBtnConnectConfig() {
		try {
			Stage dialog = new Stage();
			dialog.setTitle("Network Configuration");
			dialog.initModality(Modality.APPLICATION_MODAL);
			dialog.initOwner(GcsMain.instance.primaryStage);
			AnchorPane anchorPane = FXMLLoader.load(NetworkController.class.getResource("Network.fxml"));
			Scene scene = new Scene(anchorPane);
			scene.getStylesheets().add(GcsMainController.class.getResource("style_dark_dialog.css").toExternalForm());
			dialog.setScene(scene);
			dialog.setResizable(false);
			dialog.show();
		} catch(Exception e) {
			logger.error(e.toString());
		}
	}

	public void handleBtnConnect(ActionEvent event) {
		if(btnConnect.getText().equals("연결하기")) {
			Thread thread = new Thread() {
				@Override
				public void run() {
					Uav uav = Network.createUav();
					uav.connect();
				}
			};
			thread.setDaemon(true);
			thread.start();
		} else {
			Network.distroyUav();
		}
	}

	private void changeStatus() {
		Platform.runLater(()->{
			Uav uav = Network.getUav();
			if(uav.connected) {
				if(uav.armed) {
					btnArm.setText("시동끄기");
				} else {
					btnArm.setText("시동걸기");
				}
			}
		});
	}

	public void handleBtnArm(ActionEvent e) {
		Uav uav = Network.getUav();
		if(uav != null && uav.connected) {
			if (btnArm.getText().equals("시동걸기")) {
				uav.sendCmdComponentArmDisarm(true);
			} else {
				uav.sendCmdComponentArmDisarm(false);
			}
		}
	}

	public void handleBtnTakeoff(ActionEvent e) {
		Uav uav = Network.getUav();
		uav.sendCmdMavTakeoff(Float.parseFloat(txtTakeoffHeight.getText()));
	}

	public void handleBtnLand(ActionEvent e)  {
		Uav uav = Network.getUav();
		uav.sendSetMode(COPTER_MODE.COPTER_MODE_LAND);
	}
	
	public void handleBtnRtl(ActionEvent e) {
		Uav uav = Network.getUav();
		uav.sendSetMode(COPTER_MODE.COPTER_MODE_RTL);
	}
}
