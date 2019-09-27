package me.justeli.coins.main;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.MrAxe.BeastTokens.Tokens.TokensAPI;
import me.justeli.coins.cancel.CancelHopper;
import me.justeli.coins.cancel.CancelInventories;
import me.justeli.coins.cancel.CoinPlace;
import me.justeli.coins.cancel.PreventSpawner;
import me.justeli.coins.events.CoinsPickup;
import me.justeli.coins.events.DropCoin;
import me.justeli.coins.item.CoinParticles;
import me.justeli.coins.settings.Config;
import me.justeli.coins.settings.Settings;
import net.milkbowl.vault.economy.Economy;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

/**
 * Created by Eli on 12/13/2016.
 *
 */

public class Coins extends JavaPlugin
{
    private static Coins main;
    private static TokensAPI tokensAPI;

    static String update;

    // todo add NBT-tags for coins
    // todo config option for pitch/volume pickup
    // todo able to pickup with inventory full
    // todo send locale messages
    // todo support for standalone Vault

    // https://hub.spigotmc.org/javadocs/spigot/org/bukkit/inventory/meta/tags/CustomItemTagContainer.html
    // https://www.spigotmc.org/resources/pickupmoney.11334/

    @Override
    public void onEnable ()
    {
        main = this;
        Locale.setDefault(Locale.US);

        registerConfig();
        registerEvents();
        registerCommands();

        async(() ->
        {
            String v = Bukkit.getVersion();
            Bukkit.getConsoleSender().sendMessage(v);
            if (v.contains("1.8") || v.contains("1.7"))
                Settings.hB.put(Config.BOOLEAN.olderServer, true);
            if (v.contains("1.14") || v.contains("1.13"))
                Settings.hB.put(Config.BOOLEAN.newerServer, true);

            String version;
            try
            {
                URL           url     = new URL("https://api.github.com/repos/JustEli/Coins/releases/latest");
                URLConnection request = url.openConnection();
                request.connect();

                JsonParser  jp      = new JsonParser();
                JsonElement root    = jp.parse(new InputStreamReader((InputStream) request.getContent()));
                JsonObject  rootobj = root.getAsJsonObject();
                version = rootobj.get("tag_name").getAsString();

            }
            catch (IOException ex)
            { version = getDescription().getVersion(); }

            Coins.update = version;

            if (!getDescription().getVersion().equals(version))
            {
                Coins.console(LogType.INFO, "A new version of Coins was released (" + version + ")!");
                Coins.console(LogType.INFO, "https://www.spigotmc.org/resources/coins.33382/");
            }
        });

        later(() ->
        {
            Metrics metrics = new Metrics(this);

            metrics.add("language", WordUtils.capitalize(Settings.getLanguage()));
            metrics.add("currencySymbol", Settings.hS.get(Config.STRING.currencySymbol));
            metrics.add("dropChance", Settings.hD.get(Config.DOUBLE.dropChance)*100 + "%");
            metrics.add("dropEachCoin", String.valueOf(Settings.hB.get(Config.BOOLEAN.dropEachCoin)));
            metrics.add("pickupSound", Settings.hS.get(Config.STRING.soundName));
            metrics.add("enableWithdraw", String.valueOf(Settings.hB.get(Config.BOOLEAN.enableWithdraw)));
            metrics.add("loseOnDeath", String.valueOf(Settings.hB.get(Config.BOOLEAN.loseOnDeath)));
            metrics.add("passiveDrop", String.valueOf(Settings.hB.get(Config.BOOLEAN.passiveDrop)));

            metrics.add("nameOfCoin", Settings.hS.get(Config.STRING.nameOfCoin));
            metrics.add("coinItem", Settings.hS.get(Config.STRING.coinItem));
            metrics.add("pickupMessage", Settings.hS.get(Config.STRING.pickupMessage));
            metrics.add("moneyDecimals", String.valueOf(Settings.hD.get(Config.DOUBLE.moneyDecimals).intValue()));
            metrics.add("stackCoins", String.valueOf(Settings.hB.get(Config.BOOLEAN.stackCoins)));
            metrics.add("playerDrop", String.valueOf(Settings.hB.get(Config.BOOLEAN.playerDrop)));
            metrics.add("spawnerDrop", String.valueOf(Settings.hB.get(Config.BOOLEAN.spawnerDrop)));
            metrics.add("preventSplits", String.valueOf(Settings.hB.get(Config.BOOLEAN.preventSplits)));

            metrics.add("moneyAmount", ( String.valueOf((Settings.hD.get(Config.DOUBLE.moneyAmount_from)
                    + Settings.hD.get(Config.DOUBLE.moneyAmount_to))/2) ));
        });

        if (getServer().getPluginManager().getPlugin("Vault") == null)
            Bukkit.getPluginManager().disablePlugin(this);

        try
        {
            RegisteredServiceProvider<TokensAPI> rsp = getServer().getServicesManager().getRegistration(TokensAPI.class);
            tokensAPI = rsp.getProvider();
        }
        catch (NullPointerException | NoClassDefFoundError e)
        {
            Settings.errorMessage(Settings.Msg.NO_ECONOMY_SUPPORT, new String[]{""});
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    public static TokensAPI getTokensAPI (){ return tokensAPI; }

    public static void particles (Location location, int radius, int amount)
    {
        CoinParticles.dropCoins(location, radius, amount);
    }

    public static Coins getInstance ()
    {
        return main;
    }

    public static boolean mobFromSpawner (Entity entity)
    {
        return PreventSpawner.fromSpawner(entity);
    }

    private void registerEvents ()
    {
        PluginManager manager = getServer().getPluginManager();
        manager.registerEvents(new CancelHopper(), this);
        manager.registerEvents(new PreventSpawner(), this);
        manager.registerEvents(new CoinsPickup(), this);
        manager.registerEvents(new DropCoin(), this);
        manager.registerEvents(new CoinPlace(), this);
        manager.registerEvents(new CancelInventories(), this);
    }

    private void registerCommands ()
    {
        this.getCommand("coins").setExecutor(new Cmds());
        this.getCommand("coins").setTabCompleter(new TabComplete());

        if (Settings.hB.get(Config.BOOLEAN.enableWithdraw))
        {
            this.getCommand("withdraw").setExecutor(new Cmds());
            this.getCommand("withdraw").setTabCompleter(new TabComplete());
        }
    }

    private void registerConfig ()
    {
        Settings.enums();
    }

    private static int async (Runnable runnable)
    {
        BukkitTask task = new BukkitRunnable()
        {
            @Override
            public void run()
            {
                runnable.run();
            }
        }
                .runTaskAsynchronously(getInstance());

        return task.getTaskId();
    }

    private static int later (Runnable runnable)
    {
            BukkitTask task = new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    runnable.run();
                }
            }
                    .runTaskLater(getInstance(), 1);

            return task.getTaskId();
    }

    public enum LogType
    {
        ERROR,
        WARNING,
        INFO
    }

    public static void console (LogType type, String message)
    {
        ChatColor color;
        switch (type)
        {
            case INFO:      color = ChatColor.AQUA;     break;
            case ERROR:     color = ChatColor.RED;      break;
            case WARNING:   color = ChatColor.YELLOW;   break;
            default:        color = ChatColor.WHITE;    break;
        }
        Bukkit.getConsoleSender().sendMessage(color + "=" + type.name() + "= " + message);
    }
}
