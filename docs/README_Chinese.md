# AI 旅行助手

本项目正在开发一款 AI 旅行助手：根据用户的约束与偏好生成每日行程，结合天气感知的调整与路线规划，支持“一键确认”的预订流程，并可在提供新约束时快速重新生成计划。

## 关键特性

1. **AI 驱动的个性化行程生成**：在约束条件（目的地、预算、同行人数、日期范围、偏好）下生成每日行程计划。

2. **天气感知的动态规划**：监控并纳入目的地在相关日期的天气，智能调整活动安排。

3. **可迭代的大模型反馈回路**：支持完整的二次提示，当用户提供新约束或对方案不满意时可重新生成行程。

4. **有上下文的行程洞察，助力决策**：提供清晰的行程洞察（本地资讯与实用提示），提升决策质量与整体旅行体验。

5. **统一的预订编排**：为酒店/交通/门票提供一键确认（Mockup），展示集成式预订工作流。

6. **路线优化与可视化地图**：通过计算地点间的行程时间与距离（步行/公共交通/驾车视可用性而定）进行路线优化，并提供交互式地图可视化。

**总结：**
这些特性将 **LLM 智能 + 真实世界数据管道**（天气、地图、路线）结合起来，带来 **动态、实用、以用户为中心** 的旅行规划体验。

## 在线演示

[https://aitrip.dingzh.cc](https://aitrip.dingzh.cc)

## 项目说明

旅行规划常被拆散在多个工具中：检索、天气、路线与预订。当天气或可用性变化时，用户必须手动回溯每一步来修复冲突。**AI 旅行助手**将此工作流整合：从用户偏好与约束出发，产出结构化的逐日行程；考虑目的地天气与路线可行性等真实世界信号；在约束变化时可快速再生成。在规划之上，它还提供一键确认的预订编排演示，以及行程洞察（本地资讯与提示）以提高情境感知。目标是让旅行更智能、更具韧性，并降低用户的操作成本。

## 技术栈

* 前端：React 19、Vite、TypeScript
* 后端：Spring Boot 3.5、Java 21、Maven
* 数据库：PostgreSQL 16（Neon Serverless）
* AI Agent 模型：OpenAI gpt-4o-mini

## 高级技术

我们集成了 11 项高级技术，以显著提升系统的性能（通过 CDN 更快地分发静态资源）、可扩展性（EC2 + 无服务器 PostgreSQL）和可维护性（基于 Docker 的 CI/CD 自动化）。

| **序号** | **技术**                                      | **贡献**                             | **类别**   |
| ------ | ------------------------------------------- | ---------------------------------- | -------- |
| 1      | **React（Vite + TypeScript）+ Ant Design**    | 构建高性能的 SPA，具备类型安全与一致的 UI 组件        | 应用框架     |
| 2      | **Spring Boot（Java 21 + Maven）**            | 实现干净的分层式 REST 后端，服务可测试、架构可维护       | 应用框架     |
| 3      | **Docker + Docker Compose**                 | 将服务容器化并协同运行                        | 部署       |
| 4      | **GitHub Actions CI/CD**                    | 在推送时构建镜像并部署到 EC2                   | 部署       |
| 5      | **Nginx 反向代理**                              | 统一 80 端口的路由，并为内部容器提供安全访问           | 部署       |
| 6      | **AWS EC2（ap-southeast-1）**                 | 运行 Nginx 与后端服务；前端作为静态文件上传并通过挂载目录提供 | 云服务      |
| 7      | **AWS CloudWatch Logs**                     | 通过 awslogs 驱动聚合容器日志，用于监控与故障排查      | 云服务      |
| 8      | **AWS S3 + CloudFront / 阿里云 OSS + 阿里云 CDN** | 托管静态资源，以 CDN 加速并降低服务器负载            | 云服务      |
| 9      | **Neon Serverless PostgreSQL**              | 提供可扩展的无服务器数据库服务                    | 云服务      |
| 10     | **Stitch（借助 Gemini AI 的 UI 设计）+ Figma**     | AI 辅助生成 UI，并打磨为可用于生产的设计            | 新型 AI 工具 |
| 11     | **通过 Spring AI 框架调用 OpenAI GPT**            | 使用提示模板与输出解析器，以结构化 JSON 生成行程计划      | 新型 AI 工具 |

## 外部 API

| 外部 API                   | 使用的功能       | 提供的价值                 |
| ------------------------ | ----------- | --------------------- |
| **GeoDB Cities API**     | 目的地自动补全     | 提升感知能力、减少输入错误、提升规划准确度 |
| **Google Maps/AMAP API** | 路线视图 + 行程距离 | 确保行程可行性               |
| **OpenWeather API**      | 考虑天气的（重新）排程 | 提高计划的稳健性              |
| **Unsplash API**         | 行程时间线配图     | 更好的上下文与参与度            |
| **SendGrid API**         | 验证与找回密码     | 维持账号真实性与安全性           |
| **Booking API（模拟）**      | 一键确认预订流程    | 展示可集成的事务性工作流          |

## 面向中国大陆的适配

* 使用阿里云 CDN 加速访问位于新加坡的 EC2 服务器，优先命中边缘节点的缓存（但效果有限——当边缘节点没有缓存时仍需回源访问新加坡。后续可考虑在国内云部署以形成双源站）。
* 后端服务器位于 EC2（新加坡），代表用户向大模型（OpenAI）发起请求，规避中国大陆用户无法直接访问大模型的问题。
* 对于中国大陆无法直接访问的外部资源（如 GeoDB Cities 与 Unsplash 图片），在 EC2（新加坡）上通过 Nginx 反向代理由服务器端发起请求，避免由中国大陆用户直接请求。
* 适配高德地图（Amap）：当用户访问时使用一个很小的 Google 资源做探测；在中国大陆默认使用高德，海外默认使用 Google Maps；使用 Google Geocode 获取地理编码后再提供给高德，以解决高德对英文地名搜索支持较弱的问题。限制：高德无法为海外城市提供路线规划（场景：用户在中国大陆但规划海外城市行程）；同样，Google Maps 对中国大陆城市的路线规划支持也较弱。
* 部署双端 OSS 源站以同步静态资源，并使用 CDN 加速访问；设置 CDN 规则，使海外用户默认回源到 OSS（新加坡），中国大陆用户默认回源到 OSS（杭州），同时提升境内外的访问速度。

## 项目资源与交付物

* 在线演示：**[EC2 部署 + 阿里云 CDN 加速](https://aitrip.dingzh.cc)**
* UI 原型：**[Figma 原型](https://www.figma.com/design/Orh6WDE8hFQUl9IjHuvOG3/Untitled?node-id=0-1&t=Jw0UUMtVW4z3PVYe-1)**
* Jira 项目：**[看板与 Sprint 面板](https://uni-elec5620.atlassian.net/jira/software/projects/ELEC5620/summary?atlOrigin=eyJpIjoiMTM2NGM1YTY4ZTg2NDRkN2JjZjVhY2VjODdmNWI2NzciLCJwIjoiaiJ9)**
* Stage 1 报告：**[Overleaf 文档](https://www.overleaf.com/read/wrwbsmtwqhdx#35bd34)**

## 功能预览

### 1. 首页产品介绍

![首页](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_1.png)

用户可浏览主要特性，并通过“免费试用”立即开始规划行程。

### 2. 创建新行程 收集用户偏好

![创建行程表单](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_2.png)

目的地自动补全、日期选择、预算、货币与偏好输入，支持高度个性化的规划。

### 3. 我的行程 已保存行程看板

![我的行程](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_3.png)

便捷管理多个已保存的行程，可回访方案或创建新行程。

### 4. 行程详情时间线与 AI 行程洞察

![行程详情时间线](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_4.png)

AI 生成的每日日程、实时天气、目的地图片与上下文旅行洞察，增强决策质量。

### 5. 预订页面 一键确认流程

![预订任务](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_5.png)

所有活动转化为预订任务。展示具备批量确认的完整预订就绪度。

### 6. 行程地图视图 路线可视化

![Google 地图视图](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_6-1.png)
![高德地图视图](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_6-2.png)

展示景点之间的预计距离与耗时，助力更优的路线优化与机动性规划。

### 7. 重新规划弹窗 与 LLM 的迭代反馈

![重新规划弹窗](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_7.png)
![重新规划结果](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_8.png)

用户可新增或调整偏好，与大模型无缝再生成行程。

### 8. 用户资料 个性化体验

![用户资料](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_9.png)

资料编辑、头像上传与邮箱验证带来个性化且安全的体验。性别与年龄在适用时将作为偏好使用。

## 开发指南

### 安装必需依赖：

* [java 21](https://www.oracle.com/java/technologies/downloads/)
* [git](https://git-scm.com/install/)
* [nvm（非必须但推荐）+ Node 20](https://nodejs.org/en/download/)

*Node 版本管理器 nvm 可让你在本地安装并切换不同的 nodejs 版本。安装后即可用于安装与切换 nodejs 版本。*

```
# 如果你没有切换 Node 版本的需求，可以忽略此提示，直接安装 Node
nvm install 20.19.4
nvm use 20.19.4
```

检查是否安装完成：

```
java --version
git --version
node --version
```

### Git 配置

```
git clone https://github.sydney.edu.au/25S2-ELEC5620-Wed9-11-Group61/ELEC5620_AI_Trip_Assistant.git
git checkout <your development branch> 
```

*或者使用任意 Git 图形化客户端，如 Github Desktop、TortoiseGit。*

### PostgreSQL（Neon 无服务器）设置

本项目使用 Neon 托管 PostgreSQL。

请在 [Neon 控制台](https://console.neon.tech) 创建数据库，然后在 api/src/main/resources/application.yaml 中填写 `spring.datasource.url`、`spring.datasource.username` 与 `spring.datasource.password` 的对应值。

### OpenAI GPT-4o-Mini 设置

本项目默认使用 OpenAI GPT-4o-Mini 作为生成行程计划的大模型，你也可以选择自己的模型或使用默认模型。

1. 在 [https://platform.openai.com/account/api-keys](https://platform.openai.com/account/api-keys) 创建 OpenAI 账号与 API Key。
2. 将你的 API Key 添加到 `application.yaml` 的 spring.ai.openai.api-key。
3. 将你的模型名称添加到 `application.yaml` 的 spring.ai.openai.model-name。

OpenAI GPT-4o-Mini 文档：[https://platform.openai.com/docs/models/gpt-4o-mini](https://platform.openai.com/docs/models/gpt-4o-mini)

### AWS S3 Bucket 设置

1. 在 AWS 控制台创建新的 S3 桶（Bucket）。
2. 将桶的区域写入 `application.yaml` 的 aws.s3.region。
3. 将桶名写入 `application.yaml` 的 aws.s3.bucket-name。
4. 将桶内的目录名写入 `application.yaml` 的 aws.s3.dir-name。
5. 将你的 CDN 域名写入 `application.yaml` 的 aws.s3.cdn。
6. 创建新的 IAM 用户并生成 Access Key ID 与 Secret Access Key。
7. 将 Access Key ID 与 Secret Access Key 以环境变量的形式添加，键名分别为 `AWS_ACCESS_KEY_ID` 与 `AWS_SECRET_ACCESS_KEY`。

AWS S3 文档：[https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-bucket-configure.html](https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-bucket-configure.html)

*如果你在中国，阿里云 OSS 可能是更好的选择：[https://help.aliyun.com/zh/oss/developer-reference/oss-java-sdk/](https://help.aliyun.com/zh/oss/developer-reference/oss-java-sdk/)*

### 其他外部 API 设置

请参考以下指引设置外部 API：

* GeoDB API: [http://geodb-cities-api.wirefreethought.com/](http://geodb-cities-api.wirefreethought.com/)
* Google Maps API: [https://developers.google.com/maps/documentation/javascript/get-api-key](https://developers.google.com/maps/documentation/javascript/get-api-key)
* AMAP API: [https://lbs.amap.com/](https://lbs.amap.com/)
* Unsplash API: [https://unsplash.com/developers](https://unsplash.com/developers)
* OpenWeather API: [https://openweathermap.org/api](https://openweathermap.org/api)
* SendGrid Email API: [https://www.twilio.com/docs/sendgrid/for-developers/sending-email/api-getting-started](https://www.twilio.com/docs/sendgrid/for-developers/sending-email/api-getting-started)

### 启动前端

确保你位于 `ELEC5620_AI_Trip_Assistant`

```
cd web
npm install 
npm run dev
```

### 启动后端

确保你位于 `ELEC5620_AI_Trip_Assistant`

#### 主 API 服务

```bash
cd api
.\mvnw spring-boot:run # Windows
./mvnw spring-boot:run # Linux/Mac
```

#### 模拟的外部服务

```bash
cd external-service
.\mvnw spring-boot:run # Windows
./mvnw spring-boot:run # Linux/Mac
```

*或者你可以使用任意 IDE 来运行此项目，如 IntelliJ IDEA。*

在浏览器中访问 `http://localhost:5173/`
