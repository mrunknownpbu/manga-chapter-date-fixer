#!/bin/bash

# Script to build and push Docker image to Docker Hub
# Usage: ./build_and_push.sh <image_name>

if [ $# -eq 0 ]; then
    echo "Usage: $0 <image_name>"
    echo "Example: $0 myuser/manga-chapter-date-fixer"
    exit 1
fi

IMAGE_NAME=$1
TAG=${2:-latest}

echo "Building Docker image: $IMAGE_NAME:$TAG"

# Build the Docker image
docker build -t "$IMAGE_NAME:$TAG" .

if [ $? -ne 0 ]; then
    echo "Error: Docker build failed"
    exit 1
fi

echo "Docker image built successfully: $IMAGE_NAME:$TAG"

# Push to Docker Hub
echo "Pushing image to Docker Hub..."
docker push "$IMAGE_NAME:$TAG"

if [ $? -ne 0 ]; then
    echo "Error: Docker push failed"
    echo "Make sure you are logged in to Docker Hub: docker login"
    exit 1
fi

echo "Successfully pushed $IMAGE_NAME:$TAG to Docker Hub"