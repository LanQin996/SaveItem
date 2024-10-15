package com.lanqin.saveitem;

import com.lanqin.saveitem.commands.Command;
import com.lanqin.saveitem.event.TabEvent;
import com.lanqin.saveitem.mysql.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        boolean useMySQL = getConfig().getBoolean("mysql.enable");
        if (useMySQL) {
            String drive = getConfig().getString("mysql.drive");
            String host = getConfig().getString("mysql.host");
            int port = getConfig().getInt("mysql.port");
            String database = getConfig().getString("mysql.database");
            String username = getConfig().getString("mysql.username");
            String password = getConfig().getString("mysql.password");

            databaseManager = new DatabaseManager(drive , host , port , database, username , password);
            databaseManager.connect();
            getLogger().info("§a 数据库连接正常");
            databaseManager.createDatabase();
        }

        Bukkit.getPluginCommand("saveitem").setExecutor(new Command(databaseManager, this));
        Bukkit.getPluginCommand("saveitem").setTabCompleter(new TabEvent(this, databaseManager));

        getLogger().info("§b--------§aSaveItem§b--------");
        getLogger().info("§e作者" + this.getDescription().getAuthors());
        getLogger().info("§e版本" + this.getDescription().getVersion());
        getLogger().info("§e插件已完成加载");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (databaseManager != null){
            databaseManager.disconnect();
        }
        getLogger().info("§b--------§aSaveItem§b--------");
        getLogger().info("§e作者" + this.getDescription().getAuthors());
        getLogger().info("§e版本" + this.getDescription().getVersion());
        getLogger().info("§e插件已成功加载");
    }
}
