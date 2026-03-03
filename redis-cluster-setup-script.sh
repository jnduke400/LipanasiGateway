#!/bin/bash
# redis-cluster-setup-remote.sh

set -e

echo 'Starting Redis Cluster Deployment...'
echo "REMOTE_PATH: $REMOTE_PATH"
echo "REMOTE_PASSWORD: $SERVER_PASS"
echo "SUDO_PASSWORD: $SERVER_PASS"

# Function to handle errors
handle_error() {
    echo "Error: $1"
    exit 1
}

# Function to run SSH commands
run_ssh_command() {
    local command="$1"
    if [ -n "$REMOTE_PASSWORD" ]; then
        sshpass -p "$REMOTE_PASSWORD" ssh -o StrictHostKeyChecking=no root@75.119.130.98 "$command"
    else
        echo "Error: REMOTE_PASSWORD not set"
        exit 1
    fi
}

# Create redis-config directory on remote server
echo "Creating Redis configuration directory..."
run_ssh_command "mkdir -p $REMOTE_PATH/redis-config"

# Create Redis node configuration files on remote server
echo "Creating Redis node configurations..."
for i in {1..6}
do
    run_ssh_command "cat > $REMOTE_PATH/redis-config/redis-node-$i.conf << 'EOF'
port 6379
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
cluster-announce-ip 172.28.0.1$i
cluster-announce-port 6379
cluster-announce-bus-port 16379
maxmemory 1gb
maxmemory-policy allkeys-lru
appendonly yes
requirepass Si*%PuR=!xK!3pT@sWq<=>
masterauth Si*%PuR=!xK!3pT@sWq<=>
protected-mode yes
dir /data
EOF"
done

# Copy docker-compose file to remote server
echo "Copying Docker Compose file to remote server..."
# First, ensure we have the docker-compose file content
if [ ! -f "docker-compose-redis-cluster.yml" ]; then
    handle_error "docker-compose-redis-cluster.yml not found in current directory"
fi

# Read the docker-compose file and create it on remote server
run_ssh_command "cat > $REMOTE_PATH/docker-compose-redis-cluster.yml" < docker-compose-redis-cluster.yml

# Install Docker and Docker Compose if not already installed
echo "Ensuring Docker and Docker Compose are installed..."
run_ssh_command "if ! command -v docker &> /dev/null; then
    echo '$SUDO_PASSWORD' | sudo -S apt-get update &&
    echo '$SUDO_PASSWORD' | sudo -S apt-get install -y apt-transport-https ca-certificates curl software-properties-common &&
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add - &&
    sudo add-apt-repository 'deb [arch=amd64] https://download.docker.com/linux/ubuntu \$(lsb_release -cs) stable' &&
    sudo apt-get update &&
    sudo apt-get install -y docker-ce docker-ce-cli containerd.io
fi"

# Install Docker Compose if not already installed
run_ssh_command "if ! command -v docker-compose &> /dev/null; then
    COMPOSE_VERSION=\$(curl -s https://api.github.com/repos/docker/compose/releases/latest | grep 'tag_name' | cut -d '\"' -f 4) &&
    echo '$SUDO_PASSWORD' | sudo -S curl -L \"https://github.com/docker/compose/releases/download/\${COMPOSE_VERSION}/docker-compose-\$(uname -s)-\$(uname -m)\" -o /usr/local/bin/docker-compose &&
    sudo -S chmod +x /usr/local/bin/docker-compose
fi"

# Stop and remove any existing Redis containers
echo "Cleaning up existing Redis containers..."
run_ssh_command "cd $REMOTE_PATH && echo '$SUDO_PASSWORD' | sudo -S docker-compose -f docker-compose-redis-cluster.yml down -v 2>/dev/null || true"

# Start Redis cluster
echo "Starting Redis cluster..."
run_ssh_command "cd $REMOTE_PATH && echo '$SUDO_PASSWORD' | sudo -S docker-compose -f docker-compose-redis-cluster.yml up -d"

# Wait for containers to be ready
echo "Waiting for Redis containers to start..."
sleep 15

# Create the Redis cluster using redis-cli
echo "Creating Redis cluster..."
run_ssh_command "cd $REMOTE_PATH && echo '$SUDO_PASSWORD' | sudo -S docker run --rm --network redis-cluster_redis-net redis:7.2 redis-cli -a Si*%PuR=!xK!3pT@sWq<=> --cluster create \
    172.28.0.11:6379 \
    172.28.0.12:6379 \
    172.28.0.13:6379 \
    172.28.0.14:6379 \
    172.28.0.15:6379 \
    172.28.0.16:6379 \
    --cluster-replicas 1 \
    --cluster-yes"

# Wait for cluster to stabilize
echo "Waiting for cluster to stabilize..."
sleep 10

# Verify cluster status
echo "Verifying Redis cluster status..."
CLUSTER_INFO=$(run_ssh_command "cd $REMOTE_PATH && docker run --rm --network redis-cluster_redis-net redis:7.2 redis-cli -a Si*%PuR=!xK!3pT@sWq<=> -h 172.28.0.11 cluster info")
echo "$CLUSTER_INFO"

# Check if cluster is healthy
CLUSTER_STATE=$(echo "$CLUSTER_INFO" | grep cluster_state | awk -F':' '{print $2}' | tr -d '[:space:]')

if [[ "$CLUSTER_STATE" == "ok" ]]; then
    echo "✅ Redis Cluster is healthy and running!"

    # Show cluster nodes
    echo -e "\nCluster nodes:"
    run_ssh_command "cd $REMOTE_PATH && docker run --rm --network redis-cluster_redis-net redis:7.2 redis-cli -a Si*%PuR=!xK!3pT@sWq<=> -h 172.28.0.11 cluster nodes | head -10"

    echo -e "\n📊 Monitor your Redis Cluster at: http://75.119.130.98:8081"
    echo "🔍 Redis Commander credentials: No password required (configured in docker-compose)"
else
    echo "❌ Redis Cluster may not be healthy. Current state: $CLUSTER_STATE"
    echo "Checking cluster nodes..."
    run_ssh_command "cd $REMOTE_PATH && docker run --rm --network redis-cluster_redis-net redis:7.2 redis-cli -a Si*%PuR=!xK!3pT@sWq<=> -h 172.28.0.11 cluster nodes"
    handle_error "Redis Cluster health check failed"
fi

# Optional: Test cluster with a simple key operation
echo -e "\nTesting cluster with sample key operations..."
TEST_RESULT=$(run_ssh_command "cd $REMOTE_PATH && docker run --rm --network redis-cluster_redis-net redis:7.2 redis-cli -a Si*%PuR=!xK!3pT@sWq<=> -c -h 172.28.0.11 set test:key 'Redis Cluster is working!' && docker run --rm --network redis-cluster_redis-net redis:7.2 redis-cli -a Si*%PuR=!xK!3pT@sWq<=> -c -h 172.28.0.11 get test:key")
echo "Test result: $TEST_RESULT"

if [[ "$TEST_RESULT" == *"Redis Cluster is working!"* ]]; then
    echo "✅ Cluster test passed!"
else
    echo "⚠️ Cluster test may have issues"
fi

echo -e "\n✅ Redis Cluster deployment completed successfully!"