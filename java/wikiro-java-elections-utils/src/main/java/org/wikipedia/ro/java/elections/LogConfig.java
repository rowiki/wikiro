package org.wikipedia.ro.java.elections;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LogConfig
{

    public LogConfig()
    {
        LogManager logManager = LogManager.getLogManager();

        try
        {
            // Programmatic configuration
            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS.%1$tL %4$-7s [%3$s] (%2$s) %5$s %6$s%n");

            final FileHandler fileHandler = new FileHandler("/tmp/mayors.log");
            fileHandler.setLevel(Level.FINEST);
            fileHandler.setFormatter(new SimpleFormatter());

            final Logger app = Logger.getLogger("app");
            app.setLevel(Level.FINEST);
            app.addHandler(fileHandler);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

}
