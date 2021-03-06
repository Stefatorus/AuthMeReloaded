package fr.xephi.authme.settings;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.GeoIpService;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.util.lazytags.Tag;
import fr.xephi.authme.util.lazytags.TagReplacer;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static fr.xephi.authme.util.FileUtils.copyFileFromResource;
import static fr.xephi.authme.util.lazytags.TagBuilder.createTag;

/**
 * Configuration for the welcome message (welcome.txt).
 */
public class WelcomeMessageConfiguration implements Reloadable {

    @DataFolder
    @Inject
    private File pluginFolder;

    @Inject
    private Server server;

    @Inject
    private GeoIpService geoIpService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private PlayerCache playerCache;

    /** List of all supported tags for the welcome message. */
    private final List<Tag<Player>> availableTags = Arrays.asList(
        createTag("&",            () -> String.valueOf(ChatColor.COLOR_CHAR)),
        createTag("{PLAYER}",     pl -> pl.getName()),
        createTag("{ONLINE}",     () -> Integer.toString(bukkitService.getOnlinePlayers().size())),
        createTag("{MAXPLAYERS}", () -> Integer.toString(server.getMaxPlayers())),
        createTag("{IP}",         pl -> PlayerUtils.getPlayerIp(pl)),
        createTag("{LOGINS}",     () -> Integer.toString(playerCache.getLogged())),
        createTag("{WORLD}",      pl -> pl.getWorld().getName()),
        createTag("{SERVER}",     () -> server.getServerName()),
        createTag("{VERSION}",    () -> server.getBukkitVersion()),
        createTag("{COUNTRY}",    pl -> geoIpService.getCountryName(PlayerUtils.getPlayerIp(pl))));

    private TagReplacer<Player> messageSupplier;

    @PostConstruct
    @Override
    public void reload() {
        List<String> welcomeMessage = readWelcomeFile();
        messageSupplier = TagReplacer.newReplacer(availableTags, welcomeMessage);
    }

    /**
     * Returns the welcome message for the given player.
     *
     * @param player the player for whom the welcome message should be prepared
     * @return the welcome message
     */
    public List<String> getWelcomeMessage(Player player) {
        return messageSupplier.getAdaptedMessages(player);
    }

    /**
     * @return the lines of the welcome message file
     */
    private List<String> readWelcomeFile() {
        File welcomeFile = new File(pluginFolder, "welcome.txt");
        if (copyFileFromResource(welcomeFile, "welcome.txt")) {
            try {
                return Files.readAllLines(welcomeFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                ConsoleLogger.logException("Failed to read welcome.txt file:", e);
            }
        } else {
            ConsoleLogger.warning("Failed to copy welcome.txt from JAR");
        }
        return Collections.emptyList();
    }
}
