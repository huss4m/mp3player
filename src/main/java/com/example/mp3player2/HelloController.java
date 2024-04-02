package com.example.mp3player2;


import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;


import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

public class HelloController implements Initializable {


    /* todo: next time
    * changer progressbar into slider
    * add label with duration  - GOOD
    * playlist scene
    * choosing paths for scene
    * saving preferences (default volume, playlist, replay etc) after closing app
    * add mute button - GOOD
    * */


    @FXML
    private Pane anchorPane;

    @FXML
    private Label songLabel;

    @FXML
    private Button playButton, pauseButton, resetButton, nextButton, previousButton, toggleLoopButton;

    @FXML
    private ComboBox<String> speedComboBox;

    @FXML
    private Slider volumeSlider;


    @FXML
    private Slider progressSlider;



    @FXML
    private Label durationLabel;

    @FXML
    private Button muteButton;

    private Media media;
    private MediaPlayer mediaPlayer;
    private File directory;

    private File[] files;
    private ArrayList<File> playlist;
    private int songNumber;
    private boolean isMuted;
    private int[] speeds = {25, 50, 75, 100, 125, 150, 175, 200};
    private Timer timer;
    private TimerTask task;
    private boolean running;
    private boolean toggleLoop;

    private DoubleProperty currentTime = new SimpleDoubleProperty(0);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        playlist = new ArrayList<File>();
        directory = new File("src/main/java/com/example/mp3player2/music");
        toggleLoop = true;
        isMuted = false;

        progressSlider.setMin(0); // Position minimale
        progressSlider.setMax(100); // Position maximale (pourcentage)
        progressSlider.setValue(0); // Position initiale





        files = directory.listFiles();

        if(files != null) {
            for(File file : files) {
                playlist.add(file);
            }
        }


        media = new Media(playlist.get(songNumber).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        songLabel.setText(playlist.get(songNumber).getName());


        for(int i = 0; i < speeds.length; i++) {
            speedComboBox.getItems().add(Integer.toString(speeds[i])+"%");
        }

        speedComboBox.setOnAction(this::changeSpeed);

        volumeSlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {

                if(!isMuted) {
                    mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);
                }
            }
        });

        // Mise à jour de durationLabel avec la durée totale du premier média
        mediaPlayer.setOnReady(() -> {
            Duration totalTime = media.getDuration();
            durationLabel.setText(formatDuration(mediaPlayer.getCurrentTime())+" / "+formatDuration(totalTime));
        });


        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            if (progressSlider.isValueChanging()) {
                currentTime.set(newValue.toSeconds());
            }
        });

        // Lier la position du slider avec la propriété observable currentTime
        progressSlider.valueProperty().bindBidirectional(currentTime);

        // Détecter le début de l'interaction utilisateur avec le slider
        progressSlider.setOnMousePressed(event -> {
            double mouseX = event.getX(); // Position X du clic de la souris
            double width = progressSlider.getWidth(); // Largeur totale du slider

            // Calculer la nouvelle position en pourcentage en fonction de la position du clic de la souris
            double newPosition = (mouseX / width) * 100.0;

            // Mettre à jour la position du slider
            progressSlider.setValue(newPosition);

            // Mettre à jour la position de lecture du média
            mediaPlayer.seek(Duration.seconds(newPosition * mediaPlayer.getTotalDuration().toSeconds() / 100.0));

            // Mettre en pause la lecture pendant que l'utilisateur interagit avec le slider
            //mediaPlayer.pause();
        });


        // bugs ici
        progressSlider.setOnMouseDragged(event -> {
            mediaPlayer.pause();
            double mouseX = event.getX(); // Position X du curseur de la souris
            double width = progressSlider.getWidth(); // Largeur totale du slider

            // Calculer la nouvelle position en pourcentage en fonction de la position du curseur de la souris
            double newPosition = (mouseX / width) * 100.0;

            // Mettre à jour la position de lecture du média
            mediaPlayer.seek(Duration.seconds(newPosition * mediaPlayer.getTotalDuration().toSeconds() / 100.0));

        });


    }


    public void playMedia() {
        beginTimer();
        changeSpeed(null);
        mediaPlayer.setVolume(volumeSlider.getValue()*0.01);
        mediaPlayer.play();
        if(progressSlider.getValue() == 100) {
            resetMedia();
        }
    }

    public void pauseMedia() {
        cancelTimer();
        mediaPlayer.pause();
    }

    public void resetMedia() {
        progressSlider.setValue(0);
        mediaPlayer.seek(Duration.seconds(0));
    }

    public void previousMedia() {
        if(songNumber > 0) {
            songNumber--;
            mediaPlayer.stop();


            if(running) { cancelTimer(); }


            media = new Media(playlist.get(songNumber).toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            songLabel.setText(playlist.get(songNumber).getName());

            playMedia();
        }
        else {
            songNumber=playlist.size()-1;
            mediaPlayer.stop();


            if(running) { cancelTimer(); }


            media = new Media(playlist.get(songNumber).toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            songLabel.setText(playlist.get(songNumber).getName());

            playMedia();
        }
    }
    public void toggleLoop() {
        if (!toggleLoop) {
            toggleLoop = true;
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // Jouer en boucle indéfiniment
            System.out.println("Loop activé");
        } else {
            toggleLoop = false;
            mediaPlayer.setCycleCount(1); // Jouer une seule fois
            System.out.println("Loop désactivé");
        }
    }
    public void nextMedia() {
        if(songNumber < playlist.size()-1) {
            songNumber++;
            mediaPlayer.stop();

            if(running) { cancelTimer(); }

            media = new Media(playlist.get(songNumber).toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            songLabel.setText(playlist.get(songNumber).getName());

            playMedia();
        }
        else {
            songNumber=0;
            mediaPlayer.stop();

            if(running) { cancelTimer(); }

            media = new Media(playlist.get(songNumber).toURI().toString());
            mediaPlayer = new MediaPlayer(media);

            songLabel.setText(playlist.get(songNumber).getName());

            playMedia();
        }
    }

    public void changeSpeed(ActionEvent event) {

        if(speedComboBox.getValue() == null) {
            mediaPlayer.setRate(1);
        }
        else {
            mediaPlayer.setRate(Integer.parseInt(speedComboBox.getValue().substring(0, speedComboBox.getValue().length() - 1)) * 0.01);
        }
    }

    public void beginTimer() {
        timer = new Timer();
        task = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> updateProgressSliderAndDurationLabel());
            }
        };
        timer.scheduleAtFixedRate(task, 0, 10);
    }


    public void cancelTimer() {
        running = false;
        timer.cancel();
    }
    private void updateProgressSliderAndDurationLabel() {
        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            double current = mediaPlayer.getCurrentTime().toSeconds();
            double end = media.getDuration().toSeconds();

            //songProgressBar.setProgress(current / end);
            Duration currentTime = mediaPlayer.getCurrentTime();
            Duration totalTime = media.getDuration();
            String currentTimeString = formatDuration(currentTime);
            String totalTimeString = formatDuration(totalTime);
            durationLabel.setText(currentTimeString + " / " + totalTimeString);
            updateSliderPosition();
            if (current / end == 1) {
                cancelTimer();
            }
        }

    }

    private String formatDuration(Duration duration) {
        int minutes = (int) duration.toMinutes();
        int seconds = (int) (duration.toSeconds() % 60);
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void handleMuteButtonAction() {
        if(isMuted == true) {
            isMuted = false;

            mediaPlayer.setVolume(volumeSlider.getValue() * 0.01);


            System.out.println("Unmuting");
        }
        else {
            isMuted = true;
            mediaPlayer.setVolume(0);
            System.out.println("Muting");
        }
    }


    public void updateSliderPosition() {
        double currentTime = mediaPlayer.getCurrentTime().toSeconds();
        double totalTime = mediaPlayer.getTotalDuration().toSeconds();
        double progress = currentTime / totalTime * 100.0; // Convertir en pourcentage
        progressSlider.setValue(progress);
    }

}