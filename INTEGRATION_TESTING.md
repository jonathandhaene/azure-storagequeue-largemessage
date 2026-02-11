# Integration Testing Guide

This guide explains how to run integration tests for the Azure Storage Queue Large Message Client. Integration tests verify the full end-to-end functionality against real Azure Storage resources (Queue and Blob Storage).

## Overview

Integration tests are located in `src/test/java/com/azure/storagequeue/largemessage/integration/` and follow the naming convention `*IT.java` (e.g., `AzureStorageQueueLargeMessageIT.java`).

These tests can run against:
- **Local Azurite emulator** (default) - for development and CI/CD pipelines
- **Real Azure cloud resources** - for production-like testing

## Test Coverage

The integration test suite covers:

1. **Small message send & receive** - Messages smaller than 64KB threshold
2. **Large message send & receive** - Messages larger than 64KB with blob offloading
3. **Message with metadata** - Custom metadata preservation
4. **Message deletion with blob cleanup** - Automatic blob cleanup on message deletion
5. **Batch send & receive** - Multiple messages operations
6. **Always-through-blob mode** - Force all messages through blob storage
7. **Queue statistics** - Approximate message count verification
8. **Peek messages** - Non-destructive message inspection

## Running Integration Tests Locally with Azurite

### Prerequisites

1. **Java 17 or higher** installed
2. **Maven 3.6+** installed
3. **Azurite** installed (Azure Storage Emulator)

### Installing Azurite

#### Option 1: Using npm (Node.js)
```bash
npm install -g azurite
```

#### Option 2: Using Docker
```bash
docker pull mcr.microsoft.com/azure-storage/azurite
```

#### Option 3: Using Visual Studio Code Extension
Install the "Azurite" extension from the VS Code marketplace.

### Starting Azurite

#### If installed via npm:
```bash
# Start Azurite with default ports
azurite --silent --location ./azurite-data --debug ./azurite-debug.log

# Or start with specific services
azurite-blob --location ./azurite-data/blob --blobPort 10000
azurite-queue --location ./azurite-data/queue --queuePort 10001
```

#### If using Docker:
```bash
# Start Azurite container with blob and queue services
docker run -p 10000:10000 -p 10001:10001 \
  mcr.microsoft.com/azure-storage/azurite \
  azurite-blob --blobHost 0.0.0.0 --blobPort 10000 \
  azurite-queue --queueHost 0.0.0.0 --queuePort 10001
```

### Running the Tests

Once Azurite is running, execute the integration tests:

```bash
# Run all integration tests
mvn verify -Pintegration-test

# Run only integration tests (skip unit tests)
mvn failsafe:integration-test failsafe:verify -Pintegration-test

# Run with verbose output
mvn verify -Pintegration-test -X
```

The tests will automatically use the default Azurite connection string:
```
DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;QueueEndpoint=http://127.0.0.1:10001/devstoreaccount1
```

## Running Integration Tests Against Real Azure Resources

### Prerequisites

1. An **Azure subscription**
2. An **Azure Storage Account** with both Queue and Blob services enabled

### Setup

1. **Create an Azure Storage Account** (if you don't have one):
   ```bash
   # Using Azure CLI
   az storage account create \
     --name mystorageaccount \
     --resource-group myresourcegroup \
     --location eastus \
     --sku Standard_LRS
   ```

2. **Get the connection string**:
   ```bash
   # Using Azure CLI
   az storage account show-connection-string \
     --name mystorageaccount \
     --resource-group myresourcegroup \
     --output tsv
   ```

3. **Set the environment variable**:

   #### On Linux/macOS:
   ```bash
   export AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;AccountName=mystorageaccount;AccountKey=...;EndpointSuffix=core.windows.net"
   ```

   #### On Windows (PowerShell):
   ```powershell
   $env:AZURE_STORAGE_CONNECTION_STRING="DefaultEndpointsProtocol=https;AccountName=mystorageaccount;AccountKey=...;EndpointSuffix=core.windows.net"
   ```

   #### On Windows (Command Prompt):
   ```cmd
   set AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=mystorageaccount;AccountKey=...;EndpointSuffix=core.windows.net
   ```

### Running the Tests

```bash
# Run integration tests against real Azure resources
mvn verify -Pintegration-test

# The tests will use the connection string from the environment variable
```

## Maven Commands Reference

### Run all tests (unit + integration)
```bash
mvn clean verify -Pintegration-test
```

### Run only unit tests
```bash
mvn test
```

### Run only integration tests
```bash
mvn verify -Pintegration-test -DskipTests
```

### Run a specific integration test class
```bash
mvn verify -Pintegration-test -Dit.test=AzureStorageQueueLargeMessageIT
```

### Run a specific integration test method
```bash
mvn verify -Pintegration-test -Dit.test=AzureStorageQueueLargeMessageIT#testSmallMessageSendAndReceive
```

## Configuration

Integration test configuration is defined in `src/test/resources/application-integration-test.yml`.

Key configuration properties:

```yaml
azure:
  storage:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING:...}
    container-name: test-large-messages
  storagequeue:
    connection-string: ${AZURE_STORAGE_CONNECTION_STRING:...}
    queue-name: test-queue
    large-message-client:
      message-size-threshold: 65536  # 64 KB
      always-through-blob: false
      cleanup-blob-on-delete: true
```

## Test Isolation

Each integration test:
- Creates a unique queue name with a UUID suffix
- Creates a unique blob container with a UUID suffix
- Cleans up resources in `@AfterEach` methods
- Is independent and can run in any order

## Troubleshooting

### Azurite not starting
- Check if ports 10000 and 10001 are already in use
- Try stopping any existing Azurite instances
- Check the Azurite logs for error messages

### Connection errors
- Verify Azurite is running: `curl http://127.0.0.1:10000/devstoreaccount1?comp=list`
- Check firewall settings
- Ensure the connection string is correct

### Tests timing out
- Increase the timeout in test annotations: `@Timeout(60)`
- Check network connectivity to Azure (for cloud tests)
- Verify Azure Storage Account is accessible

### Tests failing intermittently
- Azure Storage operations are eventually consistent
- Add small delays between operations if needed
- Check for resource cleanup issues

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  integration-test:
    runs-on: ubuntu-latest
    
    services:
      azurite:
        image: mcr.microsoft.com/azure-storage/azurite
        ports:
          - 10000:10000
          - 10001:10001
    
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run integration tests
        run: mvn verify -Pintegration-test
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    
    stages {
        stage('Start Azurite') {
            steps {
                sh 'docker run -d -p 10000:10000 -p 10001:10001 --name azurite mcr.microsoft.com/azure-storage/azurite'
            }
        }
        
        stage('Integration Tests') {
            steps {
                sh 'mvn verify -Pintegration-test'
            }
        }
    }
    
    post {
        always {
            sh 'docker stop azurite && docker rm azurite'
        }
    }
}
```

## Best Practices

1. **Always run integration tests before merging** to ensure end-to-end functionality
2. **Use Azurite for local development** to avoid Azure costs and network dependencies
3. **Run tests against real Azure periodically** to catch cloud-specific issues
4. **Keep tests isolated and idempotent** so they can run in parallel
5. **Use unique resource names** to avoid conflicts between test runs
6. **Clean up resources** in teardown methods to prevent resource leaks

## Additional Resources

- [Azurite Documentation](https://github.com/Azure/Azurite)
- [Azure Storage Documentation](https://docs.microsoft.com/en-us/azure/storage/)
- [Maven Failsafe Plugin](https://maven.apache.org/surefire/maven-failsafe-plugin/)
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
