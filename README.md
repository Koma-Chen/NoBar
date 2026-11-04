# NoBar

**保留手势线功能的同时隐藏手势线，仅在 OS4.XX 系统上验证。**

一个 84 行的 LSPosed 模块：让底部手势条（小白条）不可见，同时把系统强制占用的 56px 空间还给应用，**手势功能完整保留**。

## 效果

| 项目 | 默认 | 装 NoBar 后 |
|---|---|---|
| 底部白线 | 显示 | **不绘制** |
| `mandatorySystemGestures` | 56px | **0** |
| 应用底部留白 | 被顶起 56px | **内容延伸到底** |
| 上滑回桌面 | 可用 | **可用** |
| 上滑停住进最近任务 | 可用 | **可用** |
| 左右边缘内滑返回 | 可用 | **可用** |

实测：微信底部 tab 栏下移约 80px，Telegram、知乎、酷安、设置页底部留白全部消失。

## 原理

Android 的手势导航有两个独立的 InsetsSource：

```
type=navigationBars           frame=[0,2344][1080,2400]  visible=false   ← 手势模式下本来就不占位
type=mandatorySystemGestures  frame=[0,2344][1080,2400]  visible=true    ← 真正顶起应用的是这个
```

`mandatorySystemGestures` 是 Android 的「不可遮挡手势区」，应用读到它之后会主动在底部加 padding。

NoBar 做两件事：

1. **system_server**：hook `com.android.server.wm.InsetsSourceProvider#updateSourceFrame(Rect)`，把 `mandatorySystemGestures` 的高度压成 0
2. **SystemUI**：hook `com.android.systemui.navigationbar.gestural.NavigationHandle#draw(Canvas)`，阻止白线绘制

手势识别基于触摸坐标（`EdgeBackGestureHandler`），不依赖 inset，所以压成 0 之后手势照常工作。

## 适配

| 项目 | 值 |
|---|---|
| 系统 | HyperOS 4.0 |
| 版本号 | OS4.0.0.19.XOBCNXM |
| Android | 17 (SDK 37) |
| 框架 | LSPosed 2.2.1 / Vector（API 102 兼容） |
| 作用域 | `system` + `com.android.systemui` |

**其他版本未验证。** 升级系统后先看 LSPosed 日志里有没有 `[NoBar] inset hook installed`，没有说明 hook 点漂移了。

## 编译

```bash
export ANDROID_HOME=/path/to/Android/sdk
./gradlew assembleRelease
```

> 本项目用自己写的 Xposed API stub（`app/libs/xposed-api-stub.jar`）做 `compileOnly` 依赖。
> 原因：`de.robv.android.xposed:api:82` 已从 Maven 下架，`api.xposed.info` 返回 404。
> 运行时使用 LSPosed 提供的真实 Xposed 类，不需要打包。

## 回滚

五种方式，任选一种：

```bash
# 1. LSPosed Manager 里关闭模块开关（最快）
# 2. adb 改数据库
adb shell su -c "sqlite3 /data/adb/lspd/config/modules_config.db \
  \"UPDATE modules_state SET enabled=0 WHERE module_pkg_name='com.komachan.nobar'\""

# 3. 删除模块 hook 目录
adb shell su -c "rm -rf /data/adb/lspd/modules/0/com.komachan.nobar"

# 4. 卸载
adb uninstall com.komachan.nobar

# 5. Recovery / 安全模式
```

## 风险

- hook 点在 `system_server` 进程，如果代码异常**最坏会导致开机卡住**。所有 hook 都用 try-catch 包裹，异常只写日志不外抛。
- 底部内容延伸进手势区后，某些应用的底部按钮可能与手势冲突。实测微信、Telegram、知乎、酷安、设置页正常。
- 系统升级后 hook 点可能漂移，表现为「模块装了但没效果」，不会崩溃。

## 许可证

MIT
