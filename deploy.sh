#!/bin/bash
set -e

PROJECT_DIR="/home/ec2-user/ELEC5620_AI_Trip_Assistant"
WEB_DIR="$PROJECT_DIR/web"

cd $PROJECT_DIR

echo "===== Pull Latest Code ====="
git pull origin main

echo "===== Build Frontend ====="
cd $WEB_DIR
npm install
npm run build

echo "===== Rebuild & Restart Docker ====="
cd $PROJECT_DIR
docker compose down
docker compose up -d --build

echo "✅ Deployment Completed Successfully!"
