# AI Trip Assistant

[中文版本](https://github.com/rplalala/AI_Trip_Assistant/blob/main/docs/README_Chinese.md)

This project is developing an AI Travel Assistant that generates daily itineraries from user constraints and preferences, incorporates weather-aware adjustments and routing, supports booking with a one-click confirm flow, and can quickly regenerate a plan when new constraints are provided.

## Key Features

1. **AI-driven personalized trip generation**: Generate daily trip plans under constraints (destination, budget, party size, date range, preferences).

2. **Weather-aware dynamic planning**: Monitor and incorporate destination weather for the relevant day(s), adjusting activities intelligently.

3. **Iterative LLM feedback loop**: Support full re-prompting to regenerate itineraries when users provide new constraints or are not satisfied with the plan.

4. **Contextual Trip Insights for smarter decisions**: Provide clear Trip Insights (local news and practical tips) to improve decision quality and the overall travel experience.

5. **Unified booking orchestration**: Offer one-click confirmation for hotels/transport/tickets (Mockup) as a demonstration of an integrated booking workflow.

6. **Route optimization with visual mapping**: Optimize routing and visualize navigation between locations by calculating travel time and distance (walking/transport/driving where applicable), and providing interactive map visualization.

**In summary:**
These features combine **LLM intelligence + real-world data pipelines** (weather, mapping, routing) to deliver a **dynamic, practical, and user-centric** travel planning experience.

## Live Demo
[https://aitrip.dingzh.cc](https://aitrip.dingzh.cc)

## Statement

Travel planning is typically fragmented across multiple tools for research, weather, routing, and booking. When weather or availability changes, users must manually revisit each step to fix conflicts. **AI Trip Assistant** consolidates this workflow: start from user preferences and constraints, produce a structured day-by-day itinerary, factor in real-world signals such as destination weather and route feasibility, and allow rapid regeneration when constraints change. On top of planning, it provides unified booking orchestration with a one-click confirm demo flow, and Trip Insights (local news and tips) to raise situational awareness. The goal is to make travel smarter, more resilient, and lower-effort for users.

## Tech Stack
- Frontend: React 19, Vite, TypeScript
- Backend: Spring Boot 3.5, Java 21, Maven
- Database: PostgreSQL 16 (Neon Serverless)
- AI Agent Model: OpenAI gpt-4o-mini

## Advanced Technologies
We integrated 11 advanced technologies to significantly improve system performance (faster static delivery with CDN), scalability (EC2 + serverless PostgreSQL), and maintainability (Dockerized CI/CD automation).

| **No.** | **Technology**                                | **Contribution**                                                                               | **Category**           |
|---------|-----------------------------------------------|------------------------------------------------------------------------------------------------|------------------------|
| 1       | **React (Vite + TypeScript) + Ant Design**    | Builds a high-performance SPA with type safety and consistent UI components                    | Application Frameworks |
| 2       | **Spring Boot (Java 21 + Maven)**             | Implements a clean, layered REST backend with testable services and maintainable architecture  | Application Frameworks |
| 3       | **Docker + Docker Compose**                   | Containerize and run services together                    | Deployment             |
| 4       | **GitHub Actions CI/CD**                      | Build images and deploy to EC2 on push | Deployment             |
| 5       | **Nginx Reverse Proxy**                       | Consolidates routing on port 80 and enables secure access to internal containers               | Deployment             |
| 6       | **AWS EC2 (ap-southeast-1)**                                   | Runs Nginx and backend services; frontend is uploaded as static files and served via volume.                                     | Cloud Services         |
| 7       | **AWS CloudWatch Logs**                                   | Aggregate container logs via the awslogs driver for monitoring and troubleshooting                                     | Cloud Services         |
| 8       | **AWS S3 + CloudFront / Aliyun OSS + Aliyun CDN**  | Hosts static resources with CDN acceleration and reduced server load                           | Cloud Services         |
| 9       | **Neon Serverless PostgreSQL**                | Provides a scalable serverless database service                                                | Cloud Services         |
| 10       | **Stitch (UI Design with Gemini AI) + Figma** | AI-assisted UI generation refined into production-ready design                                 | New AI Tools           |
| 11      | **OpenAI GPT via Spring AI Framework**        | Generates trip plans as structured JSON using prompt templates and output parsers              | New AI Tools           |

## External APIs
| External API             | Used in Feature                  | Value Provided                                                             |
|--------------------------|----------------------------------|----------------------------------------------------------------------------|
| **GeoDB Cities API**     | Destination auto-suggestion      | Improves perception, reduces input errors, and increases planning accuracy |
| **Google Maps/AMAP API**      | Route view + travel distance     | Ensures itinerary feasibility                                              |
| **OpenWeather API**      | Weather-aware （re)scheduling    | Increases plan robustness                                                  |
| **Unsplash API**         | Trip timeline visuals            | Better context & engagement                                                |
| **SendGrid API**         | Verification & password recovery | Maintains account authenticity & security                                  |
| **Booking API (Mocked)** | One-click confirm booking flow   | Demonstrates integration-ready transactional workflow                      |

## Adaptation for Mainland China
- Use Alibaba Cloud CDN to accelerate access to the EC2 server (Singapore), prioritizing cached content on edge nodes (but effectiveness is limited—when edge nodes have no cache they still need to access the Singapore origin. Consider later deploying domestic cloud servers to create dual-origin servers).
- Backend servers located on EC2 (Singapore) act on behalf of users to send requests to the LLM (OpenAI), avoiding the problem of users in Mainland China being unable to directly request the LLM.
- For external resources that cannot be directly accessed from Mainland China, such as GeoDB Cities and Unsplash images, use Nginx reverse proxy on EC2 (Singapore) to issue requests from the EC2 side, avoiding direct requests from users in Mainland China.
- Adapt to Amap (Gaode): use a very small Google resource as a probe when users access; within Mainland China default to Amap, overseas default to Google Maps; use Google Geocode to obtain geocoding and then provide that geocoding to Amap to solve Amap API’s poor support for searching place names in English. Limitation: Amap cannot provide route planning for overseas cities (scenario: user is in Mainland China but planning travel in an overseas city); likewise, Google Maps’ route planning support for Mainland China cities is also poor.
- Deploy dual-end OSS origin servers to synchronize static resources and accelerate access with a CDN; set CDN rules so overseas users default to origin OSS (Singapore) and Mainland China users default to origin OSS (Hangzhou), speeding up access for both domestic and overseas users.


## Project Resources & Deliverables
- Live Demo: [EC2 Deployment + Alicdn Speedup](https://aitrip.dingzh.cc)
- UI Mockup: [Figma Prototype](https://www.figma.com/design/Orh6WDE8hFQUl9IjHuvOG3/Untitled?node-id=0-1&t=Jw0UUMtVW4z3PVYe-1)
- Jira Project: [Kanban & Sprint Board](https://uni-elec5620.atlassian.net/jira/software/projects/ELEC5620/summary?atlOrigin=eyJpIjoiMTM2NGM1YTY4ZTg2NDRkN2JjZjVhY2VjODdmNWI2NzciLCJwIjoiaiJ9)
- Stage 1 Report: [Overleaf Document](https://www.overleaf.com/read/wrwbsmtwqhdx#35bd34)

## Feature Preview

### 1. Home Page Product Introduction

![Home Page](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_1.png)

Users can explore the major features and start planning a trip instantly via “Try Free”.

### 2. Create New Trip Collect User Preference

![Create Trip Form](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_2.png)

Destination autocomplete, date selection, budget, currency, and preferences input enable highly personalized planning.

### 3. My Trips Saved Itineraries Dashboard

![My Trips](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_3.png)

Manage multiple saved itineraries, revisit plans, or create new trips conveniently.

### 4. Trip Detail Timeline and AI Trip Insights

![Trip Detail Timeline](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_4.png)

AI-generated day schedule, real-time weather, destination photos and contextual travel insights enhance decision quality.

### 5. Booking Screen One Click Confirm Flow

![Booking Tasks](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_5.png)

All activities become booking tasks. Demonstrates complete booking readiness with batch confirmation.

### 6. Trip Map View Route Visualization

![Google Map View](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_6-1.png)
![Gaode Map View](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_6-2.png)

Estimated distance and duration between attractions, supporting better route optimization and mobility planning.

### 7. Re Planning Modal Iterative Feedback with LLM

![Replan Modal](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_7.png)
![Replan Result](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_8.png)

Users can add or adjust preference to regenerate itineraries seamlessly with the LLM.

### 8. User Profile Personalized Experience

![User Profile](https://alicdn.dingzh.cc/ai-trip-assistant/home_image/introduction_9.png)

Profile editing, avatar upload, and verified email ensure a personalized and secure experience. Gender and age will be used as preference if applicable.

## Development Guide
### Install required dependencies:
- [java 21](https://www.oracle.com/java/technologies/downloads/)
- [git](https://git-scm.com/install/)
- [nvm (not necessary but recommended) + Node 20](https://nodejs.org/en/download/)

*Nvm is a nodejs manager that lets you install and switch between different nodejs versions locally. After you install it, you can use it to install and switch between different nodejs versions.*
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
*Or you can use any other git gui client, like Github Desktop, TortoiseGit.*

### PostgresSQL (Neon Serverless) Setup
This project uses Neon to host PostgreSQL.

Please create a database in the [Neon Console](https://console.neon.tech), and then fill in the corresponding values for `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` in api/src/main/resources/application.yaml.

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

*If you are in China, aliyun oss service is a better choice for you: https://help.aliyun.com/zh/oss/developer-reference/oss-java-sdk/*

### Other External APIs Setup
Please refer to the following guides to set up the external APIs.
- GeoDB API: http://geodb-cities-api.wirefreethought.com/
- Google Maps API: https://developers.google.com/maps/documentation/javascript/get-api-key
- AMAP API: https://lbs.amap.com/
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
*Or you can use any other IDE to run this project, like IntelliJ IDEA.*

Go to a web browser and access `http://localhost:5173/`
