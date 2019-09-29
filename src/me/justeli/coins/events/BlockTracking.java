package me.justeli.coins.events;

import java.util.ArrayList;
import java.util.HashMap;

import me.justeli.coins.main.Coins;
import org.bukkit.Location;

public class BlockTracking {
    Coins pl;
    HashMap<String, ArrayList<Location>> chunkList = new HashMap();

    public BlockTracking(Coins var1) {
        this.pl = var1;
    }

    public void addLocation(String var1, Location var2) {
        if (!this.chunkList.containsKey(var1)) {
            this.chunkList.put(var1, new ArrayList());
        }

        ArrayList var3 = (ArrayList)this.chunkList.get(var1);
        if (!var3.contains(var2)) {
            var3.add(var2);
        }

        this.chunkList.put(var1, var3);
    }

    public boolean hasLocation(String var1, Location var2) {
        if (this.chunkList.containsKey(var1)) {
            ArrayList var3 = (ArrayList)this.chunkList.get(var1);
            if (var3.contains(var2)) {
                return true;
            }
        }

        return false;
    }

    public void removeLocation(String var1, Location var2) {
        if (this.chunkList.containsKey(var1)) {
            ((ArrayList)this.chunkList.get(var1)).remove(var2);
        }

    }
}
