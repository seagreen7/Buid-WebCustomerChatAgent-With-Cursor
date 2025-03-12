# Intelligent Customer Service System

A smart customer service system based on Spring Boot 2.0 and DeepSeek v3 model, implementing real-time chat functionality using Server-Sent Events (SSE).

## Key Features

- Intelligent dialogue based on DeepSeek v3 model
- Automatic loading of FAQ documents as context
- Real-time communication using SSE
- User-friendly interface
- Session management capabilities

## System Requirements

- Java 8 or later
- Maven 3.6 or later
- DeepSeek API key

## Installation Steps

1. Clone or download this repository

2. Configure DeepSeek API key:
   Open `src/main/resources/application.properties` file and set your API key:
   deepseek.api.key=your-api-key


3. Edit FAQ document:
Modify the `faq.txt` file in the root directory to add your own Q&A content.

4. Build the project:   mvn clean package

5. Run the application:
   java -jar target/customer-service-bot-0.0.1-SNAPSHOT.jar


6. Access the application:
Open your browser and visit `http://localhost:8080`

## Usage Instructions

1. Enter your question in the web interface
2. The system will generate responses using DeepSeek v3 model combined with your FAQ content
3. Conversation history is maintained during the session, refreshing the page will create a new session

## Custom Configuration

Modify the following settings in `application.properties`:

- `server.port`: Application port
- `deepseek.api.url`: DeepSeek API URL (if changed)
- `logging.level.*`: Log level configurations

## Notes

- DeepSeek API may require paid subscription, ensure your account has sufficient credits
- The system automatically loads `faq.txt` from root directory as context - ensure file exists

## License

MIT




   
