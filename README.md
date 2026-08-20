# AI 局域网实时相机镜像 (AI Mirror)

本项目是一个在局域网内实现两台 Android 手机相机画面实时同步与镜子镜像功能的 Android 应用。

- **核心工程目录**: [ai-mirror/](file:///Users/kele/code/ai_android_mirror/ai-mirror/)
- **详细需求与架构文档**: [ai-mirror/README.md](file:///Users/kele/code/ai_android_mirror/ai-mirror/README.md)
- **GitHub 仓库**: [https://github.com/jobverycode/ai-android-mirror](https://github.com/jobverycode/ai-android-mirror)
- **最新 Release 下载**: [AI Mirror v1.0.0 发布包](https://github.com/jobverycode/ai-android-mirror/releases/tag/v1.0.0)

## 核心特性
1. **发送端 / 接收端自由选定**：一台手机作为摄像头发送端，另一台作为镜子显示接收端。
2. **局域网自动发现与一键配对**：支持 mDNS (NSD) 和 UDP 局域网 Beacon 双重自动发现，支持扫网与直连。
3. **全功能国际化**：默认支持简体中文 (zh-CN) 与英文 (en)，支持应用内实时语言切换与跟随系统。
4. **低延迟画面流传输**：40 字节定长头部二进制传输协议，自带时间戳延迟与帧率 HUD 监控。
5. **设置中心**：可调节分辨率 (480p/720p/1080p)、目标帧率 (15~60FPS)、压缩画质、屏幕常亮及自动配对等。
6. **多设备自动部署**：支持 `./gradlew installAllDebug` 自动识别并安装至所有连接的 ADB 设备。
