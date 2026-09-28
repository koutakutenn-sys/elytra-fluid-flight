# Elytra Fluid Flight

为 **Minecraft Java 26.2 / Fabric Loader 0.19.5 / Java 25** 制作的水下、岩浆鞘翅飞行模组。

## 安装

1. 将 `elytra-fluid-flight-1.0.0+mc26.2.jar` 放入对应游戏实例的 `mods` 文件夹。
2. 启动 Minecraft 26.2，使用 Fabric Loader 0.19.5（模组声明允许更高 Loader 版本，但验证版本为 0.19.5）。
3. 无需额外安装 Fabric API、Cloth Config 或 Mod Menu。
4. 单人游戏安装到客户端即可。多人游戏的**客户端与服务器均需安装，并使用相同配置**；本版本没有自动同步配置。

这是 Fabric 模组，不适用于 Forge、NeoForge、基岩版或其他 Minecraft 版本。

## 功能与操作

- 穿着可用鞘翅滑翔进入水或岩浆时，继续使用鞘翅移动逻辑。
- 已在液体中时，离开地面后按跳跃键，也可以展开鞘翅。站在水底或岩浆底部时，先浮起再按一次跳跃。
- 滑翔时右键使用烟花，水下和岩浆中仍可推进；保留原版烟花消耗和飞行时间。
- 水中默认额外阻力倍率 `0.6`，岩浆默认 `0.35`。
- 可启用“必须有耐火效果才能在岩浆中飞行”；耐火消失时会结束岩浆中的滑翔。
- 保留鞘翅耐久消耗、落地收翼、漂浮效果限制、梯子限制和原版装备检查。
- 保留溺水、岩浆灼伤及烟花爆炸伤害；这个模组不赋予水下呼吸或耐火效果。

## 配置

首次启动自动生成实例目录下的 `config/elytra_fluid_flight.json`：

```json
{
  "waterSpeedMultiplier": 0.6,
  "lavaSpeedMultiplier": 0.35,
  "lavaRequiresFireResistance": false
}
```

| 配置项 | 默认值 | 含义 |
| --- | --- | --- |
| `waterSpeedMultiplier` | `0.6` | 水中鞘翅移动的额外速度保留比例 |
| `lavaSpeedMultiplier` | `0.35` | 岩浆中鞘翅移动的额外速度保留比例 |
| `lavaRequiresFireResistance` | `false` | 改为 `true` 后要求实际的耐火药水效果 |

倍率范围为 `0 < 倍率 <= 1`。这是**每个游戏刻**对原版鞘翅物理计算后的速度向量乘以该倍率，不是固定的最高速度比例。
数值越小，阻力越大；`1.0` 表示没有额外阻力，原版鞘翅空气阻力仍存在。
默认 `0.6`、`0.35` 会很快减速，适合配合烟花；若希望惯性滑翔更久，可尝试 `0.9`、`0.8`。
液体交界同时检测到水和岩浆时，优先采用岩浆设置。

配置在启动时读取，修改后重启客户端和服务器。非法倍率回退到对应默认值；格式损坏时使用默认配置并记录日志，保留原文件便于修正。

## 构建源码

需要 JDK 25。项目自带 Gradle 9.5.1 Wrapper，首次构建需要网络下载依赖。

```sh
# macOS / Linux
chmod +x gradlew
./gradlew build
```

```bat
:: Windows
gradlew.bat build
```

安装 `build/libs/elytra-fluid-flight-1.0.0+mc26.2.jar`，不要安装名称带 `sources` 的 JAR。
源码使用 Loom 1.17.21 和 26.2 原版类名，无需 Yarn 映射。

## 集成测试

测试使用实际 Minecraft 26.2 + Fabric Loader 0.19.5 服务端和已应用 Mixin 的游戏类，在独立临时测试世界中执行。
测试玩家由 `Player` 派生，读取实际水/岩浆方块，并调用原版移动、烟花和装备逻辑。
测试模组独立存放在 `src/integrationTest`，不会进入发行 JAR。

在已阅读并接受 Minecraft EULA 的情况下运行：

```sh
./gradlew runIntegrationTest -PacceptMinecraftEula=true
```

测试服务器仅监听 `127.0.0.1`，使用动态端口并在完成后退出；不会使用现有游戏存档。
结果写入 `run-test/test-result.txt`。47 项检查覆盖：

- 配置创建、部分配置、越界/无穷值、格式损坏及原文件保留。
- 空中起飞，两种液体中的起飞、入水保持滑翔、离开液体恢复空气物理。
- 默认与自定义阻力倍率、滑翔时不切换游泳姿态。
- 两种液体中的原版烟花推进与每次消耗一枚。
- 落地、未穿鞘翅、鞘翅损坏、漂浮效果和耐久消耗。
- 耐火条件开启、获得/失去耐火效果，以及水中行为不受该选项影响。

构建与上述 47 项服务端检查已通过。尚未进行图形客户端手动操控或完整整合包兼容性测试。
发布前建议人工复核：持续滑翔进出液面、不同俯仰角下连续使用烟花、流动液体/气泡柱，以及联机双方配置相同的情况。
修改玩家移动或鞘翅物理的其他模组，以及服务器反作弊插件，可能需要单独做兼容性验证。

## 实现说明

仅对玩家生效。通过 `shouldTravelInFluid` 将正在液体中滑翔的玩家送入原版鞘翅移动分支；
在 `updateFallFlyingMovement` 返回时施加倍率；局部放开水中起飞检查，并防止冲刺游泳姿态覆盖滑翔。
保持真正的液体检测，不全局伪装为空气，因此呼吸、灼烧和液体相关逻辑仍能工作。
原版烟花在 `isFallFlying()` 成立时就会推进，因此无需改写烟花实体或增加物品。

开发参考：[Fabric 26.2 官方开发说明](https://fabricmc.net/2026/06/15/262.html)、[Fabric Loader 0.19.5 官方仓库](https://maven.fabricmc.net/net/fabricmc/fabric-loader/0.19.5/)。

许可证：MIT。
