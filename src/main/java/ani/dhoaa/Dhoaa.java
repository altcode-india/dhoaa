/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ani.dhoaa;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author ani
 */
public class Dhoaa extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader =new FXMLLoader(getClass().getResource("/fxml/Home.fxml"));
        Parent root=loader.load();
        HomeController homeController=loader.getController();
        homeController.setApplication(this,stage);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Dhoaa");
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        stage.show();
        homeController.setApplication(this,stage);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
