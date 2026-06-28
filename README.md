# FreshwaterColorSurvival（颜色生存 + Bingo）

> 作者：淡水岛开发组 ｜ 平台：Paper 1.21.11 ｜ Java 21

每名玩家分配一种**专属颜色**，只能**破坏 / 放置 / 右键使用**与自己颜色一致的方块；
配合两队 **5x5 Bingo 连线竞速**玩法，先连成一线（横 / 竖 / 斜）的队伍获胜。

## 玩法规则

- 玩家可分配 **16 种主色**：红、橙、黄、黄绿、绿、青、淡蓝、蓝、紫、品红、粉、棕、白、浅灰、灰、黑（真·RGB 十六进制，界面用对应颜色的 ■ 显示）。
- **方块可拥有多种颜色**，取决于材质里实际出现的颜色：例如草方块 = 绿 + 棕，橡木 = 棕，白桦原木 = 黄 + 白，白桦木板 = 黄；只要方块上带一点白/黑，拥有白/黑的玩家也能交互。玩家只需拥有方块颜色中的**任意一种**即可。
- 没有方块对应颜色的玩家，**无法破坏、放置、右键交互，也无法拾取**该方块/物品。
- 两队（A 队 / B 队），每人从 16 主色中分到一种唯一颜色（同队内唯一），队伍靠不同颜色的队友互相配合。
- **平衡机制**（避免"颜色有好有坏"导致不公）：
  1. **两队颜色镜像**：两队按下标使用完全相同的一组颜色，能力对称、面对同一张卡 → 队间绝对公平。
  2. **按卡平衡**：生成 25 格卡时，把所需颜色尽量均匀摊到这组颜色上（每色约 `25÷人数` 格），并保证每格都能被在用颜色获取 → 每名玩家在卡上同等重要、且必定可完成。
  3. **择优色池**：选用的颜色都保证能在卡上拿到物品（弱色保留，靠按卡平衡与强色同等关键）。
- `config.yml` 的 `block-colors` 段内置了 **1500+ 方块**的颜色对照表（移植自 ColorBound 调色板，支持多色），可自由增删修改。
- 表中除 16 主色外，还可用 **37 个细分类别名**（如 `DARK_OAK`、`SANDSTONE`、`TEAL`、`CRIMSON_RED` 等），它们会自动归并到最接近的主色——这样既能精细标注方块，又能保证玩家不会被分到"没方块可破坏"的稀有色。
- **两阶段惩罚**
  - 宽容阶段（开局默认）：操作错误颜色仅取消 + action bar 提示。
  - 惩罚阶段：取消 + 扣血 + 随机清除背包一个物品（带冷却）。
  - 开局 **15 分钟**（可配置）后自动进入惩罚阶段；管理员可随时手动开启 / 关闭。
- 功能方块（工作台 / 熔炉 / 箱子等）默认**豁免**颜色限制，可用命令开关。
- **观战模式**：`/fwc spectate` 可切换观战，观战者不会被分队；对局进行中观战者自动进入旁观视角，对局结束后还原。
- **胜利烟花**：某队连成一线获胜时，会在获胜队伍成员位置按各自颜色连续燃放烟花庆祝。

## 安装

1. 使用 JDK 21 构建：
   ```bash
   mvn clean package
   ```
2. 将 `target/FreshwaterColorSurvival-1.0.0.jar` 放入服务端 `plugins/` 目录。
3. 重启服务器，编辑生成的 `plugins/FreshwaterColorSurvival/config.yml` 后用 `/fwc reload` 重载。

## 命令（主命令 `/fwfish-colors`，别名 `/fwc`）

| 命令 | 说明 | 权限 |
| --- | --- | --- |
| `/fwc help` | 帮助 | 所有人 |
| `/fwc about` | 插件信息 | 所有人 |
| `/fwc join <A/B>` | 加入队伍 | `fwfish-colors.play` |
| `/fwc leave` | 离开队伍 | `fwfish-colors.play` |
| `/fwc spectate` | 切换观战模式（观战者不分队，对局中为旁观视角） | `fwfish-colors.play` |
| `/fwc color` | 查看自己的颜色 | 所有人 |
| `/fwc card` | 打开 Bingo 卡 GUI | 所有人 |
| `/fwc whatcolor` | 查看视线 / 手中方块的颜色 | 所有人 |
| `/fwc team` | 查看队伍与成员颜色 | 所有人 |
| `/fwc start` | 开始对局（自动平衡未分队玩家、发色、生成卡） | `fwfish-colors.admin` |
| `/fwc stop` | 结束对局 | `fwfish-colors.admin` |
| `/fwc punish <on/off/status>` | 手动开启 / 关闭惩罚阶段、查看剩余时间 | `fwfish-colors.admin` |
| `/fwc config <...>` | 配置豁免（见下） | `fwfish-colors.admin` |
| `/fwc bypass` | 切换个人无视颜色限制 | `fwfish-colors.admin` / `.bypass` |
| `/fwc reload` | 重载配置 | `fwfish-colors.admin` |

### config 子命令

- `/fwc config sidebar <true/false>` — 开关侧边栏记分板（默认开启）
- `/fwc config utility-exempt <true/false>` — 功能方块是否豁免
- `/fwc config exempt-add <方块>` — 添加豁免方块
- `/fwc config exempt-remove <方块>` — 移除豁免方块
- `/fwc config show` — 查看当前配置

## 权限

- `fwfish-colors.admin`（默认 OP）：管理对局与配置。
- `fwfish-colors.bypass`（默认 OP）：无视颜色限制。
- `fwfish-colors.play`（默认所有人）：参与游戏。

## 配置项（`config.yml`）

- `show-action-bar`：违规时是否用 action bar 提示。
- `utility-exempt` / `exempt-materials`：功能方块豁免开关与列表。
- `punishment.auto-enable-after-minutes`：多少分钟后自动进入惩罚阶段（默认 15）。
- `punishment.damage`：惩罚扣血（默认 2.0 = 1 颗心）。
- `punishment.clear-random-item`：是否随机清除一个物品。
- `punishment.cooldown-seconds`：惩罚冷却秒数。
- `bingo-items`：Bingo 物品池（随机抽 25 个）。
- `block-colors`：方块 → 颜色 覆盖表。

## 持续集成与发布

仓库已配置 GitHub Actions：

- `.github/workflows/build.yml`：每次推送 / PR 自动用 JDK 21 构建，并上传 jar 工件。
- `.github/workflows/release.yml`：推送 `v*` 标签（或手动 `workflow_dispatch` 输入标签）时自动构建并发布 Release，附带 jar。

发布新版本：

```bash
git tag v1.0.0
git push origin v1.0.0
```

随后 Actions 会自动在 [Releases](https://github.com/FreshWaterDevTEAM/FreshwaterColorSurvival-UPEdition/releases) 生成对应版本并附上 `FreshwaterColorSurvival-1.0.0.jar`。

---

淡水岛开发组 出品。
