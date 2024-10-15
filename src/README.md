# SaveItem指南

## 指令

```
/saveitem save <name> 保存物品
/saveitem give <name> 给与物品
/saveitem giveplayer <name> <player>
/saveitem delete <name> 删除物品
/saveitem list 查询已保存物品
/saveitem reload 重载配置

/saveitem 指令可简写为 /si
```

---

### config.yml

```
# 数据库配置
mysql:
  # 是否使用mysql false为使用yml存储 true为mysql存储
  enable: true
  # 数据库版本8以上请填写 com.mysql.cj.jdbc.Driver
  drive: "com.mysql.jdbc.Driver"
  host: "localhost"
  port: 3306
  database: "saveitem"
  username: "root"
  password: "123456"
```

