#!/bin/bash
set -e

echo "===== Pull Latest Code ====="
git pull origin main

echo "===== Build Frontend ====="
cd web
npm install
npm run build
cd ..

echo "===== Rebuild & Restart Docker ====="
docker-compose down
docker-compose up -d --build

echo "✅ Deployment Completed Successfully!"