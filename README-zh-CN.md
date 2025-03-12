# 智能客服系统

基于 Spring Boot 2.0 和 DeepSeek v3 模型的智能客服系统，使用 Server-Sent Events (SSE) 实现实时聊天功能。

## 功能特点

- 基于 DeepSeek v3 模型的智能对话
- 自动加载 FAQ 文档作为上下文
- 使用 SSE 进行实时通信
- 优雅的用户界面
- 会话管理功能

## 系统要求

- Java 8 或更高版本
- Maven 3.6 或更高版本
- DeepSeek API 密钥

## 安装步骤

1. 克隆或下载本仓库

2. 配置 DeepSeek API 密钥:
   打开 `src/main/resources/application.properties` 文件，设置您的 API 密钥：
   ```
   deepseek.api.key=your-api-key
   ```

3. 编辑 FAQ 文档:
   修改根目录下的 `faq.txt` 文件，添加您自己的常见问题和答案。

4. 构建项目:
   ```
   mvn clean package
   ```

5. 运行应用:
   ```
   java -jar target/customer-service-bot-0.0.1-SNAPSHOT.jar
   ```

6. 访问应用:
   打开浏览器，访问 `http://localhost:8080`

## 使用方法

1. 在网页界面输入您的问题
2. 系统会结合您的 FAQ 文档内容，使用 DeepSeek v3 模型生成回答
3. 对话历史会在会话期间保持，刷新页面将创建新会话

## 自定义配置

您可以在 `application.properties` 文件中修改以下配置:

- `server.port`: 应用的运行端口
- `deepseek.api.url`: DeepSeek API 的 URL (如果有变更)
- `logging.level.*`: 日志级别设置

## 注意事项

- DeepSeek API 可能需要付费使用，请确保您的账户有足够的额度
- 系统默认加载根目录下的 `faq.txt` 文件作为上下文，请确保该文件存在

## 许可证

MIT 