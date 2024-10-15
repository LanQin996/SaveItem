package com.lanqin.saveitem.event;

import com.lanqin.saveitem.Main;
import com.lanqin.saveitem.mysql.DatabaseManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabEvent implements TabCompleter {

    private final Main main;

    private final DatabaseManager databaseManager;

    public TabEvent(Main main, DatabaseManager databaseManager) {
        this.main = main;
        this.databaseManager = databaseManager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String s, String[] args) {
        List<String> tab = new ArrayList<>();

        List<String> options = Arrays.asList("save","give","giveplayer","delete","reload");
        if (args.length == 1){
            String input = args[0].toLowerCase();

            for (String option : options){
                if(option.startsWith(input)){
                    tab.add(option);
                }
            }
        }

        if (args.length == 2 && ("give".equalsIgnoreCase(args[0]) || "giveplayer".equalsIgnoreCase(args[0])) || "delete".equalsIgnoreCase(args[0])){
            tab.addAll(getSaveItemName());
        }
        if (args.length == 3 && "giveplayer".equalsIgnoreCase(args[0])) {
            // si giveplayer 礼包名称 玩家名称
            for (Player onlinePlayer : sender.getServer().getOnlinePlayers()) {
                tab.add(onlinePlayer.getName());
            }
        }

        return tab;
    }

    public List<String> getSaveItemName(){
        List<String> itemnames = new ArrayList<>();
        if (!main.getConfig().getBoolean("mysql.enable")) {
            itemnames.addAll(main.getConfig().getConfigurationSection("items").getKeys(false));
        }else {
            // 如果使用MySQL，这里应该进行查询以获取物品名称
            try (Connection connection = databaseManager.getConnection()) {
                String sql = "SELECT name FROM saveitem";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                     ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        itemnames.add(resultSet.getString("name"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace(); // 可以在这里处理异常
            }
        }

//        List<String> itemnames = new ArrayList<>();
//        File file = new File(main.getDataFolder(),"saveitem.yml");
//        YamlConfiguration saveitemconfig = YamlConfiguration.loadConfiguration(file);
//        itemnames.addAll(saveitemconfig.getConfigurationSection("items").getKeys(false));
        return itemnames;
    }
}
