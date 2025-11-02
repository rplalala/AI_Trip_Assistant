# ELEC5620_AI_Trip_Assistant

Our team is developing an AI Travel Assistant that generates daily itineraries from user constraints and preferences, incorporates weather-aware adjustments and routing, supports booking with a one-click confirm flow, and can quickly regenerate a plan when new constraints are provided.

## Key Features

1. **AI-driven personalized trip generation**: Generate daily trip plans under constraints (destination, budget, party size, date range, preferences).

2. **Weather-aware dynamic planning**: Monitor and incorporate destination weather for the relevant day(s), adjusting activities intelligently.

3. **Iterative LLM feedback loop**: Support full re-prompting to regenerate itineraries when users provide new constraints or are not satisfied with the plan.

4. **Contextual Trip Insights for smarter decisions**: Provide clear Trip Insights (local news and practical tips) to improve decision quality and the overall travel experience.

5. **Unified booking orchestration**: Offer one-click confirmation for hotels/transport/tickets as a demonstration of an integrated booking workflow.

6. **Route optimization with visual mapping**: Optimize routing and visualize navigation between locations by calculating travel time and distance (walking/transport/driving where applicable), and providing interactive map visualization.

**In summary:**
These features combine **LLM intelligence + real-world data pipelines** (weather, mapping, routing) to deliver a **dynamic, practical, and user-centric** travel planning experience.

## Live Demo
[http://ec2-3-26-48-217.ap-southeast-2.compute.amazonaws.com](http://ec2-3-26-48-217.ap-southeast-2.compute.amazonaws.com)

## Statement

Travel planning is typically fragmented across multiple tools for research, weather, routing, and booking. When weather or availability changes, users must manually revisit each step to fix conflicts. **ELEC5620_AI_Trip_Assistant** consolidates this workflow: start from user preferences and constraints, produce a structured day-by-day itinerary, factor in real-world signals such as destination weather and route feasibility, and allow rapid regeneration when constraints change. On top of planning, it provides unified booking orchestration with a one-click confirm demo flow, and Trip Insights (local news and tips) to raise situational awareness. The goal is to make travel smarter, more resilient, and lower-effort for users.

## Tech Stack
- Frontend: React 19, Vite, TypeScript
- Backend: Spring Boot 3.5, Java 21, Maven
- Database: PostgreSQL 16 (Neon Serverless)
- AI Agent Model: OpenAI gpt-4o-mini

## Advanced Technologies
We integrated 10 advanced technologies to significantly improve system performance (faster static delivery with CDN), scalability (EC2 + serverless PostgreSQL), and maintainability (Dockerized CI/CD automation).

| **No.** | **Technology**                                | **Contribution**                                                                               | **Category**           |
|---------|-----------------------------------------------|------------------------------------------------------------------------------------------------|------------------------|
| 1       | **React (Vite + TypeScript) + Ant Design**    | Builds a high-performance SPA with type safety and consistent UI components                    | Application Frameworks |
| 2       | **Spring Boot (Java 21 + Maven)**             | Implements a clean, layered REST backend with testable services and maintainable architecture  | Application Frameworks |
| 3       | **Docker + Docker Compose**                   | Containerizes frontend and backend for reproducible production environments                    | Deployment             |
| 4       | **GitHub Webhook-based CI/CD**                | Automatically pulls latest code, rebuilds Docker images, and restarts services on push to main | Deployment             |
| 5       | **Nginx Reverse Proxy**                       | Consolidates routing on port 80 and enables secure access to internal containers               | Deployment             |
| 6       | **AWS EC2**                                   | Deploys the full-stack Dockerized application in the cloud                                     | Cloud Services         |
| 7       | **AWS S3 + CloudFront**                       | Hosts static resources with CDN acceleration and reduced server load                           | Cloud Services         |
| 8       | **Neon Serverless PostgreSQL**                | Provides a scalable serverless database service                                                | Cloud Services         |
| 9       | **Stitch (UI Design with Gemini AI) + Figma** | AI-assisted UI generation refined into production-ready design                                 | New AI Tools           |
| 10      | **OpenAI GPT via Spring AI Framework**        | Generates trip plans as structured JSON using prompt templates and output parsers              | New AI Tools           |

## External APIs
| External API             | Used in Feature                  | Value Provided                                                             |
|--------------------------|----------------------------------|----------------------------------------------------------------------------|
| **GeoDB Cities API**     | Destination auto-suggestion      | Improves perception, reduces input errors, and increases planning accuracy |
| **Google Maps API**      | Route view + travel distance     | Ensures itinerary feasibility                                              |
| **OpenWeather API**      | Weather-aware （re)scheduling     | Increases plan robustness                                                  |
| **Unsplash API**         | Trip timeline visuals            | Better context & engagement                                                |
| **SendGrid API**         | Verification & password recovery | Maintains account authenticity & security                                  |
| **Booking API (Mocked)** | One-click confirm booking flow   | Demonstrates integration-ready transactional workflow                      |

## Team
- Pin-Hsuan Lai - Fullstack Engineer
- Ting-Yi Lee - Fullstack Engineer
- Rong Cao - Fullstack Engineer
- Zihang Ding - Fullstack Engineer
- Kexuan Zhao - Frontend Engineer

## Project Resources & Deliverables
- Live Demo: [EC2 Deployment](http://ec2-3-26-48-217.ap-southeast-2.compute.amazonaws.com)
- UI Mockup: [Figma Prototype](https://www.figma.com/design/Orh6WDE8hFQUl9IjHuvOG3/Untitled?node-id=0-1&t=Jw0UUMtVW4z3PVYe-1)
- Jira Project: [Kanban & Sprint Board](https://uni-elec5620.atlassian.net/jira/software/projects/ELEC5620/summary?atlOrigin=eyJpIjoiMTM2NGM1YTY4ZTg2NDRkN2JjZjVhY2VjODdmNWI2NzciLCJwIjoiaiJ9)
- Stage 1 Report: [Overleaf Document](https://www.overleaf.com/read/wrwbsmtwqhdx#35bd34)
- CI/CD Webhook: [GitHub Hooks Settings](https://github.sydney.edu.au/25S2-ELEC5620-Wed9-11-Group61/ELEC5620_AI_Trip_Assistant/settings/hooks/10084)

## Feature Preview

### 1. Home Page Product Introduction

![Home Page](https://rongcaorc.s3.ap-southeast-2.amazonaws.com/elec5620/1_home.png)

Users can explore the major features and start planning a trip instantly via “Try Free”.

### 2. Create New Trip Collect User Preference

![Create Trip Form](https://rongcaorc.s3.ap-southeast-2.amazonaws.com/elec5620/2_create.png)

Destination autocomplete, date selection, budget, currency, and preferences input enable highly personalized planning.

### 3. My Trips Saved Itineraries Dashboard

![My Trips](https://rongcaorc.s3.ap-southeast-2.amazonaws.com/elec5620/3_trips.png)

Manage multiple saved itineraries, revisit plans, or create new trips conveniently.

### 4. Trip Detail Timeline and AI Trip Insights

![Trip Detail Timeline](https://rongcaorc.s3.ap-southeast-2.amazonaws.com/elec5620/4_trip_detail.png)

AI-generated day schedule, real-time weather, destination photos and contextual travel insights enhance decision quality.

### 5. Booking Screen One Click Confirm Flow

![Booking Tasks](https://rongcaorc.s3.ap-southeast-2.amazonaws.com/elec5620/5_trip_booking.png)

All activities become booking tasks. Demonstrates complete booking readiness with batch confirmation.

### 6. Trip Map View Route Visualization

![Map View](https://rongcaorc.s3.ap-southeast-2.amazonaws.com/elec5620/6_trip_map.png)

Estimated distance and duration between attractions, supporting better route optimization and mobility planning.

### 7. Re Planning Modal Iterative Feedback with LLM

![Replan Modal](https://rongcaorc.s3.ap-southeast-2.amazonaws.com/elec5620/7_trip_replan.png)

Users can add or adjust preference to regenerate itineraries seamlessly with the LLM.

### 8. User Profile Personalized Experience

![User Profile](https://rongcaorc.s3.ap-southeast-2.amazonaws.com/elec5620/8_user_profile.png)

Profile editing, avatar upload, and verified email ensure a personalized and secure experience. Gender and age will be used as preference if applicable.

## Development Guide
### Install required dependencies:
- [java 21](https://www.oracle.com/java/technologies/downloads/)
- [git](https://git-scm.com/install/)
- [nvm (not necessary but recommended) + Node 20](https://nodejs.org/en/download/)

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

### Other External APIs Setup
Please refer to the following guides to set up the external APIs.
- GeoDB API: http://geodb-cities-api.wirefreethought.com/
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
