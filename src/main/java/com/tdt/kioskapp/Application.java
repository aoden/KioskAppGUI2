package com.tdt.kioskapp;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.tdt.kioskapp.config.AppConfig;
import com.tdt.kioskapp.service.BaseService;
import com.tdt.kioskapp.ui.KioskUI;
import org.apache.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;

import java.awt.*;

public class Application {

    public static void main(String[] args) {

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {

                try {
                    String vlcHome = System.getenv("VLC_HOME");
                    // path variable MUST NOT have slashing at the end(correct example is "/home/aaa/temp"),
                    // if env variable not found then default relatively "temp" will be chosen(and may cause white screen bug)
                    BaseService.TEMP_DIR = System.getenv("TEMP_DIR") != null ? System.getenv("TEMP_DIR"): BaseService.TEMP_DIR;
                    NativeLibrary.addSearchPath(
                            RuntimeUtil.getLibVlcLibraryName(), vlcHome
                    );
                    Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
                    new KioskUI(new AnnotationConfigApplicationContext(AppConfig.class));
                } catch (Exception e) {

                    Logger logger = Logger.getLogger(KioskUI.class);
                    logger.error(e.getCause());
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        });
    }
}
