import GameState.Start;
import javafx.application.Application;
import javafx.stage.Stage;

/*
 * Created by: Matt Zafiriou
 * Github: CaptainWolfie
 * Start Date: 28/8/2019
 */
public class Launcher extends Application {
	
	public static void main(String[] args) {
		new Start("Game Launcher", 900, 700);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
	}
}