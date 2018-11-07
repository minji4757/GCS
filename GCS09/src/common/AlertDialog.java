package common;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertDialog {
    private static Logger logger = LoggerFactory.getLogger(AlertDialog.class);

    public static Alert showOkButton(String title, String contentText) {
        Alert alert = new Alert(Alert.AlertType.NONE, contentText, new ButtonType("닫기", ButtonBar.ButtonData.OK_DONE)); //None으로 지정하면 버튼을 우리가 지정할 수 있다. 버튼타입도 새로만들어야함.(...하면 가변매개변수)
        alert.setTitle(title);
        alert.show();
        return alert;

    }
    public static Alert showNoButton(String title, String contentText) { //잠깐 보여줬다가 사라지는 버튼
        Alert alert = new Alert(Alert.AlertType.NONE, contentText);
        alert.setTitle(title);
        //프로그램에서 close() 메소드를 호출할 때 자동으로 다이얼로그가 닫히도록 설정
        alert.setResult(ButtonType.OK); //이걸 넣어야만 다이얼로그가 닫힌다.
        alert.show();
        return alert;
    }
}
