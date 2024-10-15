package com.lanqin.saveitem.mysql;

import java.sql.*;

public class DatabaseManager {
    private String driver;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;

    private Connection connection;

    public DatabaseManager(String driver, String host, int port, String database, String username, String password) {
        this.driver = driver;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public void connect() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }
            // 加载驱动
            Class.forName(driver);
            // 建立连接
            connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password);
        } catch (SQLException e) {
            System.out.println("数据库连接失败: " + e.getMessage());
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("JDBC驱动加载失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connect();  // 重新连接
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void createDatabase() {
        String createSQL = "CREATE TABLE IF NOT EXISTS `saveitem`  (" +
                "  `id` int NOT NULL AUTO_INCREMENT," +
                "  `name` varchar(255) NOT NULL," +
                "  `data` text NOT NULL," +
                "  PRIMARY KEY (`id`)" +
                ") ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;";

        try (PreparedStatement preparedStatement = getConnection().prepareStatement(createSQL)) {
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("创建表时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
