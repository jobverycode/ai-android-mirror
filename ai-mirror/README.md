# AI 镜像 (AI Mirror) - 局域网实时相机镜像系统

> **项目标识**: `ai-mirror` (所有 AI 相关项目均以 `ai-` 前缀命名)  
> **适用平台**: Android 7.0+ (API 24+)  
> **核心开发语言与技术栈**: Kotlin, Jetpack Compose, CameraX, Kotlin Coroutines & Flow, Java-WebSocket, NsdManager & UDP Beacon

---

## 1. 项目概述与需求 (Requirements & Overview)

本项目是一款专为 Android 设备设计的**双机局域网高清实时相机镜像软件**。
在同一个 Wi-Fi / 局域网环境下，两台手机配合使用：
- **手机 A（发送端 / 摄像头）**：开启前置或后置摄像头采集画面，并在局域网中以极低延迟实时推流。
- **手机 B（接收端 / 镜子显示器）**：接收手机 A 传输的视频流，提供水平镜像翻转、全屏无边框显示、低延迟指标 HUD 及拍照截图保存等功能。

### 核心功能列表
1. **双角色自由选择与无缝切换**：
   - 发送端（Sender）：采集推流、手电筒控制、前后摄像头切换、推流质量与帧率调控。
   - 接收端（Receiver）：实时渲染、水平镜像翻转（真实镜子效果）、全屏模式、画质/延迟 HUD、一键截图保存。
2. **局域网自动发现与一键配对**：
   - **双模发现机制**：采用 Android 原生 `NsdManager` (mDNS / DNS-SD `_aimirror._tcp.`) 与 **UDP 局域网广播 Beacon (8889 端口)** 双重保障，确保在任何屏蔽组播的家用/企业级路由器环境下均能 100% 自动发现设备。
   - **一键配对与直接 IP 直连**：设备列表中点击一键发起配对请求，同时支持直接输入目标 IP 与端口进行直连。
3. **国际化多语言适配 (i18n)**：
   - 默认支持**简体中文 (zh-CN)** 与**英语 (en)**，同时支持跟随系统语言。
   - 支持在 App 设置中动态切换语言，无需重启系统。
4. **完备的设置与性能调谐 (Settings)**：
   - 画质与分辨率预设：480p 标清 (极速低延迟/省流)、720p 高清 (推荐平衡)、1080p 超清。
   - 目标帧率：15 FPS, 24 FPS, 30 FPS (流畅), 60 FPS (极度流畅)。
   - 压缩质量调谐：Low (60%), Medium (80%), High (95%)。
   - 端口配置（默认 8888）、自动同意配对开关、屏幕常亮保持开关。
5. **严谨的软件架构与测试覆盖**：
   - 包含完整的核心协议编解码测试、广播报文测试、指标统计测试与网络工具测试。

---

## 2. 架构设计与网络协议规范 (Architecture & Protocols)

### 2.1 整体架构拓扑
```
+-------------------------------------------------------------------+
|                        局域网 (Wi-Fi / LAN)                        |
+---------------------------------+---------------------------------+
                                  |
            [UDP Beacon (8889) / NSD (_aimirror._tcp.)]
            <----------------- 设备自动发现 ----------------->
                                  |
     +----------------------------+----------------------------+
     |                                                         |
+----+-----------------------+            +--------------------+-------------------+
|    发送端 (Sender 手机)     |            |       接收端 (Receiver 手机)           |
+----------------------------+            +----------------------------------------+
| 1. CameraX Preview & 分析   |            | 1. WebSocket 客户端连接与配对握手         |
| 2. YUV420 to JPEG 压缩     |            | 2. 帧数据包解包与时间戳延迟计算           |
| 3. StreamPacket 二进制封装  |            | 3. 动态旋转校正与水平镜像翻转 (Mirror Flip)|
| 4. WebSocket Server (8888) | ===TCP===> | 4. Jetpack Compose Canvas 全屏平滑渲染  |
| 5. 实时性能监控与多客户端广播 |            | 5. 帧率/码率/延迟 HUD 监控与截图保存     |
+----------------------------+            +----------------------------------------+
```

### 2.2 二进制流式协议规范 (Binary Stream Protocol)
为保证极致的吞吐量与极低解析开销，视频传输与握手采用统一的二进制报文协议：

#### 报文头部结构 (固定 40 字节 Header)
| 偏移量 (Offset) | 字段名 (Field) | 长度 (Bytes) | 类型 (Type) | 说明 (Description) |
|---|---|---|---|---|
| 0 ~ 3 | Magic Header | 4 | Byte[4] | 固定为 `AIMR` (`0x41 0x49 0x4D 0x52`) |
| 4 | Version | 1 | Byte | 协议版本号，当前为 `0x01` |
| 5 | Type | 1 | Byte | 报文类型（详见下方报文类型表） |
| 6 ~ 7 | Flags | 2 | Short | 扩展保留标志位 |
| 8 ~ 15 | Sequence Number | 8 | Long | 帧序号 / 消息序号，单调递增 |
| 16 ~ 23 | Timestamp | 8 | Long | 发送端 Unix 时间戳 (ms)，用于接收端计算端到端延迟 |
| 24 ~ 27 | Width | 4 | Int | 图像宽度 (px) |
| 28 ~ 31 | Height | 4 | Int | 图像高度 (px) |
| 32 ~ 35 | Rotation | 4 | Int | 图像旋转角度 (0, 90, 180, 270) |
| 36 ~ 39 | Payload Length | 4 | Int | 载荷数据字节长度 $N$ |
| 40 ~ (39+N) | Payload Data | $N$ | Byte[N] | 载荷（JPEG 图像二进制流或 JSON 握手数据） |

#### 报文类型代码 (Packet Types)
- `0x01` (`TYPE_FRAME_DATA`): 视频帧数据报文，Payload 为压缩后的 JPEG 图像字节流。
- `0x02` (`TYPE_PAIR_REQUEST`): 配对请求报文，Payload 为 `PairRequestPayload` JSON 字符串。
- `0x03` (`TYPE_PAIR_RESPONSE`): 配对响应报文，Payload 为 `PairResponsePayload` JSON 字符串。
- `0x04` (`TYPE_HEARTBEAT_PING`): 心跳检测 Ping 报文。
- `0x05` (`TYPE_HEARTBEAT_PONG`): 心跳响应 Pong 报文。
- `0x06` (`TYPE_CONTROL_CONFIG`): 控制指令报文（用于远程调整画质、切换摄像头等）。

---

## 3. 项目目录结构说明 (Directory Structure)

```
ai-mirror/
├── app/
│   ├── build.gradle.kts                     # 应用模块构建脚本与依赖配置
│   ├── proguard-rules.pro                   # 混淆与反射保护规则
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml          # 权限（相机、网络、Wi-Fi广播等）与入口声明
│       │   ├── java/com/ai/mirror/
│       │   │   ├── AiMirrorApplication.kt   # 全局 Application，初始化持久化设置与语言环境
│       │   │   ├── data/
│       │   │   │   ├── model/               # 数据模型（DeviceRole, DiscoveredDevice, AppSettings 等）
│       │   │   │   ├── protocol/            # 二进制报文协议与编解码器 (MirrorProtocol, PacketCodec)
│       │   │   │   ├── discovery/           # NSD 发现与 UDP 广播信标管理器
│       │   │   │   ├── streaming/           # WebSocket 服务端与客户端、帧处理器、性能统计
│       │   │   │   └── repository/          # 设置持久化与状态仓库
│       │   │   ├── ui/
│       │   │   │   ├── theme/               # Material 3 主题、颜色、排版
│       │   │   │   ├── components/          # 通用组件（HUD StatsOverlay, 对话框, 权限卡片）
│       │   │   │   ├── home/                # 首页（角色选择、局域网设备扫描与一键配对）
│       │   │   │   ├── sender/              # 发送端（CameraX 预览、控制与推流）
│       │   │   │   ├── receiver/            # 接收端（实时镜像渲染、翻转、拍照截图）
│       │   │   │   ├── settings/            # 设置中心（语言切换、画质调节、端口与配对管理）
│       │   │   │   ├── navigation/          # Jetpack Compose 页面路由导航
│       │   │   │   └── MainActivity.kt      # 主 Activity（屏幕常亮、Edge-to-Edge 支持）
│       │   │   └── utils/
│       │   │       ├── LocaleHelper.kt      # 动态国际化语言上下文封装
│       │   │       └── NetworkUtils.kt      # 局域网 IP 与广播地址解析、合法性校验
│       │   └── res/
│       │       ├── values/strings.xml       # 英文默认多语言资源
│       │       ├── values-zh-rCN/strings.xml# 简体中文多语言资源
│       │       ├── values-zh/strings.xml    # 中文区域通用回退资源
│       │       ├── values/colors.xml        # 颜色资源
│       │       └── drawable/                # 矢量图标与自适应 App 图标
│       └── test/java/com/ai/mirror/         # 核心单元测试用例
│           ├── PacketCodecTest.kt           # 协议编解码与边界测试
│           ├── DiscoveryPacketTest.kt       # 广播与握手序列化测试
│           ├── StreamStatsTest.kt           # 帧率、码率与延迟计算测试
│           └── NetworkUtilsTest.kt          # IP 与端口校验工具测试
├── gradle/libs.versions.toml                # 依赖版本目录 (Version Catalog)
├── build.gradle.kts                         # 根工程构建脚本
├── settings.gradle.kts                      # 工程设置与镜像仓库配置
└── README.md                                # 本项目完整需求与架构规范文档
```

---

## 4. 构建与测试说明 (Build & Test Guide)

### 4.1 运行单元测试
在 `ai-mirror` 目录下执行：
```bash
./gradlew test
```
测试包含：
- 协议报文编解码验证（正常报文、空 Payload 报文、损坏 Magic 头部校验、流式粘包/截断处理）
- UDP 广播报文序列化/反序列化及设备角色解析
- 实时性能指标统计器准确性
- IP 与端口校验边界条件

### 4.2 编译与打包 APK
```bash
# 编译 Debug APK
./gradlew assembleDebug

# 生成的 APK 路径：
# app/build/outputs/apk/debug/app-debug.apk
```

### 4.3 多设备一键自动安装与部署 (ADB Multi-Device Deployment)
当通过 USB 或 Wi-Fi ADB 连接两台或多台手机时，可直接通过 Gradle Task 或快捷脚本自动构建并并发安装到所有连接的手机：
```bash
# 方式 1：通过 Gradle Task 一键构建并安装到所有 ADB 设备
./gradlew installAllDebug

# 方式 2：通过 Shell 脚本自动构建并安装
./scripts/install_all.sh
```

---

## 5. 维护与后续扩展指南 (Developer & AI Handoff Note)

> **致后续开发者或接手的 AI 助手**：
> 1. **代码规范**：所有新建或修改的代码必须保持清晰模块化，遵循 Kotlin 官方编码规范及 Jetpack 架构指南。
> 2. **多语言更新**：若新增 UI 文本，必须同步更新 `res/values/strings.xml` (英文) 和 `res/values-zh-rCN/strings.xml` (简体中文)。
> 3. **协议兼容性**：若需要拓展报文格式，在 `MirrorProtocol` 中递增 `flags` 或定义新的 `TYPE_*`，不要破坏固定 40 字节 Header 的向后兼容性。
> 4. **硬件加速编码升级**：当前版本采用成熟且高度兼容所有机型的 `FrameProcessor (YUV420 to JPEG)` + WebSocket 二进制通道；若未来需要引入 H.264 / HEVC 硬件 MediaCodec 编码，可直接通过 `TYPE_CONTROL_CONFIG` 协商并在 `PacketCodec` 的 Payload 中传输 NAL 单元。
> 5. **多机自动化部署**：每次打包或修改需求后，使用 `./gradlew installAllDebug` 确保已连接的所有测试手机同步更新。
> 6. **Git 规范**：每次提交需使用中文清晰描述修改内容，项目目录保持以 `ai-` 开头。
