package com.tdt.kioskapp.ui;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.tdt.kioskapp.dto.FirebaseTokenDTO;
import com.tdt.kioskapp.dto.SlideDTO;
import com.tdt.kioskapp.service.BaseService;
import net.lingala.zip4j.exception.ZipException;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.ResourceAccessException;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main UI of program
 *
 * @author aoden
 */
public class KioskUI extends JFrame implements Runnable {

    public static final Dimension TEXT_SIZE = new Dimension(120, 25);
    public static final int MARGIN_RATIO = 4;
    // this var is meant to prevent the second download
    public static volatile int playCount = 0;
    // this is meant to prevent program uses previous cached data when not yet activated
    public static volatile boolean activated = false;
    protected static volatile boolean play = true;
    protected EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
    protected ApplicationContext context;
    protected JPanel initPanel = new JPanel();
    protected JLabel initLabel = new JLabel("Please enter a key to unlock: ");
    protected JTextField textField = new JTextField();
    protected JButton startBtn = new JButton("START");
    Logger logger = Logger.getLogger(KioskUI.class);
    Thread slideShowThread;
    private BaseService baseService;
    private Firebase firebase = null;
    private ExecutorService executorService = Executors.newFixedThreadPool(1);
    private KeyAdapter keyListener = new KeyAdapter() {
        @Override
        public void keyTyped(KeyEvent e) {

        }

        @Override
        public void keyPressed(KeyEvent e) {

            if (e.isAltDown() && e.isControlDown()) {

                System.exit(0);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }
    };
    private ObjectMapper objectMapper = new ObjectMapper();
    private Container loadingPane;

    public KioskUI(ApplicationContext context) throws ZipException, IOException {

        this.context = context;
        final BaseService baseService = context.getBean(BaseService.class);
        this.baseService = baseService;
        textField.addKeyListener(keyListener);
        initUI();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        if (baseService.registered()) {

            new Thread(KioskUI.this).start();
            playCount++;
        } else {

            startBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    hideMouse();
                    new Thread(KioskUI.this).start();
                    playCount++;
                }
            });
        }

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                mediaPlayerComponent.release(true);
            }
        });
    }

    // for testing purpose only
    public static void main(String[] args) {
        String path = "H:/KioskAppGUI/classes/artifacts/KioskAppGUI_jar/KioskAppGUI.jar/../temp/1/0001401696.mp4";
        System.out.println(BaseService.reformatPath(path));
    }

    private void startSlideShow(final BaseService baseService) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, SlideDTO> manifest = null;
        try {
            manifest = objectMapper.
                    readValue(new File(baseService.reformatPath(baseService.TEMP_DIR + "/" + baseService.MANIFEST_JSON)), new TypeReference<Map<String, SlideDTO>>() {
                    });

        } catch (FileNotFoundException e) {

            e.printStackTrace();
            return;
        }
        for (Map.Entry<String, SlideDTO> entry : manifest.entrySet()) {
            SlideDTO currentValue = entry.getValue();
            File currentFile = baseService.readAllFiles(BaseService.reformatPath(BaseService.TEMP_DIR + "/" + currentValue.getLocation()));
            if (currentFile != null) {
                hideMouse();
                refresh(BaseService.reformatPath(BaseService.TEMP_DIR + "/" + currentValue.getLocation() + "/" + currentFile.getName()), currentValue.getSeconds() * 1000);
            }
        }
        playCount++;
    }

    private void showMessage() {

        JOptionPane.showMessageDialog(this, "invalid token, please try again!");
    }

    private void initUI() {

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout());
        setUndecorated(true);
        setVisible(true);
        hideMouse();
        getContentPane().setPreferredSize(getBounds().getSize());
        // show input if there is no login key in database
        if (!baseService.registered()) {

            textField.setPreferredSize(TEXT_SIZE);
            initPanel.add(initLabel);
            initPanel.add(textField);
            initPanel.add(startBtn);
            JPanel margin = new JPanel();
            margin.setPreferredSize(new Dimension((int) getBounds().getWidth(), (int) getBounds().getHeight() / MARGIN_RATIO));
            add(margin, BorderLayout.NORTH);
            add(initPanel, BorderLayout.CENTER);
        }
        pack();
        startBtn.setDefaultCapable(true);
        getRootPane().setDefaultButton(startBtn);
        EmbeddedMediaPlayer mediaPlayer = mediaPlayerComponent.getMediaPlayer();
        mediaPlayer.setFullScreen(true);
        mediaPlayer.setEnableKeyInputHandling(true);
        mediaPlayer.setEnableMouseInputHandling(true);
        mediaPlayerComponent.getVideoSurface().addKeyListener(keyListener);
    }

    private void refresh(String location, int duration) throws IOException, InterruptedException {

        play(location, duration);
    }

    private void play(String location, int duration) throws IOException, InterruptedException {

        hideMouse();
        EmbeddedMediaPlayer mediaPlayer = mediaPlayerComponent.getMediaPlayer();
        mediaPlayer.startMedia(BaseService.reformatPath(location));
        hideMouse();
        mediaPlayer.parseMedia();
        mediaPlayerComponent.getVideoSurface().requestFocusInWindow();
        if ("image/jpeg".equals(Files.probeContentType(Paths.get(location))) || location == null) {

            Thread.sleep(duration);
        } else {

            Thread.sleep(mediaPlayer.getLength());
        }
    }

    @Override
    public void run() {

        String key = baseService.getLogin(null);
        if (!baseService.registered()) {

            key = baseService.register(textField.getText());
        }

        key = baseService.login(key);
        final String finalKey = key;

        while (play) {

            Container oldContentPane = KioskUI.this.getContentPane();
            try {

                slideShowThread = null;
                File manifest = new File(BaseService.reformatPath(BaseService.TEMP_DIR + "/" + BaseService.MANIFEST_JSON));
                boolean exist = manifest.exists() && (baseService.countFiles(BaseService.TEMP_DIR) >= 2);
                EventQueue.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        initFirebase(finalKey);
                    }
                });
                if (exist) {

                    if (KioskUI.this.getContentPane() != mediaPlayerComponent) {
                        KioskUI.this.setContentPane(mediaPlayerComponent);
                    }
                    resetUI();
                    activated = true;
                    hideMouse();
                    startSlideShow(baseService);
                } else { // subscribe to 2 channels

                    // add loading text
                    if (KioskUI.this.getContentPane() != loadingPane) {

                        loadingPane = new JPanel();
                        KioskUI.this.setContentPane(loadingPane);
                        KioskUI.this.getContentPane().setBackground(Color.BLACK);
                        JLabel loadingText = new JLabel("<html><font color='gray'>LOADING...</font></html>");
                        KioskUI.this.getContentPane().add(loadingText);
                        resetUI();
                    }
                }
            } catch (Exception e) {
                if (!(e instanceof ResourceAccessException || e instanceof ZipException) || !baseService.registered()) {

                    showMessage();
                    e.printStackTrace();
                    KioskUI.this.setContentPane(oldContentPane);
                    resetUI();
                    activated = false;
                    break;
                }
                if (e instanceof ResourceAccessException) {

                    while (true) {

                        try {
                            baseService.downloadAndUnpack(key);
                            break;
                        } catch (Exception e1) {

                            e1.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void initFirebase(final String finalKey) {
        if (firebase == null) {

            firebase = new Firebase(baseService.getFirebaseURL());
            FirebaseTokenDTO tokenDTO = baseService.getFirebaseToken(finalKey);
            firebase.authWithCustomToken(tokenDTO.getFirebaseToken(), null);

            firebase.child(tokenDTO.getUpdates()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String manifest = "{}";
                    try {
                        File manifestFile = new File(baseService.reformatPath(BaseService.TEMP_DIR + "/" + BaseService.MANIFEST_JSON));
                        if (manifestFile.exists())
                            manifest = objectMapper.readValue(manifestFile, new TypeReference<String>() {
                            });
                        baseService.getRequest(finalKey, manifest);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });

            firebase.child(tokenDTO.getDownloads()).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {

                                baseService.downloadAndUnpack(finalKey);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(FirebaseError firebaseError) {

                }
            });
        }
    }

    private void hideMouse() {

        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");
        getContentPane().setCursor(blankCursor);
        mediaPlayerComponent.setCursor(blankCursor);
    }

    private void resetUI() {
        hideMouse();
        KioskUI.this.revalidate();
        KioskUI.this.repaint();
        getRootPane().setDefaultButton(startBtn);
    }
}

