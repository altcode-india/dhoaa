/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ani.dhoaa;

import com.github.axet.vget.VGet;
import com.github.axet.vget.info.VideoFileInfo;
import com.github.axet.vget.info.VideoInfo;
import com.google.api.services.youtube.model.SearchResult;
import com.sun.javafx.collections.ObservableListWrapper;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 *
 * @author ani
 */
public class HomeController implements Initializable {

    @FXML
    private ToggleButton youtubeToggle;
    @FXML
    private AnchorPane root;
    @FXML
    private Button exit, minimize;
    @FXML
    private TextField searchbox;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ListView searchview;
    @FXML
    private ListView playlist;
    @FXML
    private ImageView audioIcon;
    @FXML
    private ImageView nextButton;
    @FXML
    private Label title,infoBar;
    @FXML
    private ImageView playIcon;
    private final Image play = new Image(HomeController.class.getResourceAsStream("/icons/play.png")), pause = new Image(HomeController.class.getResourceAsStream("/icons/pause.png")),
            music = new Image(HomeController.class.getResourceAsStream("/icons/music.png")), next = new Image(HomeController.class.getResourceAsStream("/icons/next.png"));
    @FXML
    private Slider slider;
    private boolean youtubeOn = false;
    private final YoutubeEngine youtubeEngine = new YoutubeEngine();

    @FXML
    private void handleButtonAction(ActionEvent event) {
        System.out.println(((Control) event.getSource()).getId() + ": triggered!");
        if (event.getSource().equals(exit)) {
            System.exit(0);
        }
        if (event.getSource().equals(minimize)) {
            stage.setIconified(true);
        }
        if (event.getSource().equals(youtubeToggle)) {
            youtubeOn = youtubeToggle.isSelected();
            Platform.runLater(() -> {
                if (youtubeOn) {
                    youtubeToggle.setText("Online");
                } else {
                    youtubeToggle.setText("Offline");
                }
            });
        }
    }
    
    private final ArrayList<AudioEngine.AudioElement> searchlist = new ArrayList<>();
    private int searchCallIndex = 0;

    @FXML
    private void searchEvent(ActionEvent e) {
        searchCallIndex++;
        progressIndicator.setVisible(true);
        new Thread(() -> {
            int index = searchCallIndex;
            if (youtubeOn) {
                searchlist.clear();
                try {
                    final ArrayList<AudioEngine.AudioElement> searchlistRef = searchlist;
                    List<SearchResult> list = youtubeEngine.search(searchbox.getText());
                    for (SearchResult res : list) {
                        try {
                            synchronized (searchlistRef) {
                                if (index == searchCallIndex) {
                                    VGet vget = new VGet(new URL("https://www.youtube.com/watch?v=" + res.getId().getVideoId()));
                                    vget.extract();
                                    VideoInfo video = vget.getVideo();
                                    video.getInfo().stream().filter((vfi) -> (vfi.getContentType().startsWith("audio"))).forEach((VideoFileInfo vfi) -> {
                                        URL source = vfi.getSource();
                                        String title1 = video.getTitle();
                                        String id = res.getId().getVideoId();
                                        URL icon = video.getIcon();
                                        searchlistRef.add(new AudioEngine.AudioElement(source, title1, icon, id));
                                    });
                                    searchview.setItems(new ObservableListWrapper(searchlistRef));
                                } else {
                                    System.out.println("Search Stopped: " + index);
                                    searchlistRef.clear();
                                    break;
                                }
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                } catch (IOException ex) {
                    Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else{
                searchlist.clear();
                if(!scanOn){
                    List<AudioEngine.AudioElement> list=fileEngine.fromDB(searchbox.getText());
                    searchlist.addAll(list);
                    searchview.setItems(new ObservableListWrapper(searchlist));
                }
                else{
                    searchview.setItems(new ObservableListWrapper(Arrays.asList(new AudioEngine.AudioElement(null,"Background scanning is ",null,1,null,1),new AudioEngine.AudioElement(null,"in progress... ",null,1,null,1),new AudioEngine.AudioElement(null,"Please go Online ",null,1,null,1),new AudioEngine.AudioElement(null,"for now! ",null,1,null,1))));
                }
                
            }
            if (index == searchCallIndex) {
                progressIndicator.setVisible(false);
            }
        }).start();

    }

    private AudioEngine audioEngine;
    private Timer sliderTimer;

    @FXML
    private void searchboxMouseClick(MouseEvent e) {
        System.out.println("MouseEvent searchbox: " + e.getClickCount() + " " + searchview.getSelectionModel().getSelectedItem());
        if (e.getClickCount() == 2) {
            if (audioEngine == null) {
                audioEngine = new AudioEngine(this, (st, audio) -> {
                    state = st;
                    System.out.println("State:" + state);
                    if (state == AudioEngine.BUFFERING) {
                            Platform.runLater(() -> {
                                title.setText(audio.getTitle());
                            }// ...
                            );
                        showInfo("Buffering...");
                    }
                    if (state == AudioEngine.PLAYING) {
                        seeking = false;
                        try {
                            hideInfo();
                            Platform.runLater(() -> {
                                title.setText(audio.getTitle());
                            }// ...
                            );
                            audioIcon.setImage(new Image(audio.getIcon().openStream()));
                            audioIcon.setPreserveRatio((audio.getIconType() != 0));
                            playIcon.setImage(pause);
                            nextButton.setImage(next);
                            slider.setVisible(true);
                            slider.setMax(audioEngine.getLength());

                            if (sliderTimer != null) {
                                sliderTimer.cancel();
                                sliderTimer = null;
                            }
                            sliderTimer = new Timer();
                            sliderTimer.scheduleAtFixedRate(new TimerTask() {
                                @Override
                                public void run() {
                                    if (state == AudioEngine.UNLOADED) {
                                        next();
                                        if (sliderTimer != null) {
                                            sliderTimer.cancel();
                                            sliderTimer = null;
                                        }
                                    } else if (!seeking) {
                                        slider.setValue(audioEngine.getTime()); //To change body of generated methods, choose Tools | Templates.
                                    }
                                }
                            }, 0, 1000);
                        } catch (IOException ex) {
                            Logger.getLogger(HomeController.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    if (state == AudioEngine.PAUSED) {
                        playIcon.setImage(play);
                        if (sliderTimer != null) {
                            sliderTimer.cancel();
                            sliderTimer = null;
                        }
                    }
                    if (state == AudioEngine.UNLOADED) {
                        audioIcon.setImage(music);
                        audioIcon.setPreserveRatio(true);
                        playIcon.setImage(null);
                        nextButton.setImage(null);

                        slider.setVisible(false);
                    }

                });
            }
            AudioEngine.AudioElement audio = (AudioEngine.AudioElement) searchview.getSelectionModel().getSelectedItem();
            if (audio != null ) {
                    if(!(audio.getType()==1&&scanOn))audioEngine.play(audio);
            }
        }
    }

    private Dhoaa app;
    private Stage stage;
    private final Point drgSet = new Point();
    private ArrayList<AudioEngine.AudioElement> queue;
    private boolean scanOn=false;
    void setApplication(Dhoaa app, Stage stage) {
        this.app = app;
        this.stage = stage;
        stage.getIcons().add(music);
    }
    FileEngine fileEngine;
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        fileEngine=new FileEngine();
        scanOn=true;
        showInfo("Background scanning of harddisk in progress...");
        fileEngine.startScan(()->{
            scanOn=false;
            hideInfo();
        },false).start();
        audioIcon.setImage(music);
        root.setOnMousePressed((MouseEvent e) -> {
            drgSet.x = e.getSceneX();
            drgSet.y = e.getSceneY();
        });
        root.setOnMouseDragged((MouseEvent e) -> {

            stage.setX(stage.getX() + (e.getSceneX() - drgSet.x));
            stage.setY(stage.getY() + (e.getSceneY() - drgSet.y));
        });
        queue = new ArrayList<>();
        searchview.setOnDragDetected((MouseEvent event) -> {
            int x = searchview.getSelectionModel().getSelectedIndex();
            if (x != -1) {
                /* drag was detected, start a drag-and-drop gesture*/
 /* allow any transfer mode */
                Dragboard db = searchview.startDragAndDrop(TransferMode.COPY);

                /* Put a string on a dragboard */
                ClipboardContent content = new ClipboardContent();
                content.putString(x + "");
                db.setContent(content);

                event.consume();
            }
        });
        playlist.setOnDragOver((DragEvent event) -> {
            /* data is dragged over the target */
 /* accept it only if it is not dragged from the same node
            * and if it has a string data */
            if (event.getGestureSource() != playlist
                    && event.getDragboard().hasString()) {
                /* allow for both copying and moving, whatever user chooses */
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }

            event.consume();
        });
        playlist.setOnDragEntered((DragEvent event) -> {
            /* the drag-and-drop gesture entered the target */
 /* show to the user that it is an actual gesture target */
            if (event.getGestureSource() != playlist
                    && event.getDragboard().hasString()) {
                playlist.setStyle("-fx-text-fill:#fff;");
            }

            event.consume();
        });
        playlist.setOnDragExited((DragEvent event) -> {
            /* mouse moved away, remove the graphical cues */
            if (event.getGestureSource() != playlist
                    && event.getDragboard().hasString()) {
                playlist.setStyle("-fx-text-fill:#ff6666;");
            }

            event.consume();
        });
        playlist.setOnDragDropped((DragEvent event) -> {
            /* data dropped */
 /* if there is a string data on dragboard, read it and use it */
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {

                AudioEngine.AudioElement audio = searchlist.get(Integer.parseInt(db.getString()));
                System.out.println(audio.getSource());
                queue.add(audio);
                playlist.setItems(new ObservableListWrapper(queue));
                success = true;

            }
            /* let the source know whether the string was successfully
            * transferred and used */
            event.setDropCompleted(success);

            event.consume();
        });
    }
    private int state;

    @FXML
    private void playButtonHandle(MouseEvent e) {
        System.out.println(state);
        if (state == AudioEngine.PLAYING || state == AudioEngine.PAUSED) {
            audioEngine.togglePause();
            System.out.println(state);
        }

    }

    @FXML
    private void nextButtonHandle(MouseEvent e) {
        System.out.println(state);
        if (!queue.isEmpty()) {
            next();
        }
    }

    private void next() {

        System.out.println(state);
        AudioEngine.AudioElement audio2 = queue.get(0);
        audioEngine.play(audio2);
        queue.remove(audio2);
        playlist.setItems(new ObservableListWrapper(queue));
    }

    @FXML
    private void playlistHandle(MouseEvent e) {
        if (e.getClickCount() == 2 && !queue.isEmpty()) {
            AudioEngine.AudioElement audio2 = (AudioEngine.AudioElement) playlist.getSelectionModel().getSelectedItem();
            audioEngine.play(audio2);
            queue.remove(audio2);
            playlist.setItems(new ObservableListWrapper(queue));
        }
    }
    boolean seeking = false;

    @FXML
    private void sliderHandle(MouseEvent e) {
        System.out.println(state);
        if (state == AudioEngine.PLAYING || state == AudioEngine.BUFFERING || state == AudioEngine.PAUSED) {
            seeking = true;
            double newtime = slider.getValue();
            audioEngine.seek((long) (newtime - audioEngine.getTime()));
        }

    }
    private void showInfo(String s){
        Platform.runLater(()->{
            infoBar.setText(s);
            infoBar.setVisible(true);
        });
    }
    private void hideInfo(){
        Platform.runLater(()->{
            infoBar.setVisible(false);
        });
    }
    private class Point {

        public double x = 0, y = 0;
    }
}
