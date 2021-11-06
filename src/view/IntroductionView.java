package view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import util.FATUtil;

public class IntroductionView {
    public IntroductionView(){
        initIntro();
    }
    public void initIntro(){
        try{
            Stage newStage=new Stage();
            FXMLLoader loader=new FXMLLoader();
            loader.setLocation(getClass().getResource("/ui/introduction.fxml"));
            Parent root =(Parent) loader.load();
            Scene scene=new Scene(root);
            newStage.getIcons().add(new Image(FATUtil.INTRODUCTION_IMG));
            newStage.setScene(scene);
            newStage.setTitle("作者简介");
            newStage.show();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
