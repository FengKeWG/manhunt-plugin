package org.windguest.manhunt.jobs;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Consumer;

public class Job {
    private final String displayName;
    private final Material iconMaterial;
    private final List<String> lore;
    private final int menuSlot;
    private final Consumer<Player> kitGiver;

    public Job(int menuSlot, String displayName, Material iconMaterial, List<String> lore, Consumer<Player> kitGiver) {
        this.menuSlot = menuSlot;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.lore = lore;
        this.kitGiver = kitGiver;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }

    public List<String> getLore() {
        return lore;
    }

    public int getMenuSlot() {
        return menuSlot;
    }

    public void giveKit(Player player) {
        kitGiver.accept(player);
    }
}