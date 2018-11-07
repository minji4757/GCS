package uav.gcs.main;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GcsMain extends Application {
	private static Logger logger = LoggerFactory.getLogger(GcsMain.class);
	public static GcsMain instance;
	public Stage primaryStage;
	public Scene scene;

	@Override
	public void start(Stage primaryStage) throws Exception {
		instance = this;
		this.primaryStage = primaryStage;
		//primaryStage's white background not visible
		this.primaryStage.setOpacity(0.0);

		BorderPane root = FXMLLoader.load(GcsMainController.class.getResource("GcsMain.fxml"));
		scene = new Scene(root);
		scene.getStylesheets().add(GcsMainController.class.getResource("style_dark.css").toExternalForm());
		
		primaryStage.setTitle("UAV Ground Control Station");
		primaryStage.setScene(scene);

		primaryStage.setMaximized(true);
		
		primaryStage.show();
		//primaryStage's background visible
		this.primaryStage.setOpacity(1.0);
	}
	
	@Override
	public void stop() {
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}


