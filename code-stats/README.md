# 代码统计工具

## 📁 文件说明

- `cloc-2.04.exe` - CLOC 统计工具主程序
- `simple-cloc.ps1` - 简单快速统计（推荐）
- `precise-cloc.ps1` - 精确统计（避免误排除 Java 包）
- `quick-cloc.bat` - 批处理快速统计
- `cloc-stats.bat` - 详细分模块统计

## 🚀 使用方法

### 快速统计（推荐）

```powershell
cd code-stats
.\simple-cloc.ps1
```

### 精确统计（包含 Java 代码）

```powershell
cd code-stats
.\precise-cloc.ps1
```

### 详细分模块统计

```cmd
cd code-stats
cloc-stats.bat
```

## 📊 统计范围

- **包含文件类型**：`.java`, `.ts`, `.vue`, `.js`, `.css`, `.scss`, `.html`
- **排除目录**：`node_modules`, `target`, `.git`, `.idea`, `.vscode`, `dist`, `build` 等
- **项目模块**：`homestay-backend`, `homestay-front`, `homestay-admin`

## 💡 提示

- 如果看不到 Java 文件，使用 `precise-cloc.ps1`
- 统计报告文件会生成在当前目录
- 所有脚本都已优化速度，通常几秒内完成
