package me.justeli.coins.settings;

import me.justeli.coins.main.Coins;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Created by Eli on 12/14/2016.
 *
 */

public class Settings
{
    public static final HashMap<Config.BOOLEAN, Boolean> hB = new HashMap<>();
    public static final HashMap<Config.STRING, String> hS = new HashMap<>();
    public static final HashMap<Config.DOUBLE, Double> hD = new HashMap<>();
    public static final HashMap<Config.ARRAY, List<String>> hA = new HashMap<>();

    final static HashMap<Messages, String> language = new HashMap<>();

    public static File configf;
    public static File mobf;
    public static File blockf;
    public static FileConfiguration config;
    public static FileConfiguration mob;
    public static FileConfiguration block;
    private static Coins coins;

    public Settings(Coins coins) {

        Settings.coins = coins;
        getFiles();
    }

    public static void getFiles ()
    {
        configf = new File(coins.getDataFolder(), "config.yml");
        mobf = new File(coins.getDataFolder(), "mob.yml");
        blockf = new File(coins.getDataFolder(), "block.yml");
        configf = new File( Coins.getInstance().getDataFolder() + File.separator + "config.yml" );
        if (!configf.exists()) {
            Coins.console(Coins.LogType.INFO,"config.yml doesn't exist! Creating new file.");
            Coins.getInstance().saveDefaultConfig();
        }
        if (!mobf.exists()) {
            Coins.console(Coins.LogType.INFO,"mob.yml doesn't exist! Creating new file.");
            mobf.getParentFile().mkdirs();
            coins.saveResource("mob.yml", false);
        }
        if (!blockf.exists()) {
            Coins.console(Coins.LogType.INFO,"block.yml doesn't exist! Creating new file.");
            blockf.getParentFile().mkdirs();
            coins.saveResource("block.yml", false);
        }
        config = YamlConfiguration.loadConfiguration( configf );
        mob = YamlConfiguration.loadConfiguration( mobf );
        block = YamlConfiguration.loadConfiguration( blockf );
    }

    public static FileConfiguration getMob() {
        return mob;
    }

    public static FileConfiguration getBlock() {
        return block;
    }

    public static boolean enums ()
    {
        FileConfiguration file = config;
        int errors = 0;

        try
        {
            for (Config.BOOLEAN s : Config.BOOLEAN.values())
                hB.put(s, file.getBoolean( s.name() ) );

            for (Config.STRING s : Config.STRING.values())
            {
                if (s.equals(Config.STRING.multiSuffix) && file.getString(s.name()) == null)
                {
                    hS.put(s, "s");
                    errorMessage(Msg.OUTDATED_CONFIG, new String[] {"multiSuffix: s", "dropEachCoin: false"});
                }
                else if (s.equals(Config.STRING.coinItem))
                {
                    if (file.getString(s.name()) != null)
                    {
                        try
                        {
                            Material coin = Material.valueOf(file.getString(s.name())
                                    .toUpperCase().replace(" ", "_").replace("COIN", "SUNFLOWER"));
                            hS.put(s, coin.name() );
                        }
                        catch (IllegalArgumentException | NullPointerException e)
                        {
                            errorMessage( Msg.NO_SUCH_MATERIAL, new String[]{file.getString(s.name())} );
                            hS.put(s, "SUNFLOWER" );
                            errors++;
                        }
                    }
                    else
                    {
                        errorMessage(Msg.OUTDATED_CONFIG, new String[] {"coinItem: coin"});
                        hS.put(s, "SUNFLOWER" );
                        errors ++;
                    }
                }
                else if (file.getString( s.name() ) != null)
                    hS.put(s, file.getString( s.name() ) );
                else
                {
                    errorMessage (Msg.OUTDATED_CONFIG, new String[]{s.name()});
                    errors++;
                }
            }

            for (Config.DOUBLE s : Config.DOUBLE.values())
                hD.put(s, file.getDouble( s.name().replace('_', '.') ) );

            for (Config.ARRAY s : Config.ARRAY.values())
                hA.put(s, file.getStringList( s.name() ) );

        }
        catch (NullPointerException e)
        {
            errorMessage (Msg.OUTDATED_CONFIG, null);
            return false;
        }

        boolean langErr = setLanguage();
        if (!langErr) errors++;

        return errors == 0;
    }

    public static void remove ()
    {
        hB.clear();
        hS.clear();
        hD.clear();
        hA.clear();
        getFiles();
    }

    public static String getSettings ()
    {
        StringBuilder message = new StringBuilder( Messages.LOADED_SETTINGS.toString() + "\n&r" );

        for (Config.BOOLEAN s : Config.BOOLEAN.values())
            if (!s.equals(Config.BOOLEAN.olderServer))
                message.append(s.toString()).append(" &7\u00BB ")
                        .append(hB.get(s).toString().replace("true", "&atrue").replace("false", "&cfalse")).append("\n&r");

        for (Config.DOUBLE s : Config.DOUBLE.values())
            message.append(s.toString().replace("_", " &o")).append(" &7\u00BB &e").append(hD.get(s)).append("\n&r");

        for (Config.ARRAY s : Config.ARRAY.values())
            message.append(s.toString()).append(" &7\u00BB &b").append(hA.get(s)).append("\n&r");

        return message.toString();
    }

    private static boolean setLanguage ()
    {
        for (String lang :new String[]{"english", "dutch", "spanish", "german", "french", "swedish", "chinese", "hungarian"})
            if (!new File(Coins.getInstance().getDataFolder() + File.separator + "language" + File.separator + lang + ".json").exists())
                Coins.getInstance().saveResource("language/" + lang + ".json", false);

        FileConfiguration file = config;
        String lang = getLanguage();
        boolean failure = false;

        if (lang == null)
        {
            failure = true;
            lang = "english";
        }

        try
        {
            JSONParser parser = new JSONParser();
            Object object = parser.parse(new InputStreamReader(new FileInputStream(Coins.getInstance().getDataFolder()
                    + File.separator + "language" + File.separator + lang + ".json"), StandardCharsets.UTF_8));
            JSONObject json = (JSONObject) object;
            for (Messages m : Messages.values())
            {
                try
                {
                    Object name = json.get(m.name());
                    language.put( m, name.toString() );
                }
                catch (NullPointerException ex)
                {
                    failure = true;
                    if (m.equals(Messages.INVENTORY_FULL))
                        language.put( m, "&cYou cannot withdraw when your inventory is full!" );
                    if (m.equals(Messages.VERSION_CHECK))
                        language.put(m, "&c/coins version &7- check if there's a new release");
                    errorMessage(Msg.NO_TRANSLATION, new String[]{m.name()});
                }
            }
        }
        catch (FileNotFoundException e)
        { errorMessage(Msg.LANG_NOT_FOUND, new String[] {file.getString("language")}); return false; }
        catch (ParseException | IOException e)
        { e.printStackTrace(); return false; }

        return !failure;
    }

    public static String getLanguage ()
    {
        try
        {
            FileConfiguration file = config;
            return file.getString("language").toLowerCase();
        }
        catch (NullPointerException e)
        { errorMessage(Msg.OUTDATED_CONFIG, new String[] {"language: english"}); return null; }
    }

    public enum Msg
    {
        OUTDATED_CONFIG,
        LANG_NOT_FOUND,
        NO_SUCH_ENTITY,
        NO_SUCH_SOUND,
        NO_ECONOMY_SUPPORT,
        NO_BEASTTOKENS_SUPPORT,
        NO_JETSMINIONS_SUPPORT,
        NO_WILDSTACKER_SUPPORT,
        NO_SUCH_MATERIAL,
        NO_TRANSLATION,
    }

    public static void errorMessage (Msg msg, String[] input)
    {
        switch (msg)
        {
            case OUTDATED_CONFIG:
                Coins.console(Coins.LogType.ERROR, "Your config of Coins is outdated, update the Coins config.yml.");
                Coins.console(Coins.LogType.ERROR, "You can copy it from here: https://github.com/JustEli/Coins/blob/master/src/config.yml");
                Coins.console(Coins.LogType.ERROR, "Use /coins reload afterwards. You could also remove the config if you haven't configured it.");
                if (input != null)
                    Coins.console(Coins.LogType.ERROR, "This option is probably missing (add it): " + Arrays.toString(input));
                break;
            case LANG_NOT_FOUND:
                Coins.console(Coins.LogType.ERROR, "The language '" + input[0] + "' that you set in your config does not exist.");
                Coins.console(Coins.LogType.ERROR, "Check all available languages in the folder 'Coins/language'.");
                break;
            case NO_SUCH_ENTITY:
                Coins.console(Coins.LogType.ERROR, "There is no entity with the name '" + input[0] + "', please change the Coins config.");
                Coins.console(Coins.LogType.ERROR, "Get types from here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/entity/EntityType.html");
                break;
            case NO_SUCH_MATERIAL:
                Coins.console(Coins.LogType.ERROR, "There is no material with the name '" + input[0] + "', please change the Coins config.");
                Coins.console(Coins.LogType.ERROR, "Get materials from here: https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html");
                break;
            case NO_SUCH_SOUND:
                Coins.console(Coins.LogType.ERROR, "The sound '" + input[0] + "' does not exist. Change it in the Coins config." );
                Coins.console(Coins.LogType.ERROR, "Please use a sound from: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Sound.html" );
                break;
            case NO_ECONOMY_SUPPORT:
                Coins.console(Coins.LogType.ERROR, "There seems to be no Vault or Economy plugin installed." );
                Coins.console(Coins.LogType.ERROR, "Please install Vault." );
                Coins.console(Coins.LogType.ERROR, "Coins will be disabled now.." );
                break;
            case NO_BEASTTOKENS_SUPPORT:
                Coins.console(Coins.LogType.ERROR, "There seems to be no BeastTokens plugin installed." );
                Coins.console(Coins.LogType.ERROR, "Please install BeastTokens." );
                Coins.console(Coins.LogType.ERROR, "Coins will be disabled now.." );
            case NO_JETSMINIONS_SUPPORT:
                Coins.console(Coins.LogType.ERROR, "There seems to be no JetsMinions plugin installed." );
                Coins.console(Coins.LogType.ERROR, "Please install JetsMinions." );
                Coins.console(Coins.LogType.ERROR, "Coins will be disabled now.." );
            case NO_WILDSTACKER_SUPPORT:
                Coins.console(Coins.LogType.ERROR, "There seems to be no WildStacker plugin installed." );
                Coins.console(Coins.LogType.ERROR, "Please install WildStacker." );
                Coins.console(Coins.LogType.ERROR, "Coins will be disabled now.." );
            case NO_TRANSLATION:
                Coins.console(Coins.LogType.ERROR, "The translation for '" + input[0] + "' was not found.");
                Coins.console(Coins.LogType.ERROR, "Please add it to the {language}.json file.");
                Coins.console(Coins.LogType.ERROR, "Or delete your /language/ folder in /Coins/. RECOMMENDED");
                break;
        }

    }

}
