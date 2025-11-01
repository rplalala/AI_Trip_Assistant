# ELEC5620_AI_Trip_Assistant
Our team is developing an AI Travel Assistant that plans daily itineraries from user preferences, monitors weather/availability in real time, auto-replans on disruptions, and supports booking + payment + calendar integration.

This project will have some key requirements including but not limited to:

1. Generate daily trip plans under constraints (budget, food, time, opening-hours).

2. Monitor weather/transport; send advice and automatically re-plan when needed.

3. Single-item modification and full reprompting to regenerate itinerary.

4. Group travel support: roles (Editor/Viewer), questionnaire-based preference collection, budget management and alerts.

5. Unified booking orchestration (hotel/flight/tickets), payment redirect, and calendar insertion.

## Statement
Travel planning is fragmented across many apps (planning, booking, weather, transport). Plans often break when weather changes or availability shifts. Users jump between tools and manually fix conflicts.

**AI Travel Assistant** centralises this flow: collect preferences (or group questionnaire) → generate primary plan with alternatives → validate opening hours & availability → live weather monitoring → auto-replanning on alerts → booking & payment → calendar write-back and notifications. 
The goal is to make travel **smart**, **resilient**, and **low-effort**.

## Tech Stack
- Frontend: React 19, Vite, TypeScript
- Backend: Spring Boot 3.5, Java 21, Maven
- Database: PostgreSQL 16 (Neon Serverless)
- Ai Agent Model: OpenAI gpt-4o-mini

## Advanced Technologies
**(1) Application Frameworks**
- **React (Vite + TypeScript) + Ant Design:** builds a high-performance SPA with Vite and TypeScript type safety, while crafts page layouts using Ant Design’s components and theming to speed up delivery and ensure visual consistency.
- **Spring Boot (Java 21 + Maven):** implements a clean, layered REST backend with modern Java, while ensures testable services, clear configuration, and repeatable Maven builds. 

**(2) Cloud Services**
- **AWS S3 + Cloudflare:** hosts static resources with CDN acceleration while storing object URLs in the DB to cut app memory and improve throughput.
- **Neon Serverless PostgresSQL:** provides a serverless database service.

**(3) Deployment**
- **CI/CD:**

**(4) New AI Tools**
- **Stitch (UI Design with Gemini AI) + Figma:** designs UI with AI assistant, then refined in Figma, speeding up design-to-code handoff and keeping visual consistency.
- **OpenAI GPT via Spring AI Framework:** generates trip plans as structured JSON using prompt templates and output parsers, then validate and persist to databases.

*Note*: Since the backend uses the Spring Boot framework, we chose Spring AI to integrate LLMs instead of LangChain because it better fits the Spring ecosystem.

**(5) External APIs**
- **Google Maps API:** provides route planning for itinerary activities.
- **Unsplash API:** provides high-quality related images for the itinerary timeline.
- **OpenWeather API:** provides weather forecasts for the trip plan.
- **SendGrid Email API:** provides verification for registration, forgot password reset, and email change.

## Team
- Pin-Hsuan Lai - xxx
- Ting-Yi Lee - xxx
- Kexuan Zhao - xxx
- Rong Cao - xxx
- Zihang Ding - xxx

## Stage 1 Report
https://www.overleaf.com/read/wrwbsmtwqhdx#35bd34

## Jira Board
https://uni-elec5620.atlassian.net/jira/software/projects/ELEC5620/summary?atlOrigin=eyJpIjoiMTM2NGM1YTY4ZTg2NDRkN2JjZjVhY2VjODdmNWI2NzciLCJwIjoiaiJ9

## UI Mockup
https://www.figma.com/design/Orh6WDE8hFQUl9IjHuvOG3/Untitled?node-id=0-1&t=Jw0UUMtVW4z3PVYe-1

## Development Guide
### Install required dependencies:
- java 21 https://www.oracle.com/java/technologies/downloads/
- git https://git-scm.com/install/
- nvm (not necessary but recommended) + Node 20 https://nodejs.org/en/download/

Nvm is a nodejs manager that lets you install and switch between different nodejs versions locally. After you install it, you can use it to install and switch between different nodejs versions.
```
# if you don't have the requirement of switching node versions, you can ignore this tip and just install node directly
nvm install 20.19.4
nvm use 20.19.4
```
Check that everything is installed:
```
java --version
git --version
node --version
```

### Git Setup
```
git clone https://github.sydney.edu.au/25S2-ELEC5620-Wed9-11-Group61/ELEC5620_AI_Trip_Assistant.git
git checkout <your development branch> 
```
Or you can use any other git gui client, like Github Desktop, TortoiseGit.

### PostgresSQL (Neon Serverless) Setup
This project uses Neon to host PostgreSQL. Neon project: https://console.neon.tech/app/projects/round-sound-33830706

Please create a database in the Neon Console https://console.neon.tech, and then fill in the corresponding values for `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` in api/src/main/resources/application.yaml.

### OpenAI GPT-4o-Mini Setup
This project uses OpenAI GPT-4o-Mini as the default LLM model to generate trip plans, you can choose your own model or use the default model.
1. Create a new OpenAI account in https://platform.openai.com/account/api-keys.
2. Add your API key to `application.yaml` spring.ai.openai.api-key.
3. Add your model name to `application.yaml` spring.ai.openai.model-name.

OpenAI GPT-4o-Mini docs: https://platform.openai.com/docs/models/gpt-4o-mini

### AWS S3 Bucket Setup
1. Create a new S3 bucket in AWS Console.
2. Add your bucket region to `application.yaml` aws.s3.region.
3. Add your bucket name to `application.yaml` aws.s3.bucket-name.
4. Add your dir name in bucket to `application.yaml` aws.s3.dir-name.
5. Add your cdn domain to `application.yaml` aws.s3.cdn.
6. Create a new IAM user and generate an Access Key ID and Secret Access Key.
7. Add the Access Key ID and Secret Access Key to your environment variables with the key name `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`.

AWS S3 docs: https://docs.aws.amazon.com/AmazonS3/latest/userguide/create-bucket-configure.html

### Other External APIs Setup
Please refer to the following guides to set up the external APIs.
- Google Maps API: https://developers.google.com/maps/documentation/javascript/get-api-key
- Unsplash API: https://unsplash.com/developers
- OpenWeather API: https://openweathermap.org/api
- SendGrid Email API: https://www.twilio.com/docs/sendgrid/for-developers/sending-email/api-getting-started

### Launch Frontend
Make sure you're inside of `ELEC5620_AI_Trip_Assistant`
```
cd web
npm install 
npm run dev
```

### Launch Backend
Make sure you're inside of `ELEC5620_AI_Trip_Assistant`
#### Main API Service
```bash
cd api
.\mvnw spring-boot:run # Windows
./mvnw spring-boot:run # Linux/Mac
```

#### Mocked External Service
```bash
cd external-service
.\mvnw spring-boot:run # Windows
./mvnw spring-boot:run # Linux/Mac
```
Or you can use any other IDE to run this project, like IntelliJ IDEA.

Go to a web browser and access `http://localhost:5173/`
