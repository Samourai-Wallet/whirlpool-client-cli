package com.samourai.whirlpool.cli;

import com.samourai.whirlpool.cli.services.CliConfigService;
import com.samourai.whirlpool.cli.services.CliService;
import com.samourai.whirlpool.cli.utils.CliUtils;
import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

/** Main application. */
@SpringBootApplication
@ServletComponentScan(value = "com.samourai.whirlpool.cli.config.filters")
public class Application implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static Integer listenPort;
  private static boolean debug;
  private static boolean debugClient;
  private static ConfigurableApplicationContext applicationContext;
  private static ApplicationArguments applicationArguments;
  private static boolean restart;
  private static int exitCode = 0;

  @Autowired Environment env;
  @Autowired CliService cliService;

  public static void main(String... args) {
    // override configuration with local file
    System.setProperty(
        "spring.config.location",
        "classpath:application.properties,./" + CliConfigService.CLI_CONFIG_FILENAME);

    // start REST api if --listen
    listenPort = ApplicationArgs.getMainListen(args);
    if (listenPort != null) {
      System.setProperty("server.port", Integer.toString(listenPort));
    }

    // enable debug logs with --debug
    debug = ApplicationArgs.isMainDebug(args);
    debugClient = ApplicationArgs.isMainDebugClient(args);
    CliUtils.setLogLevel(debug, debugClient);

    // run
    WebApplicationType wat =
        listenPort != null ? WebApplicationType.SERVLET : WebApplicationType.NONE;
    applicationContext =
        new SpringApplicationBuilder(Application.class)
            .logStartupInfo(debugClient)
            .web(wat)
            .run(args);

    if (restart) {
      // restart
      restart();
    } else {
      // exit
      if (exitCode != 0) {
        // error
        if (log.isDebugEnabled()) {
          log.debug("Exit with error: " + exitCode);
        }
        SpringApplication.exit(applicationContext, () -> exitCode);
        System.exit(exitCode);
      } else {
        // success
        if (log.isDebugEnabled()) {
          log.debug("CLI startup complete.");
        }
      }
    }
  }

  @PreDestroy
  public void preDestroy() {
    cliService.shutdown();
  }

  @Override
  public void run(ApplicationArguments applicationArguments) {
    restart = false;

    Application.applicationArguments = applicationArguments;
    CliUtils.setLogLevel(debug, debugClient); // run twice to fix incorrect log level

    if (log.isDebugEnabled()) {
      log.debug("Run... " + Arrays.toString(applicationArguments.getSourceArgs()));
    }

    boolean listen = (listenPort != null);
    if (log.isDebugEnabled()) {
      log.debug("[cli/debug] debug=" + debug + ", debugClient=" + debugClient);
      log.debug("[cli/protocolVersion] " + WhirlpoolProtocol.PROTOCOL_VERSION);
      log.debug(
          "[cli/listen] "
              + (listen
                  ? listenPort + ", https=" + System.getProperty("security.require-ssl")
                  : "false"));
    }

    try {
      if (env.acceptsProfiles(CliUtils.SPRING_PROFILE_TESTING)) {
        log.info("Running unit test...");
        return;
      }

      restart = cliService.run(listen);
    } catch (NotifiableException e) {
      exitCode = 1;
      CliUtils.notifyError(e.getMessage());
    } catch (IllegalArgumentException e) {
      exitCode = 1;
      log.error("Invalid arguments: " + e.getMessage());
    } catch (Exception e) {
      exitCode = 1;
      log.error("", e);
    }
  }

  public static void restart() {
    long restartDelay = 1000;
    if (log.isDebugEnabled()) {
      log.debug("Restarting CLI in " + restartDelay + "ms");
    }

    // wait for restartDelay
    try {
      Thread.sleep(restartDelay);
    } catch (InterruptedException e) {
    }

    // restart application
    log.info("Restarting CLI...");
    Thread thread =
        new Thread(
            () -> {
              applicationContext.close();

              String[] restartArgs = computeRestartArgs();
              main(restartArgs);
            });
    thread.setDaemon(false);
    thread.start();
  }

  private static String[] computeRestartArgs() {
    return Arrays.stream(applicationArguments.getSourceArgs())
        .filter(a -> !a.toLowerCase().equals("--" + ApplicationArgs.ARG_INIT))
        .toArray(i -> new String[i]);
  }
}
