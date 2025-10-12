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
- Ai Agent Model: Gemini-2.5-flash

## Team
- Pin-Hsuan Lai - xxx
- Ting-Yi Lee - xxx
- Kexuan Zhao - xxx
- Rong Cao - xxx
- Zihang Ding - xxx

## Jira Board
https://uni-elec5620.atlassian.net/jira/software/projects/ELEC5620/summary?atlOrigin=eyJpIjoiMTM2NGM1YTY4ZTg2NDRkN2JjZjVhY2VjODdmNWI2NzciLCJwIjoiaiJ9

## Stage 1 Report
https://cn.overleaf.com/read/wrwbsmtwqhdx#35bd34

## Development Guide
### Install required dependencies:
- java 21
- git
- nvm (not necessary, but recommended) + node 20.19.4

Nvm is a Node manager that lets you install and switch between different Node versions locally. After you install it, you can use it to install and switch between different Node versions.
```
# if you don't have the requirement of switching node versions
# you can ignore this tip and just install node directly
nvm install 20.19.4
nvm use 20.19.4
```
Check that everything is installed:
```
java --version
git --version
node --version
```

### Setup Git
```
git clone https://github.com/rplalala/ELEC5620_AI_Trip_Assistant.git
git checkout <your development branch> 
```
Or you can use any other git gui client, like Github Desktop, TortoiseGit, etc..

### Setup PostgresSQL
This project uses Neon to host PostgreSQL.

Please create a database in the Neon Console https://console.neon.tech, 
and then fill in the corresponding values for `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` in api/src/main/resources/application.yaml.

### Setup Gemini API Key (gemini-2.5-flash)
1. Sign in to Google AI Studio with your Google account.
2. Open the API Keys page and click Create API key.
3. Copy the generated key and keep it secure.
4. Add the API key to your environment variables with the key name `GEMINI_API_KEY`.

Gemini API docs: https://ai.google.dev/gemini-api/docs/api-key#linuxmacos---bash

### Launch Frontend
Make sure you're inside of `ELEC5620_AI_Trip_Assistant`
```
cd web
npm install 
npm run dev
```

### Launch Backend
Make sure you're inside of `ELEC5620_AI_Trip_Assistant`
```bash
cd api
.\mvnw spring-boot:run # Windows
./mvnw spring-boot:run # Linux/Mac
```
Or you can use any other IDE to run the project, like IntelliJ IDEA.

Go to a web browser and access `http://localhost:5173/` 
