# AnniTest



## 项目简介



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



## TODO



1. **boss 功能完善**：原版bossbar问题还未解决。
2. **Bug 修复**：游戏内仍然存在不少 BUG，但大部分已经被修复，后续会持续排查和解决。
3. **职业系统开发**：添加后续职业。
4. **代码优化**



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