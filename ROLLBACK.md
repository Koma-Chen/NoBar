# NoBar 回滚预案

## 如果 system_server 崩溃 / 开机卡住

### 方法 1：LSPosed 里禁用（最快）
开机后在 LSPosed Manager → 模块 → NoBar → 关闭"启用模块"

### 方法 2：adb 改数据库
adb shell su -c "sqlite3 /data/adb/lspd/config/modules_config.db \
  \"UPDATE modules_state SET enabled=0 WHERE module_pkg_name='com.komachan.nobar'\""

### 方法 3：删除模块目录
adb shell su -c "rm -rf /data/adb/lspd/modules/0/com.komachan.nobar"

### 方法 4：卸载 APK
adb uninstall com.komachan.nobar

### 方法 5：Recovery / 安全模式
音量下 + 电源键进 recovery，或用 KernelSU 的安全模式
