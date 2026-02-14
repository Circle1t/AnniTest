# AnniTest

欢迎关注我的个人博客：[Circle1t](https://circle1t.top)

本项目已进入维护期，将会适当更新。

欢迎给我点个⭐！这样我会更有动力进行更新！

## 项目简介

> 项目已在 梦天涯（公益服 IP: tiancraft.cn） 服务器开放测试

> 项目已在 梦想天堂（IP: mc244.com） 服务器开放测试

借助AI大模型从零开始复现核心战争插件。

项目已经实现了核心战争的90%游戏功能。

我希望重新开发的核心战争能有自己的特色，后期会逐步增加传统核心战争不存在的内容

[【从0开始实现核心战争】进度展示 #1](https://www.bilibili.com/video/BV1BwKpeaEj6/)

[【从0开始实现核心战争】进度展示 #2](https://www.bilibili.com/video/BV1c6PYe8Eu4/)

[【从0开始实现核心战争】进度展示 #3](https://www.bilibili.com/video/BV19A5vzMEVh/)

[【从0开始实现核心战争】进度展示 #4](https://www.bilibili.com/video/BV1UKuFzDEfY)

* 作者是第一次制作并发布开源项目，这也是我的第一个mc插件，经验不足请多包涵。

  

## 基本信息



- **名称**：AnniTest
- **版本**：`1.0-SNAPSHOT`
- **主类**：`cn.zhuobing.testPlugin.AnniTest`
- **API 版本**：`1.20-1.21`
- **JDK** ：`22`
- **项目管理工具**：`Maven`



## 快速开始

### 开发基础环境配置

✔插件打包方式：Maven 生命周期 package

🆗将target目录下的 AnniTest-1.0.jar 文件移动到服务器根目录plugins文件夹中

❗<span style="color:red">注意：我在pom文件中配置了自己的plugins文件目录，你需要将它修改为自己的服务器plugins目录</span> 配置此文件的目的是：每次package不用手动移动jar包到plugins中，只需在服务端reload即可

🚀**开发风格**：

* 实现一个包一般需要写三个类 `Manager` `Command` `Listener` ，降低代码耦合
* Command类需要实现预定义的`CommandHandler`接口
* 所有`Manager`、`Command`、`Listener` 和 职业 都需要在`AnniTest`主类中注册才能生效
* 已将职业框架搭建完成 职业开发请参考已写好的职业类

🔋**特殊说明**
* 有少量职业的实现方式可能会参与到其他类中
* 地图添加方式请参考地图模块部分说明

------

### 插件使用基础配置

#### 1. 插件部署

将编译好的 AnniTest jar 包放置于服务端的 plugins 目录下。

#### 2. 初始文件生成与服务端关闭

插件首次启动后，会在 plugins 目录下自动生成 `AnniTest` 文件夹，该文件夹包含 5 个文件：`maps`、`lobby-config.yml`、`maps-config.yml`、`boss-config.yml` 和 `anni-config.yml`。待插件成功启动后，关闭服务端，准备进行地图基础配置。

#### 3. 地图基础配置
**⚠ 务必注意：需要打开地狱门、末地门权限。否则无法更换职业和进入BOSS点！！！**

请参照文档中 “**地图模块使用方式**” 进行lobby和boss地图基础配置。

需要注意的是，**必须先完成大厅地图的手动配置，才能进入服务器**。

在添加至少一张**未配置地图**（originalMap）后，方可开始后续配置。

#### 4. 服务器启动与地图配置器获取

启动服务器，以管理员（OP）身份登录，此时将获得一个地图配置器。使用该地图配置器选择某张地图，进入游戏地图配置流程。

#### 5. 游戏地图详细配置

以下是具体的配置指令及对应配置内容：

- **队伍重生点配置**：使用 `/team` 指令，可根据需求添加多个重生点。
- **队伍核心配置**：参照 `/nexus` 指令部分进行操作。
- **队伍核心保护区域配置**：同样依据 `/nexus` 指令部分完成设置。（注意：Nexus部分需要手动保存！）
- **商店配置**：按照 `/store` 指令部分的说明进行配置。
- **末影高炉配置**：按照 `/enderfurnace` 指令部分的说明进行配置。
- **队伍女巫重生点配置**：使用 `/witch` 指令完成相关配置。
- **钻石生成点配置**：依据 `/diamond` 指令部分进行设置。
- **地图边界配置**：按照 `/annimap` 指令部分的要求完成配置。
- **地图图标与名称等内容配置**：使用 `/annimap` 指令完成地图图标、名称等信息的配置。
- **config.yml配置**：确保相关config内容符合你的要求（XP系统目前没有实现数据库配置）。

#### 6. 配置完成后返回大厅

完成上述所有配置后，输入 `/annimap leave` 指令返回大厅。

#### 7. 最终配置与服务器重启

关闭服务器，将配置好的地图添加到配置文件的游戏候选地图（gameMaps）中。再次启动服务器，插件即可按照新的配置正常运行。

⚠ 提醒：一定要打开地狱与末地的权限，否则将无法进入地狱门与Boss点！
⚠ 提醒：目前添加了XP系统但未实现数据库，建议关闭XP系统！


## 指令说明



### `/annimap`

- **描述**：用于游戏地图的各种设置，包括边界设置、地图映射名设置、地图图标设置以及离开当前地图配置。
- 用法
  - `/annimap setborder <1/2/3/4>`：设置地图边界的四个角（俯视视角的矩形四个角）。
  - `/annimap setmapname <地图名>`：设置地图的映射名（就是地图在游戏内显示的名称）。
  - `/annimap setmapicon <Material枚举类型（例如STONE）>`：设置地图的图标（投票界面中显示的图标）。
  - `/annimap leave`：离开当前地图配置。
- 使用示例
  - `/annimap setborder 1`：设置地图边界的第一个角。
  - `/annimap setmapname new_map_name`：将当前地图的映射名设置为 `new_map_name`。
  - `/annimap setmapicon STONE`：将当前地图的图标设置为石头。
  - `/annimap leave`：离开当前地图配置。
- **注意事项**：设置地图图标时，需使用有效的 `Material` 枚举类型。

### `/annihelp`

- **描述**：查看面向玩家的游戏指令帮助；使用子参数可获取教程书。
- **用法**：`/annihelp [book]`
- 使用示例
  - `/annihelp`：显示玩家常用指令（队伍、指南针、重生、教程书、全体说话等）及插件作者信息。
  - `/annihelp book`：获得一本游戏教程书（与开局发放的相同）。
- **注意事项**：仅玩家可用。

### `/annistart`

- **描述**：启动游戏。
- **用法**：`/annistart`
- **使用示例**：`/annistart`
- **注意事项**：通常只有具备管理员权限的玩家才能使用此指令。

### `/boss`

- **描述**：一系列与游戏中 boss 相关的指令，包括设置团队 boss 传送点、设置 boss 重生点、手动刷新 boss、清除 boss 以及进入和离开 boss 点。
- 用法
  - `/boss tp <队伍名称>`：设置指定队伍的 boss 传送点。
  - `/boss set`：设置 boss 重生点。
  - `/boss spawn`：手动刷新 boss。
  - `/boss clear`：清除 boss。
  - `/boss enter`：进入 boss 点。
  - `/boss leave`：离开 boss 点。
- 使用示例
  - `/boss tp red`：设置红色队伍的 boss 传送点。
  - `/boss set`：设置 boss 重生点。
  - `/boss spawn`：手动刷新 boss。
  - `/boss clear`：清除 boss。
  - `/boss enter`：进入 boss 点。
  - `/boss leave`：离开 boss 点。
- **注意事项**：只有管理员能操作。

### `/compass`

- **描述**：玩家获得核心指南针。
- **用法**：`/compass`
- **使用示例**：`/compass`
- **注意事项**：无特殊注意事项。

### `/diamond`

- **描述**：将准星对准的方块设置为钻石重生点，也可取消已设置的钻石重生点。
- **用法**：`/diamond <cancel>`
- 使用示例
  - `/diamond`：将准星对准的方块设置为钻石重生点。
  - `/diamond cancel`：取消已设置的钻石重生点。
- **注意事项**：钻石只会在阶段三开始出现，在之前会以圆石形态存在。

### `/enderfurnace`

- **描述**：末影高炉管理命令。末影高炉介绍：它是一种特殊的熔炉，每个人在里面的东西都是相互隔离的（类似末影箱），一般每个队伍只有1个，每名玩家只能打开自己队伍的末影高炉。
- **用法**：`/enderfurnace set <队伍> 或 /enderfurnace remove`
- 使用示例
  - `/enderfurnace set red`：将准星对准的高炉设置为红队末影高炉。
  - `/enderfurnace remove`：取消已设置的末影高炉。

### `/getteamstar`

- **描述**：玩家获得队伍选择之星。
- **用法**：`/getteamstar`
- **使用示例**：`/getteamstar`
- **注意事项**：无特殊注意事项。

### `/kl` `/suicide`

- **描述**：这两个指令功能相同，均为自杀指令，效果等同于 `/kill`，可绕过部分权限问题。
- **用法**：`/kl` 或 `/suicide`
- **使用示例**：`/kl` 或 `/suicide`
- **注意事项**：使用后玩家角色将死亡，请谨慎操作。

### `/lobby`

- **描述**：与游戏大厅相关的指令，目前仅支持设置大厅重生点。
- **用法**：`/lobby <respawn>`
- **使用示例**：`/lobby respawn`：设置大厅的重生点。
- **注意事项**：必须配置一个重生点，否则将无法进入服务器。

### `/nexus`

- **描述**：管理队伍核心的相关设置，包括核心位置、血量、保护区域边界的设置，以及核心的移除和设置保存。
- 用法
  - `/nexus set <队伍>`：设置指定队伍的核心位置。
  - `/nexus sethealth <队伍> <血量>`：设置指定队伍核心的血量。
  - `/nexus setborder <队伍> <first/second>`：设置指定队伍核心保护区域的对角点。
  - `/nexus remove <队伍>`：移除指定队伍的核心设置。
  - `/nexus save`：保存核心设置。
- 使用示例
  - `/nexus set red`：设置红色队伍的核心位置。
  - `/nexus sethealth red 100`：将红色队伍核心的血量设置为 100，默认值为 75。
  - `/nexus setborder red first`：设置红色队伍核心保护区域的第一个对角点。
  - `/nexus remove red`：移除红色队伍的核心设置。
  - `/nexus save`：保存所有核心设置。
- **注意事项**：此模块为防止误修改，未实现自动保存功能，设置好地图后需手动执行 `/nexus save` 保存设置。核心保护区域是一个矩形，`first/second` 应分别为两个对角点。设置核心保护区域时注意不要漏区域，否则存在挖地道直通核心风险。

### `/phase`

- **描述**：设置游戏的阶段。
- **用法**：`/phase set <阶段编号>`
- **使用示例**：`/phase set 2`：将游戏阶段设置为 2。
- **注意事项**：阶段编号必须为有效整数，且在游戏规定的阶段范围内。

### `/store`

- **描述**：用于告示牌商店的设置与管理，包括设置酿造商店、武器商店以及移除商店设置。
- 用法
  - `/store brew`：设置酿造商店告示牌。
  - `/store weapon`：设置武器商店告示牌。
  - `/store remove`：移除商店告示牌设置。
- 使用示例
  - `/store brew`：将准星对准的告示牌设置为酿造商店告示牌。
  - `/store weapon`：将准星对准的告示牌设置为武器商店告示牌。
  - `/store remove`：移除准星对准的告示牌的商店设置。
- **注意事项**：操作时需确保准星对准的是有效的告示牌。

### `/team`

- **描述**：用于队伍选择、团队重生点的设置与移除，以及管理员强制玩家入队。
- 用法
  - `/team <red/yellow/blue/green>`：选择加入指定颜色的队伍。
  - `/team random`：随机加入人数最少的队伍。
  - `/team leave`：离开当前所在队伍。
  - `/team <玩家名> <红/黄/蓝/绿>`：**管理员**强制指定玩家加入某队伍（支持中文队名 红/黄/蓝/绿 或英文 red/yellow/blue/green）。
  - `/team respawn <队伍英文名>`：为指定队伍添加重生点。
  - `/team respawnremove <队伍英文名>`：移除指定队伍的重生点设置。
- 使用示例
  - `/team red`：选择加入红色队伍。
  - `/team Steve 红`：管理员强制将玩家 Steve 加入红队。
  - `/team respawn red`：为红色队伍添加一个重生点，可多次使用以添加多个重生点。
  - `/team respawnremove red`：移除红色队伍的重生点设置。
- **注意事项**：玩家会在队伍设置的多个重生点中随机复活。`/team 玩家名 队伍` 仅 OP 可用，且可无视阶段与核心是否被摧毁。

### `/witch`

- **描述**：设置和移除队伍女巫的重生点。
- 用法
  - `/witch set <队伍>`：为指定队伍设置女巫重生点。
  - `/witch remove <队伍>`：移除指定队伍的女巫重生点。
- 使用示例
  - `/witch set red`：为红色队伍设置女巫重生点。
  - `/witch remove red`：移除红色队伍的女巫重生点。
- **注意事项**：设置的重生点位置应合理，确保女巫能够正常重生。



## 地图模块使用方式
1. 在插件plugins目录下，插件在第一次启动后会自动生成`AnniTest`文件夹，其中有5个文件 `maps,lobby-config.yml,maps-config.yml,boss-config.yml,anni-config.yml`
2. 你需要在maps文件夹中放入你的地图文件，注意地图文件中`uid.dat`如果存在需要删除
3. 打开`maps-config.yml`,`lobby-config.yml`，`boss-config.yml`，`anni-config.yml`，并按照如下格式配置:

>**注意：**
>
>[1]自动生成的 yml 文件已经自动置入了模板，你只需要根据注释修改即可。这里的模板文件不一定最新，请参考自动生成的模板文件。
>
>[2]boss地图配置需要输入指令进入 /boss enter
>
>**⚠大厅配置须知：**
>
>[1]第一次使用大厅地图请手动配置大厅重生点，否则会被服务器提示`大厅传送失败`踢出！
>
>[2]对于大厅模板你只需要修改模板上重生点的x,y,z即可，其余内容非必须配置。
>
>[3]建议进入大厅后在游戏内使用指令`/lobby respawn`指令再次配置大厅重生点，你可以把默认重生点从配置文件中删除
>
>[4]请不要配置大厅重生点的world属性！否则可能出现大厅传送失败的bug！
```
# lobby-config.yml
lobbyMap: "defaultLobby"  # 大厅地图模板名称（需存放在 plugins/插件名/maps/ 目录下）
respawnPoints:            # 大厅重生点坐标列表（自动生成，请勿手动编辑）
  '0':                    # 第一个重生点，可以添加多个
    x: 100.5              # X坐标（你可以在本地游戏中获取 X Y Z 坐标，并配置在此处）
    y: 64.0               # Y坐标
    z: 200.5              # Z坐标
    yaw: 0.0              # 水平朝向角度
    pitch: 0.0            # 垂直俯仰角度
```

```
# maps-config.yml
gameMaps:                 # 游戏候选地图列表（文件夹名称，需存放在 plugins/插件名/maps/ 目录下）
  - "map1"                # 地图模板1
  - "map2"                # 地图模板2

originalMaps:             # 原始地图列表（还未被配置的地图）
  - "original_map1"       # 原始地图1
  - "original_map2"       # 原始地图2

mapIcons:                 # 地图投票图标配置
  map1: GRASS_BLOCK       # 地图1的展示材质（必须使用有效的材质名称）
  map2: NETHERRACK        # 地图2的展示材质

mapFolderNameMapping:     # 地图文件夹名与显示名称映射
  map1: "草原地图"         # 显示在投票界面的地图名称
  map2: "地狱岩地图"
```

```
# boss-config.yml
bossMap: "BossTemplate"    # Boss地图模板名称（需存放在 plugins/插件名/maps/ 目录下）
bossSpawn:                 # Boss生成点坐标（建议参数都在游戏里配置,你只需要配置好地图名称即可）
  world: AnniBoss          # 必须为 AnniBoss 世界
  x: 100.5
  y: 64.0
  z: 200.5
  yaw: 0.0
  pitch: 0.0
teamTpLocations:           # 队伍传送点坐标
  red:                     # 红队传送点
    world: AnniBoss
    x: -150.5
    y: 64.0
    z: 300.5
    yaw: 90.0
    pitch: 0.0
  blue:                    # 蓝队传送点
    world: AnniBoss
    x: 200.5
    y: 64.0
    z: -100.5
    yaw: -90.0
    pitch: 0.0
```

```
# anni-config.yml - 核心配置文件  更新中

# 游戏设置
settings:
  min-players-to-start: 4 # 启动游戏需要的最小玩家数
  boss-health: 500 # Boss的基础血量

# 路径配置
paths:
  map-config-folder: AnniMapConfig # 地图配置文件夹名称
  
```
4. 请以管理员身份进入服务器，你将会获得一个地图配置器，通过它你可以进入选中的地图进行属性配置。配置过程请参照指令目录。
5. boss地图配置需要输入指令进入 /boss enter
## TODO

**配置文件更新**：目前正在做配置文件统一化整理工作。

**Bug 修复**：游戏内仍然存在不少 BUG，但大部分已经被修复，后续会持续排查和解决。

**职业系统开发**：添加后续职业。

**游戏玩法开发**：将把核心战争变成一个多元化的游戏。



## 反馈



如果你在使用过程中遇到任何问题或有建议，请通过以下方式反馈：



- 在 GitHub 仓库的 Issues 板块提交问题。
- 联系作者的 BiliBili 账号。📺BiliBili：[烧烤蒸馏水](https://space.bilibili.com/293990463)

不妨给我点一个Star⭐！

## 开源协议



本项目基于 MIT 协议开源，你可以免费将其用于学习交流和游戏体验。使用此项目时，**请务必保留作者声明**。本项目开发过程中借助了 AI 辅助。



### MIT 协议全文



```plaintext
MIT License

Copyright (c) 2025 灼冰/Circle1t

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```



## 