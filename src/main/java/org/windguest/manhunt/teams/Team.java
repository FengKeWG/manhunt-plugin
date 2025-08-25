package org.windguest.manhunt.teams;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class Team {
    Color color;
    Team opponent;
    private Inventory sharedChest;
    private String name;
    private double dragonDamage;
    private Set<Player> players = new CopyOnWriteArraySet<>();
    private String colorString;
    private String icon;
    private boolean endPortalOpened;

    public Team(String name, String colorString, String icon, Color color) {
        this.name = name;
        this.colorString = colorString;
        this.icon = icon;
        this.color = color;
        this.dragonDamage = 0;
        this.endPortalOpened = false;
        this.opponent = null;
        this.sharedChest = Bukkit.createInventory(null, 54, name + "共享背包");
    }

    public Color getColor() {
        return color;
    }

    public String getColorString() {
        return colorString;
    }

    public Team getOpponent() {
        return opponent;
    }

    public void sendBackMessage(Player player) {
        String message = colorString + "[" + icon + "] 你已恢复" + name + "！";
        player.sendMessage(message);
    }

    public void sendWinMessage(Player player) {
        String message = colorString + "[" + icon + "] " + name + "获胜！";
        player.sendMessage(message);
    }

    public Inventory getSharedChest() {
        return sharedChest;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return this.colorString + this.name;
    }

    public double getDragonDamage() {
        return dragonDamage;
    }

    public Set<Player> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public String getIcon() {
        return icon;
    }

    public boolean isEndPortalOpened() {
        return endPortalOpened;
    }

    public void setEndPortalOpened(boolean opened) {
        this.endPortalOpened = opened;
    }

    public void addPlayer(Player player) {
        this.players.add(player);
    }

    public void removePlayer(Player player) {
        this.players.remove(player);
    }

    public boolean hasPlayer(Player player) {
        return this.players.contains(player);
    }

    public void addDragonDamage(double damage) {
        this.dragonDamage += damage;
    }

    public void resetDragonDamage() {
        this.dragonDamage = 0;
    }

    public int getPlayerCount() {
        return this.players.size();
    }

    public boolean isEmpty() {
        return this.players.isEmpty();
    }

    public void clearPlayers() {
        this.players.clear();
    }

    public void resetState() {
        clearPlayers();
        resetDragonDamage();
        setEndPortalOpened(false);
        // sharedChest.clear();
    }
}
