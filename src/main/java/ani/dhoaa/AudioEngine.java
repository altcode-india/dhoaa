
package ani.dhoaa;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.discovery.NativeDiscovery;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;

/*
 * Licensed under AltCode GPv1 license.
 * http://www.altcode.in
 */
/**
 *
 * @author Aniruddha Sarkar
 */
public class AudioEngine {

    public interface StateListener {

        public void changeDetected(int state, AudioElement audio);

    }
    static public final int BUFFERING = 1, PAUSED = 2, UNLOADED = 3, PLAYING = 4;
    private int state;
    private final HomeController controller;
    private final MediaPlayer player;
    private AudioElement audio;
    private final StateListener stateChanged;
    private final boolean foundNative;

    public AudioEngine(HomeController controller, StateListener onStateChange) {
        this.controller = controller;
        this.state = UNLOADED;
        stateChanged = onStateChange;
        foundNative = new NativeDiscovery().discover();
        player = new AudioMediaPlayerComponent().getMediaPlayer();
        player.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                state = PLAYING;
                stateChanged.changeDetected(state, audio);
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                state = UNLOADED;
                stateChanged.changeDetected(state, audio);
            }

            @Override
            public void mediaFreed(MediaPlayer mediaPlayer) {
                state = UNLOADED;
                stateChanged.changeDetected(state, audio);
            }

            @Override
            public void buffering(MediaPlayer mediaPlayer, float newCache) {
                state = player.isPlaying()?PLAYING:BUFFERING;
                stateChanged.changeDetected(state, audio);
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                state = PAUSED;
                stateChanged.changeDetected(state, audio);
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                state = UNLOADED;
                stateChanged.changeDetected(state, audio);
            }

        });
    }

    public AudioElement getCurrentAudio() {
        return audio;
    }

    public void play(AudioElement audio) {
        this.audio = audio;
        stop();
        try {
            player.playMedia(audio.getType()==AudioElement.FILE?URLDecoder.decode(audio.getSource().getPath(),"UTF-8"):audio.getSource().toExternalForm());
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(AudioEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
        

    }

    public void seek(long time) {
        player.skip(time);
    }

    public void togglePause() {
        if (player != null) {
            if (state == PAUSED) {
                player.play();
            } else {
                player.pause();
            }
        }
    }

    public void stop() {
        if (player != null && state != UNLOADED) {
            player.stop();
            
        }
    }

    public long getLength() {
        return player.getLength();
    }

    public long getTime() {
        return player.getTime();
    }

    public int getState() {
        return state;
    }

    public static class AudioElement implements Serializable {
        public final static int FILE=1, WEB=0, RESIZABLE=0,NON_RESIZABLE=1;
        private int type,icon_type;
        private String title, id;
        private URL source, icon;

        public AudioElement(URL source, String title, URL icon, String id) {
            this.source = source;
            this.title = title;
            this.icon = icon;
            this.icon_type=RESIZABLE;
            this.id = id;
            this.type=WEB;
        }
        
        public AudioElement(URL source, String title, URL icon,int icon_type, String id,int type) {
            this.source = source;
            this.title = title;
            this.icon = icon;
            this.icon_type=icon_type;
            this.id = id;
            this.type=type;
        }

        public AudioElement() {
        }

        public void setSource(URL source) {
            this.source = source;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setIcon(URL icon) {
            this.icon = icon;
        }
        
        public void setType(int type) {
            this.type = type;
        }
        public void setIconType(int icon_type) {
            this.icon_type = icon_type;
        }

        public void setID(String id) {
            this.id = id;
        }

        public URL getSource() {
            return source;
        }

        public String getTitle() {
            return title;
        }

        public URL getIcon() {
            return icon;
        }

        public String getID() {
            return id;
        }
        public int getType(){
            return type;
        }
        public int getIconType(){
            return icon_type;
        }
        @Override
        public boolean equals(Object o){
            if(o instanceof AudioElement)
            return o.hashCode()==hashCode();
            else return false;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 37 * hash + Objects.hashCode(this.id);
            return hash;
        }
        @Override
        public String toString() {
            return title;
        }
    }
}
