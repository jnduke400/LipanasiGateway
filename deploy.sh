#!/bin/bash

echo 'Starting to Deploy...'
echo "REMOTE_PATH: $REMOTE_PATH"
echo "REMOTE_PASSWORD: $SERVER_PASS"
echo "SUDO_PASSWORD: $SERVER_PASS"

# Function to handle errors
handle_error() {
    echo "Error: $1"
    exit 1
}

# Prompt for credentials if not provided as environment variables
if [ -z "$REMOTE_PASSWORD" ]; then
    read -sp "Enter password for mbet@75.119.130.98: " REMOTE_PASSWORD
    echo
fi

if [ -z "$SUDO_PASSWORD" ]; then
    read -sp "Enter sudo password for mbet@75.119.130.98: " SUDO_PASSWORD
    echo
fi

if [ -z "$DOCKER_PASSWORD" ]; then
    read -sp "Enter Docker Hub password: " DOCKER_PASSWORD
    echo
fi

# Use sshpass instead of expect if you want to ssh to another server
#run_ssh_command() {
#    local command="$1"
#    sshpass -p "$REMOTE_PASSWORD" ssh -o StrictHostKeyChecking=no root@75.119.130.98 "$command"
#}

# we are not expecting to ssh to another server, so we can use this simple function
run_ssh_command() {
    local command="$1"
    eval "$command"
}

echo "Installing dependencies on remote server..."
# Update and install dependencies on remote server
run_ssh_command "echo '$SUDO_PASSWORD' | sudo -S apt-get update && sudo -S apt-get upgrade -y"

# Install Docker if not already installed
run_ssh_command "if ! command -v docker &> /dev/null; then
    echo '$SUDO_PASSWORD' | sudo -S apt-get install -y apt-transport-https ca-certificates curl software-properties-common &&
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add - &&
    sudo add-apt-repository 'deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable' &&
    sudo apt-get update &&
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io
fi"

# Fix key directory permissions
run_ssh_command "mkdir -p $REMOTE_PATH/access-refresh-token-keys && chmod 777 $REMOTE_PATH/access-refresh-token-keys"


# Install or update Docker Compose on remote server
echo "Installing Docker Compose on remote server..."
run_ssh_command "COMPOSE_VERSION=\$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep 'tag_name' | cut -d '\"' -f 4) &&
    echo '$SUDO_PASSWORD' | sudo -S curl -L \"https://github.com/docker/compose/releases/download/\${COMPOSE_VERSION}/docker-compose-\$(uname -s)-\$(uname -m)\" -o /usr/local/bin/docker-compose &&
    sudo -S chmod +x /usr/local/bin/docker-compose"

# Set Logstash permissions if needed
run_ssh_command "if [ -d \"logstash\" ]; then
    chmod 644 logstash/config/logstash.yml
    chmod 644 logstash/pipeline/logstash.conf
fi"

# Docker operations on remote host
echo "Performing Docker operations..."

# Docker login
run_ssh_command "echo '$DOCKER_PASSWORD' | sudo docker login --username greentelecom --password-stdin"

# Pull latest image
run_ssh_command "echo '$SUDO_PASSWORD' | sudo -S docker pull greentelecom/mbet-payment-gw-engine-lb:latest"

# Required for Elasticsearch
echo "Setting vm.max_map_count for Elasticsearch..."
echo "$SUDO_PASSWORD" | sudo -S sysctl -w vm.max_map_count=262144
echo "$SUDO_PASSWORD" | sudo -S sh -c 'echo "vm.max_map_count=262144" >> /etc/sysctl.conf'

# Change directory to REMOTE_PATH and run docker-compose for elk stack
run_ssh_command "cd $REMOTE_PATH && echo '$SUDO_PASSWORD' | sudo -S docker-compose -f docker-compose-lipanasi-elk-stack.yaml up --build -d --remove-orphans"

# Stop existing containers and free ports
echo "Stopping existing containers..."
docker-compose -f docker-compose-payment-gw.yaml down 2>/dev/null || true

# If port 3306 is still bound by something outside compose, kill it
if lsof -i :3306 &>/dev/null; then
    echo "Port 3306 still in use, killing process..."
    fuser -k 3306/tcp || true
    sleep 3
fi

# Change directory to REMOTE_PATH and run docker-compose for payment gateway
run_ssh_command "cd $REMOTE_PATH && echo '$SUDO_PASSWORD' | sudo -S docker-compose -f docker-compose-payment-gw.yaml up --build -d --remove-orphans"

## Change directory to REMOTE_PATH and run docker-compose
#run_ssh_command "cd $REMOTE_PATH && echo '$SUDO_PASSWORD' | sudo -S docker-compose -f docker-compose-payment-gw.yaml up --build -d --remove-orphans"

# Clean up old images
run_ssh_command "echo '$SUDO_PASSWORD' | sudo -S docker image prune -f"

sleep 15

# Health check function
PORT=8082
checkHealth() {
    PORT=$1
    url="http://75.119.130.98:$PORT/actuator/health"
    pingCount=0
    stopIterate=0
    loopStartTime=$(date +%s)
    loopWaitTime=150

    echo "Checking health at $url"

    while [[ $pingCount -lt 2 && $stopIterate == 0 ]]; do
        startPingTime=$(date +%s)
        if curl -s -m 10 -X GET "$url" > /dev/null; then
            pingCount=$((pingCount + 1))
            echo "Successful health check ($pingCount/2)"
        else
            echo "Health check failed"
        fi

        endPingTime=$(date +%s)
        loopEndTime=$(date +%s)
        loopTimeTaken=$((loopEndTime - loopStartTime))

        if [[ $pingCount -lt 2 && $loopTimeTaken -gt $loopWaitTime ]]; then
            echo "Health check timed out after $loopWaitTime seconds"
            return 1
        fi
        sleep 5
    done

    [[ $pingCount -eq 2 ]] && return 0 || return 1
}

echo "Performing health check..."
if checkHealth $PORT; then
    echo "Service is running successfully on port $PORT"
    echo 'Deployment completed successfully'
else
    handle_error "Health check failed"
fi

# Clean up temporary expect script
rm -f /tmp/ssh_script.exp