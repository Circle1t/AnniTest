# AnniTest



## 项目简介

> 嘿！好久不见! 最近一直在为别的事情而忙活，恰好遇到假期闲来无事便开始再次更新这个插件。
>
> 这是一个重大更新！为插件添加了地图选择模块，请你认真阅读文档了解用法，很简单的！

借助AI大模型从零开始复现核心战争插件。

项目已经实现了核心战争大部分游戏功能。

我希望重新开发的核心战争能有自己的特色，后期会逐步增加传统核心战争不存在的内容



* 作者是第一次制作并发布开源项目，这也是我的第一个mc插件，经验不足请多包涵。由于前期未规划项目结构，基本上是走一步看一步，导致项目结构与代码有点混乱，但我已经在尽力优化。

* 开发过程中没有参考Anni插件源代码。

  

## 基本信息



- **名称**：AnniTest
- **版本**：`1.0-SNAPSHOT`
- **主类**：`cn.zhuobing.testPlugin.AnniTest`
- **API 版本**：`1.20`
- **项目管理工具**：`Maven`



## 快速开始



✔插件打包方式：Maven 生命周期 package

🆗将target目录下的 AnniTest-1.0-SNAPSHOT.jar 文件移动到服务器根目录plugins文件夹中

❗<span style="color:red">注意：我在pom文件中配置了自己的plugins文件目录，你需要将它修改为自己的服务器plugins目录</span> 配置此文件的目的是：每次package不用手动移动jar包到plugins中，只需在服务端reload即可

🚀**开发风格**：

* 实现一个包一般需要写三个类 Manager Command Listener ，降低代码耦合
* Command类需要实现预定义的CommandHandler接口
* 所有Manager、Command、Listener 和 职业 都需要在AnniTest主类中注册才能生效
* 已将职业框架搭建完成 职业开发请参考已写好的职业类

🔋**特殊说明**
* 有少量职业的实现方式可能会参与到其他类中
* 地图添加方式请参考地图模块部分说明

## 指令说明



### `/team`



- **描述**：队伍相关指令 / 团队重生点相关指令。
- 使用示例：`/team respawn red` 或 `/team respawn lobby` 可以添加多个重生点。
- 注意：玩家会在多个重生点中随机复活
- **用法**：`/team <red/yellow/blue/green/random/leave>/<respawn/respawncancel>`



### `/nexus`



- **描述**：管理队伍核心。
- 注意：这个模块为了防止误修改并未实现自动保存功能，你需要设置好地图后手动 /nexus save
- 用法: /nexus set <队伍>
  或 /nexus sethealth <队伍> <血量>
  或 /nexus setborder <队伍> <first/second>
  或 /nexus remove <队伍> 或 /nexus save
- **用法**：`/nexus <set/sethealth/setborder/remove/save>`



### `/annistart`



- **描述**：游戏启动。
- **用法**：`/annistart`



### `/phase`



- **描述**：设置游戏阶段。
- **用法**：`/phase set <阶段编号>`



### `/diamond`



- **描述**：将准星对准方块设置为钻石重生点。
- 注意：钻石只会在阶段三开始出现，在之前会以圆石形态存在
- **用法**：`/diamond <cancel>`



### `/getteamstar`



- **描述**：获得队伍选择之星。
- **用法**：`/getteamstar`



### `/compass`



- **描述**：获得核心指南针。
- **用法**：`/compass`



### `/boss`



- **描述**：boss 相关指令，包括 `tp`（团队 boss 点 tp 位置）、`set`（boss 重生点）、`spawn`（手动刷新 boss）、`clear`（清除 boss）。
- **用法**：`/boss <tp/set/spawn/clear> <队伍名称/null>`



### `/store`



- **描述**：告示牌商店 <酿造/武器/移除>
- **用法**：`/store <brew/weapon/remove>`



### `/kl /suicide`



- **描述**：自杀 同/kill效果（通过新指令绕过权限问题）
- **用法**：`/kl /suicide`


### `/witch`
- **描述**：设置队伍女巫重生点
- **用法**：`/witch <set/remove> <队伍>`

### `/annimap`
- **描述**：游戏地图设置 <边界设置>/<设置地图底部 将会创建基岩并舍去基岩下方方块>/<设置地图映射名（文件夹名->地图名）>/<设置地图图标>/<离开当前地图配置>
- **用法**：`/annimap <setborder 1/2/3/4>/<setundersurface>/<setmapname 地图名>/<setmapicon Material枚举类型（例如STONE）>/<leave>`


## TODO

**配置文件更新**：目前正在做配置文件统一化整理工作

**指令优化**：正在逐步添加指令tab填充功能

**Bug 修复**：游戏内仍然存在不少 BUG，但大部分已经被修复，后续会持续排查和解决。

**职业系统开发**：添加后续职业。





## 地图模块使用方式
1. 在插件plugins目录下，插件在第一次启动后会自动生成AnniTest文件夹，其中有4个文件 `maps,lobby-config.yml,maps-config.yml,boss-config.yml,anni-config.yml`
2. 你需要在maps文件夹中放入你的地图文件，注意`uid.dat`如果存在需要删除
3. 打开`maps-config.yml`,`lobby-config.yml`，`boss-config.yml`并按照如下格式配置:

>注意：第一次使用大厅地图请手动配置大厅重生点！你只需要修改模板上的x,y,z复制粘贴到yml文件里即可。
> 
>建议进入大厅后在游戏内使用指令`/lobby respawn`指令再次配置大厅重生点
> 
> 请不要配置大厅重生点的world属性！否则可能出现大厅传送失败的bug！
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
# lobby-config.yml
lobbyMap: "defaultLobby"  # 大厅地图模板名称（需存放在 plugins/插件名/maps/ 目录下）
respawnPoints:            # 大厅重生点坐标列表（自动生成，请勿手动编辑）
  '0':                      # 第一个重生点，可以添加多个
    x: 100.5              # X坐标（你可以在本地游戏中获取 X Y Z 坐标，并配置在此处）
    y: 64.0               # Y坐标
    z: 200.5              # Z坐标
    yaw: 0.0              # 水平朝向角度
    pitch: 0.0            # 垂直俯仰角度


```

```
# boss-config.yml
bossMap: "BossTemplate"    # Boss地图模板名称（需存放在 plugins/插件名/maps/ 目录下）
bossSpawn:                 # Boss生成点坐标（建议参数都在游戏里配置）
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
## 反馈



如果你在使用过程中遇到任何问题或有建议，请通过以下方式反馈：



- 在 GitHub 仓库的 Issues 板块提交问题。
- 联系作者的 BiliBili 账号。📺BiliBili：[烧烤蒸馏水](https://space.bilibili.com/293990463)



## 开源协议



本项目基于 MIT 协议开源，你可以免费将其用于学习交流和游戏体验。使用此项目时，请务必保留作者声明。本项目开发过程中借助了 AI 辅助。



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