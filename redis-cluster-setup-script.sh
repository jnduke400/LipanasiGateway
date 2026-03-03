#!/bin/bash
# redis-cluster-setup-remote.sh

set -e

# Load environment variables from deploy-env file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/.deploy-env" ]; then
    source "$SCRIPT_DIR/.deploy-env"
    echo "Loaded .deploy-env successfully"
else
    echo "Warning: .deploy-env not found at $SCRIPT_DIR/.deploy-env"
fi

echo 'Starting Redis Cluster Deployment...'
echo "REMOTE_PATH: $REMOTE_PATH"
echo "REMOTE_PASSWORD: $REMOTE_PASSWORD"
echo "SUDO_PASSWORD: $SUDO_PASSWORD"

# Function to handle errors
handle_error() {
    echo "Error: $1"
    exit 1
}

# Function to run SSH commands on remote server
run_ssh_command() {
    local command="$1"
    if [ -n "$REMOTE_PASSWORD" ]; then
        sshpass -p "$REMOTE_PASSWORD" ssh -o StrictHostKeyChecking=no root@75.119.130.98 "$command"
    else
        # REMOTE_PASSWORD not set — run command locally (script is already on the target server)
        echo "REMOTE_PASSWORD not set, running command locally..."
        eval "$command"
    fi
}

# Create redis-config directory
echo "Creating Redis configuration directory..."
mkdir -p "$REMOTE_PATH/redis-config"

# Create Redis node configuration files
echo "Creating Redis node configurations..."
for i in {1..6}; do
    cat > "$REMOTE_PATH/redis-config/redis-node-$i.conf" << EOF
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
requirepass rEd1s123
masterauth rEd1s123
protected-mode yes
dir /data
EOF
done

# Copy docker-compose file if not already present
echo "Checking Docker Compose file..."
if [ ! -f "$REMOTE_PATH/docker-compose-redis-cluster.yaml" ]; then
    handle_error "docker-compose-redis-cluster.yaml not found in $REMOTE_PATH"
fi

# Stop and remove any existing Redis containers
echo "Cleaning up existing Redis containers..."
cd "$REMOTE_PATH" && docker compose -f docker-compose-redis-cluster.yaml down -v 2>/dev/null || true

# Start Redis cluster
echo "Starting Redis cluster..."
cd "$REMOTE_PATH" && docker compose -f docker-compose-redis-cluster.yaml up -d

# Wait for containers to be ready
echo "Waiting for Redis containers to start..."
sleep 15

# Detect the actual network name (it gets prefixed with the project/folder name)
echo "Detecting Redis network name..."
NETWORK_NAME=$(docker network ls --filter name=redis-net --format '{{.Name}}' | head -1)
if [ -z "$NETWORK_NAME" ]; then
    handle_error "Could not find redis-net network. Is the cluster running?"
fi
echo "Using network: $NETWORK_NAME"

# Create the Redis cluster
echo "Creating Redis cluster..."
docker run --rm --network "$NETWORK_NAME" redis:7.2 \
    redis-cli -a 'rEd1s123' --cluster create \
    172.28.0.11:6379 \
    172.28.0.12:6379 \
    172.28.0.13:6379 \
    172.28.0.14:6379 \
    172.28.0.15:6379 \
    172.28.0.16:6379 \
    --cluster-replicas 1 \
    --cluster-yes

# Wait for cluster to stabilize
echo "Waiting for cluster to stabilize..."
sleep 10

# Verify cluster status
echo "Verifying Redis cluster status..."
CLUSTER_INFO=$(docker run --rm --network "$NETWORK_NAME" redis:7.2 \
    redis-cli -a 'rEd1s123' -h 172.28.0.11 cluster info)
echo "$CLUSTER_INFO"

# Check if cluster is healthy
CLUSTER_STATE=$(echo "$CLUSTER_INFO" | grep cluster_state | awk -F':' '{print $2}' | tr -d '[:space:]')

if [[ "$CLUSTER_STATE" == "ok" ]]; then
    echo "✅ Redis Cluster is healthy and running!"

    echo -e "\nCluster nodes:"
    docker run --rm --network "$NETWORK_NAME" redis:7.2 \
        redis-cli -a 'rEd1s123' -h 172.28.0.11 cluster nodes | head -10

    echo -e "\n📊 Monitor your Redis Cluster at: http://75.119.130.98:8280"
else
    echo "❌ Redis Cluster may not be healthy. Current state: $CLUSTER_STATE"
    echo "Checking cluster nodes..."
    docker run --rm --network "$NETWORK_NAME" redis:7.2 \
        redis-cli -a 'rEd1s123' -h 172.28.0.11 cluster nodes
    handle_error "Redis Cluster health check failed"
fi

# Test cluster with a simple key operation
echo -e "\nTesting cluster with sample key operations..."
docker run --rm --network "$NETWORK_NAME" redis:7.2 \
    redis-cli -a 'rEd1s123' -c -h 172.28.0.11 set test:key 'Redis Cluster is working!'
TEST_RESULT=$(docker run --rm --network "$NETWORK_NAME" redis:7.2 \
    redis-cli -a 'rEd1s123' -c -h 172.28.0.11 get test:key)
echo "Test result: $TEST_RESULT"

if [[ "$TEST_RESULT" == *"Redis Cluster is working!"* ]]; then
    echo "✅ Cluster test passed!"
else
    echo "⚠️ Cluster test may have issues"
fi

echo -e "\n✅ Redis Cluster deployment completed successfully!"