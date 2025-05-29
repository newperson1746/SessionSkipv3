package org.newhacker1746.sessionskipv3;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
// Decided not to manipulate at GameProfile level and to just use Prelogin stuff
// (like the original SessionSkip)
// import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.plugin.Plugin;

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Plugin(
    id = "sessionskipv3",
    name = "SessionSkip v3",
    version = "3.0.0",
    description = "Skip authentication with the Mojang session servers under certain conditions.",
    authors = {"newhacker1746"}
)
public class SessionSkip {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private boolean debug;
    private boolean enabled;
    private List<String> listeners;
    private List<String> hostnames;
    private List<String> remoteips;
    private List<String> players;

    @Inject
    public SessionSkip(ProxyServer server,
                       Logger logger,
                       @com.velocitypowered.api.plugin.annotation.DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("Initializing…");
        try {
            loadConfig();
            logger.info("Config loaded (enabled={}, debug={}, listeners={}, hostnames={}, remoteips={}, players={})",
                enabled, debug, listeners.size(), hostnames.size(), remoteips.size(), players.size());
        } catch (IOException e) {
            logger.error("Failed to load config", e);
        }

        // Register the /sessionskip command
        CommandMeta meta = server.getCommandManager()
            .metaBuilder("sessionskip")
            .aliases("sskip")
            .build();
        server.getCommandManager().register(meta, new SessionSkipCommand(this));
    }

    private void loadConfig() throws IOException {
        Path cfg = dataDirectory.resolve("config.yml");
        boolean firstRun = !cfg.toFile().exists();

        if (firstRun) {
            cfg.getParent().toFile().mkdirs();
            logger.info("First run detected – creating default config.yml");

            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in == null) {
                    logger.warn("Default config.yml not found in JAR resources!");
                    cfg.toFile().createNewFile();
                } else {
                    Files.copy(in, cfg);
                }
            }
        }

        ConfigurationLoader<?> loader = YamlConfigurationLoader.builder()
            .path(cfg)
            .build();
        ConfigurationNode root = loader.load();

        this.debug     = root.node("debug").getBoolean(true);
        this.enabled   = root.node("enabled").getBoolean(true);
        this.listeners = root.node("listeners").getList(String.class, new ArrayList<>());
        this.hostnames = root.node("hostnames").getList(String.class, new ArrayList<>());
        this.remoteips = root.node("remoteips").getList(String.class, new ArrayList<>());
        this.players   = root.node("players").getList(String.class, new ArrayList<>());
    }

public String reloadConfig() {
    try {
        loadConfig();
        String msg = String.format(
            "[SessionSkip] Config reloaded: enabled=%s, debug=%s, listeners=%d, hostnames=%d, remoteips=%d, players=%d",
            enabled, debug, listeners.size(), hostnames.size(), remoteips.size(), players.size()
        );
        logger.info(msg);
        return msg;
    } catch (IOException e) {
        logger.error("Reload failed: ", e);
        String err = String.format("[SessionSkip] Reload failed: %s", e.getMessage());
        return err;
    }
}

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

/**
   * Earliest-possible hook: force offline-mode before any session check.
   */
  @Subscribe
  public void onPreLogin(PreLoginEvent event) {
      InboundConnection conn = event.getConnection();
      String playerName = event.getUsername();
      String playerIp   = conn.getRemoteAddress().getAddress().getHostAddress();
      String hostname   = conn.getVirtualHost()
                              .map(vh -> vh.getHostString())
                              .orElse("");
      String token      = playerName + "@" + playerIp + "/" + hostname;

      if (!enabled) {
          logger.info("We are not enabled, letting {} authenticate normally", playerName);
          event.setResult(PreLoginComponentResult.allowed());
          return;
      }

      if (debug) {
          logger.info("PreLogin connection: {}", token);
      }

      // listener rule
      String listenerStr = conn.getVirtualHost()
                               .map(InetSocketAddress::toString)
                               .orElse("");
      if (listeners.contains(listenerStr)) {
          logSkip("listener", playerName, token);
          event.setResult(PreLoginComponentResult.forceOfflineMode());
          return;
      }

      // hostname rule
      if (hostnames.contains(hostname)) {
          logSkip("hostname", playerName, token);
          event.setResult(PreLoginComponentResult.forceOfflineMode());
          return;
      }

      // IP rule
      if (remoteips.contains(playerIp)) {
          logSkip("remote IP", playerName, token);
          event.setResult(PreLoginComponentResult.forceOfflineMode());
          return;
      }

      // player-specific tokens
      if (players.contains(token)
       || players.contains(playerName + "@*/*")
       || players.contains(playerName + "@*/" + hostname)
       || players.contains(playerName + "@" + playerIp + "/*")) {
          logSkip("player token", playerName, token);
          event.setResult(PreLoginComponentResult.forceOfflineMode());
          return;
      }

      if (debug) {
          logger.info("No skip-rule matched for {}, proceeding normally", playerName);
      }
      event.setResult(PreLoginComponentResult.allowed());
  }

    private void logSkip(String reason, String player, String token) {
        logger.info("Skipping authentication for {} — reason: {} matched {}", player, reason, token);
    }
}