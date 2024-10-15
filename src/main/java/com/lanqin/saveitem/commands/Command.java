package com.lanqin.saveitem.commands;

import com.lanqin.saveitem.Main;
import com.lanqin.saveitem.mysql.DatabaseManager;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class Command implements CommandExecutor {
    private final DatabaseManager databaseManager;
    private final File file;
    private final YamlConfiguration config;
    private final Main main;

    public Command(DatabaseManager databaseManager, Main main) {
        this.databaseManager = databaseManager;
        this.main = main;
        this.file = new File(main.getDataFolder(), "saveitem.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String s, String[] args) {
        if (args.length == 0 || (args.length == 1 && "help".equalsIgnoreCase(args[0]))) {
            showHelp(sender);
            return true;
        }

        if (args.length == 1 && "reload".equalsIgnoreCase(args[0])) {
            reloadConfig(sender);
            return true;
        }

        if (args.length == 3 && "giveplayer".equalsIgnoreCase(args[0])) {
            giveItemToPlayer((Player) sender, args[1], args[2]);
            return true;
        }

        if (args.length == 2 && ("save".equalsIgnoreCase(args[0])) || "give".equalsIgnoreCase(args[0]) || "delete".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c此命令仅限玩家使用");
            } else if (args.length == 2) {
                if ("save".equalsIgnoreCase(args[0])) {
                    saveItem((Player) sender, args[1]);
                    return true;
                } else if ("give".equalsIgnoreCase(args[0])) {
                    giveItem((Player) sender, args[1]);
                    return true;
                } else {
                    deleteItem((Player) sender, args[1]);
                    return true;
                }
            }
        }

        if (args.length == 1 && ("save".equalsIgnoreCase(args[0])) || "give".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§c此命令仅限玩家使用");
                return true;
            } else {
                sender.sendMessage("§c请输入物品的保存名!");
                return true;
            }
        } else if (args.length == 1 && "delete".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§c请输入你要删除的保存名");
        }

        if (args.length == 1 && "list".equalsIgnoreCase(args[0])) {
            List<String> savedItems = getSavedItemNames();
            if (savedItems.isEmpty()) {
                sender.sendMessage("§a当前没有已保存的物品。");
            } else {
                sender.sendMessage("§a已保存的物品列表：");
                for (String itemName : savedItems) {
                    sender.sendMessage(" - " + itemName);
                }
            }
            return true;
        }
        return false;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("§a-------§bSaveItem§a-------");
        sender.sendMessage("§a /saveitem save <name> 保存物品");
        sender.sendMessage("§a /saveitem give <name> 给与自己物品");
        sender.sendMessage("§a /saveitem giveplayer <name> <player> 给其他玩家物品");
        sender.sendMessage("§a /saveitem delete <name> 删除物品");
        sender.sendMessage("§a /saveitem list 浏览已保存物品");
        sender.sendMessage("§a /saveitem reload 重载配置文件");
    }

    private void reloadConfig(CommandSender sender) {
        main.reloadConfig();
        sender.sendMessage("§a配置文件已重新加载！");
    }

    private void saveItem(Player player, String savename) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage("§c你没有手持物品！");
            return;
        }

        if (useMySQL()) {
            saveItemToDatabase(player, savename, item);
        } else {
            saveItemToYaml(player, savename, item);
        }
    }

    private void saveItemToDatabase(Player player, String savename, ItemStack item) {
        String sql = "INSERT INTO saveitem (name, data) VALUES (?, ?) ON DUPLICATE KEY UPDATE data = VALUES(data)";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, savename);
            preparedStatement.setString(2, serializeItem(item));
            preparedStatement.executeUpdate();
            player.sendMessage("§a物品§7[" + savename + "§7]§a已保存至数据库");
        } catch (SQLException | IOException e) {
            handleException(player, "保存物品时发生错误", e);
        }
    }

    private void saveItemToYaml(Player player, String savename, ItemStack item) {
        config.set("items." + savename, item);
        try {
            config.save(file);
            player.sendMessage("§a物品§7[" + savename + "§7]§a已保存");
        } catch (IOException e) {
            handleException(player, "物品保存时出错", e);
        }
    }

    private void giveItem(Player player, String savename) {
        if (useMySQL()) {
            giveItemFromDatabase(player, savename);
        } else {
            giveItemFromYaml(player, savename);
        }
    }

    private List<String> getSavedItemNames() {
        List<String> itemNames = new ArrayList<>();
        // 从YAML文件获取物品名称
        if (!main.getConfig().getBoolean("mysql.use-mysql")) {
            itemNames.addAll(config.getConfigurationSection("items").getKeys(false));
        } else {
            // 如果使用MySQL，这里应该进行查询以获取物品名称
            try (Connection connection = databaseManager.getConnection()) {
                String sql = "SELECT name FROM saveitem";
                try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                     ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        itemNames.add(resultSet.getString("name"));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace(); // 可以在这里处理异常
            }
        }
        return itemNames;
    }

    private void giveItemFromDatabase(Player player, String savename) {
        String sql = "SELECT data FROM saveitem WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, savename);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String itemData = resultSet.getString("data");
                ItemStack item = deserializeItem(itemData);
                player.getInventory().addItem(item);
                player.sendMessage("§a物品§7[" + savename + "§7]§a已获取");
            } else {
                player.sendMessage("§a物品§7[" + savename + "§7]§a不存在");
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            handleException(player, "获取物品时发生错误", e);
        }
    }

    private void giveItemFromYaml(Player player, String savename) {
        ItemStack item = config.getItemStack("items." + savename);
        if (item != null) {
            player.getInventory().addItem(item);
            player.sendMessage("§a物品§7[" + savename + "§7]§a已获取");
        } else {
            player.sendMessage("§a物品§7[" + savename + "§7]§a不存在");
        }
    }

    private void giveItemToPlayer(Player sender, String savename, String targetPlayerName) {
        Player targetPlayer = sender.getServer().getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            sender.sendMessage("§c玩家§7[" + targetPlayerName + "§7]§c不在线！");
            return;
        }

        if (useMySQL()) {
            giveItemFromDatabase(targetPlayer, savename);
        } else {
            giveItemFromYaml(targetPlayer, savename);
        }
    }

    private void deleteItem(Player player, String savename) {
        if (useMySQL()) {
            deleteItemFromDatabase(player, savename);
        } else {
            deleteItemFromYaml(player, savename);
        }
    }

    private void deleteItemFromDatabase(Player player, String savename) {
        String sql = "DELETE FROM saveitem WHERE name = ?";
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, savename);
            int rowsAffected = preparedStatement.executeUpdate();
            if (rowsAffected > 0) {
                player.sendMessage("§a物品§7[" + savename + "§7]§a已删除");
            } else {
                player.sendMessage("§a物品§7[" + savename + "§7]§a不存在");
            }
        } catch (SQLException e) {
            handleException(player, "删除物品时发生错误", e);
        }
    }

    private void deleteItemFromYaml(Player player, String savename) {
        if (config.contains("items." + savename)) {
            config.set("items." + savename, null);
            try {
                config.save(file);
                player.sendMessage("§a物品§7[" + savename + "§7]§a已删除");
            } catch (IOException e) {
                handleException(player, "物品删除时出错", e);
            }
        } else {
            player.sendMessage("§a物品§7[" + savename + "§7]§a不存在");
        }
    }

    private String serializeItem(ItemStack item) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream bukkitObjectOutputStream = new BukkitObjectOutputStream(byteArrayOutputStream)) {
            bukkitObjectOutputStream.writeObject(item);
            return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
        }
    }

    private ItemStack deserializeItem(String itemData) throws IOException, ClassNotFoundException {
        byte[] decodedData = Base64.getDecoder().decode(itemData);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decodedData);
             BukkitObjectInputStream bukkitObjectInputStream = new BukkitObjectInputStream(byteArrayInputStream)) {
            return (ItemStack) bukkitObjectInputStream.readObject();
        }
    }

    private boolean useMySQL() {
        return main.getConfig().getBoolean("mysql.enable");
    }

    private void handleException(CommandSender sender, String message, Exception e) {
        sender.sendMessage("§c" + message + "：" + e.getMessage());
        e.printStackTrace();
    }


}
