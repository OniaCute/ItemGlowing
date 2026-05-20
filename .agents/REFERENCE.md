# Item Growing
该项目名为"Item Glowing"是由"Onia"(www.oniacute.cc)编写的一个修改掉落物的插件, 插件将运行在1.21.11的Paper服务端上.

## 功能
该插件包含以下功能:
1. 掉落物发光
2. 掉落物NameTag, 可以在配置文件自定义样式, 物品名称显示中文.
3. 掉落物计时器, 倒计时结束自动掉落物自动消失
4. 文字输出和nametag显示要支持"&(代替mc原版的颜色格式化字符)"和"&#"等mc内部的颜色格式化字符和十六进制RGB颜色显示.
5. 按物品品质,稀有程度决定物品的显示颜色.

特殊要求:
1. 确保不会对服务器有太大影响
2. 不允许存在内存泄露问题, 也不允许占用过多内存和CPU资源
3. 尝试使用Async来处理以避免导致服务器滞后 (如果有更好的方法可以忽略这项)

## 命令
1. `/itemglowing add <方块ID>` 将某个方块添加到忽略名单, 权限节点`itemglowing.admin`
2. `/itemglowing remove <方块ID>` 将某个方块从名单种移除, 权限节点`itemglowing.admin`
3.  `/itemglowing reload` 重载配置文件, 权限节点`itemglowing.admin`
3.  `/itemglowing setrarity <方块ID> common/uncommon/...` 设置物品品质覆盖, 权限节点`itemglowing.admin`
4. `/itemglowing timeset 品质 <int数字>` 将某个品质的掉落物的消失时间计时器变为指定数, 单位为秒, 权限节点`itemglowing.admin`


## 配置文件
一个yml文件, 所有项都可以在此编辑, 同时允许热加载和自动保存:
```yml

# 是否发光
glowing: true

# 物品发光颜色
commonColor: '#ffffff'
uncommonColor: '#fff6a7'
rareColor: "#2dabff"
epicColor: "#dc67ff"
legendColor: "#ffb727"


# 显示的名牌样式, 如果为""则不显示.
nametag: "{rarityColor}{item} &fx &b{amount} &7| &#6cd3ff{time}s"

# 不同品质的物品消失时间, 单位为秒
despawnCommon: 150
despawnUncommon: 260
despawnRare: 300
despawnEpic: 400
despawnLegend: 500

# 物品消失黑烟效果
despawnEffect: true

# 物品消失效果持续时长, 单位为Tick
despawnEffectDuration: 1

# 物品消失效果高度偏移量
despawnEffectOffset: 0.3

# 只有物品半径内有玩家才显示glow和nametag, 如果为-1则始终显示
radius: -1

# 检测时间间隔, 单位为Tick, 最低为1即每tick都检测
detectTimer: 2

# 禁用的世界
disabledWorlds: [
    the_end_world # 末地
]

# 语言项
messages: {
    "prefix": "&#6cd3ffItemGlowing &8»",
    "noPromission": "{prefix} &c你无权执行该操作!",
    "invalid": "{prefix} &c错误的用法! 用例:add/remove/reload [item]",
    "added": "{prefix} &a物品 &e{item} &a已经被添加到忽略名单.",
    "removed": "{prefix} &c物品 &e{item} &c已经被移出忽略名单.",
    "timeset": "{prefix} &a品质为 &e{rarity} &a的物品消失时间已设置为 &e{time} &a.",
    "rarityset": "{prefix} &a物品 &e{item} &a的品质已设置为 &e{rarity} &a.",
    "reloaded": "{prefix} &a插件已重载,有 &e{items} &a个项目被忽略.",
    "error": "{prefix} &c重载失败, 请检查配置文件是否正确!"
}

# 忽略项名单,在此名单中不glow和显示nametag
ignored: [
    "NETHERRACK"
]

# 自定义品质名单
rarity: {
    "END_CRYSTAL": "uncommon"
}

```