#!/bin/bash


if [ $# -ne 2 ]; then
        echo "Usage: $0 <username> <password>"
        exit 1
fi


LOGIN_USERNAME=$1
LOGIN_PASSWORD=$2


RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

export LOGIN_USERNAME
export LOGIN_PASSWORD


docker pull almosf/onlabbackend


CONTAINER_ID=$(docker run -d --network host -p 3001:3001 -e LOGIN_USERNAME -e LOGIN_PASSWORD almosf/onlabbackend)


DOCKER_IP=$(curl -s https://api.ipify.org)

echo "Docker is running at IP: $DOCKER_IP"


echo -e "${GREEN}Login credentials:"
echo -e "${RED}Username - $LOGIN_USERNAME"
echo -e "${RED}PASSWORD - $LOGIN_PASSWORD${NC}"
