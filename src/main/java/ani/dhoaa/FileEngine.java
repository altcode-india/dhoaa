package ani.dhoaa;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

/*
 * Licensed under AltCode GPv1 license.
 * http://www.altcode.in
 */
/**
 *
 * @author Aniruddha Sarkar
 */
public class FileEngine {

    Path app_home, sqlitedb, config, scan_dir;
    Properties dict;

    private void saveProps() {
        try {
            dict.store(Files.newBufferedWriter(config), "");
        } catch (IOException ex) {
            Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setProp(String key, String val) {
        dict.setProperty(key, val);
        saveProps();
    }

    public String getProp(String key) {
        return dict.getProperty(key,"0");
    }

    private void loadProps() {
        try {
            dict.load(Files.newBufferedReader(config));
        } catch (IOException ex) {
            Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public FileEngine() {
        try {
            boolean proceed = false;
            app_home = Paths.get(System.getProperty("user.home"), ".dhoaa");
            if (Files.exists(app_home)) {
                if (!Files.isDirectory(app_home)) {
                    Files.delete(app_home);
                } else {
                    proceed = true;
                }
            }
            if (!Files.exists(app_home)) {
                app_home = Files.createDirectory(app_home);
                proceed = true;

            }
            if (proceed) {
                sqlitedb = app_home.resolve("tracks.db");
                config = app_home.resolve("config.dict");
                dict = new Properties();
                dict.setProperty("scan_dir", Paths.get(System.getProperty("user.home"), "Music").toString());
                if (Files.exists(config)) {
                    loadProps();
                }
                Connection con = DriverManager.getConnection("jdbc:sqlite:" + sqlitedb.toString());
                if (con != null) {
                    Statement stmt = con.createStatement();
                    stmt.execute("CREATE TABLE IF NOT EXISTS music(id INTEGER PRIMARY KEY AUTOINCREMENT,source TEXT UNIQUE,name TEXT,icon TEXT);");
                    con.close();
                }
                scan_dir = Paths.get(dict.getProperty("scan_dir"));
                saveProps();
            }
            System.out.println("Scan dir:" + scan_dir + "\n"
                    + "App Dir:" + app_home + "\n"
                    + "sqliteb:" + sqlitedb + "\n"
                    + "config:" + config);
        } catch (SQLException | IOException ex) {
            Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Thread startScan(Runnable onScanDone, boolean force) {
        Thread t = new Thread(() -> {
            try {
                if (force || (!getProp("scan").equals("1"))) {
                    scanFilesToDB(scan_dir);
                    setProp("scan","1");
                }
            } catch (IOException ex) {
                Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            onScanDone.run();
        });
        return t;
    }

    public Thread startScan(Runnable onScanDone) {
        Thread t = new Thread(() -> {
            try {
                if ((getProp("scan").equals("1"))) {
                    scanFilesToDB(scan_dir);
                }
            } catch (IOException ex) {
                Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
            onScanDone.run();
        });
        return t;
    }

    public Thread startScan(Runnable onScanDone, Path p) {
        Thread t = new Thread(() -> {
            try {
                scanFilesToDB(p);
            } catch (IOException ex) {
                Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
            onScanDone.run();
        });
        return t;
    }

    public void scanFilesToDB(Path p) throws IOException {
        System.out.println("Scanning in progress!");
        Files.walkFileTree(p, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith("mp3")) {
                    System.out.println("Found:" + file);
                    try {
                        toDB(parseAudioFile(file));
                    } catch (CannotReadException | ReadOnlyFileException | InvalidAudioFrameException | TagException ex) {
                        Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                return FileVisitResult.CONTINUE; //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE; //To change body of generated methods, choose Tools | Templates.
            }
        });

    }

    public AudioEngine.AudioElement parseAudioFile(Path p) throws CannotReadException, ReadOnlyFileException, InvalidAudioFrameException, TagException, IOException {
        return parseAudioFile(p, false);
    }

    public AudioEngine.AudioElement parseAudioFile(Path p, boolean icon_rep) throws CannotReadException, ReadOnlyFileException, InvalidAudioFrameException, TagException, IOException {

        Tag tag = AudioFileIO.read(p.toFile()).getTag();
        URL url;
        String name = p.getFileName().toString();
        try {
            url = new URL(tag.getFirst(FieldKey.COVER_ART));
        } catch (MalformedURLException | NullPointerException ex) {
            url = FileEngine.class.getResource("/icons/music.png");
        }
        try {
            name = tag.getFirst(FieldKey.TITLE);
        } catch (NullPointerException ex) {

        }
        return new AudioEngine.AudioElement(new URL(p.toUri().toString()), name, url, 1, p.getFileName().toString(), 1);

    }

    public void toDB(AudioEngine.AudioElement elem) {
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + sqlitedb.toString())) {
            Statement stmt = con.createStatement();
            stmt.execute("INSERT INTO music (source,name,icon) VALUES ('" + URLEncoder.encode(elem.getSource().toString(), "UTF-8") + "','" + elem.getTitle() + "','" + URLEncoder.encode(elem.getIcon().toString(), "UTF-8") + "');");
        } catch (SQLException | UnsupportedEncodingException ex) {

        }
    }

    public AudioEngine.AudioElement fromDB(int id) {

        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + sqlitedb.toString())) {
            Statement stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM music WHERE id=" + id + ";");
            if (rs.first()) {
                URL url;
                try {
                    url = new URL(URLDecoder.decode(rs.getString("icon"), "UTF-8"));
                } catch (MalformedURLException | UnsupportedEncodingException ex) {
                    Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
                    url = FileEngine.class.getResource("/icons/music.png");
                }
                AudioEngine.AudioElement elem = new AudioEngine.AudioElement(new URL(URLDecoder.decode(rs.getString("source"), "UTF-8")), rs.getString("name"), url, 1, rs.getString("id"), 1);

                return elem;
            }
        } catch (SQLException | MalformedURLException | UnsupportedEncodingException ex) {
            Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

    public List<AudioEngine.AudioElement> fromDB(String like) {
        ArrayList<AudioEngine.AudioElement> list = new ArrayList();
        try (Connection con = DriverManager.getConnection("jdbc:sqlite:" + sqlitedb.toString())) {
            Statement stmt = con.createStatement();
            like=URLEncoder.encode(like, "UTF-8").replaceAll("\\+", "%20");
            ResultSet rs = stmt.executeQuery("SELECT * FROM music WHERE name LIKE '%" + like + "%' OR source LIKE '%" + like + "%';");
            while (rs.next()) {
                URL url;
                try {
                    url = new URL(URLDecoder.decode(rs.getString("icon"), "UTF-8"));
                } catch (MalformedURLException | UnsupportedEncodingException ex) {
                    url = FileEngine.class.getResource("/icons/music.png");
                    Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
                }
                URL file = new URL(URLDecoder.decode(rs.getString("source"), "UTF-8"));
                list.add(new AudioEngine.AudioElement(file, rs.getString("name"), url, 1, rs.getString("id"), 1));
            }
        } catch (SQLException | MalformedURLException | UnsupportedEncodingException ex) {
            Logger.getLogger(FileEngine.class.getName()).log(Level.SEVERE, null, ex);
        }
        return list;

    }
}
