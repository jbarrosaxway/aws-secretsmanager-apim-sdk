# 🔐 AWS Secrets Manager - Test Scripts Guide

This directory contains scripts to create and manage test secrets in AWS for use with the AWS Secrets Manager filter.

## 📋 Available Scripts

### 1. `create-test-secrets.sh`
Creates a test data set with 10 different secrets in AWS.

**Execution:**
```bash
./scripts/create-test-secrets.sh
```

**Created Secrets:**
1. `test-api-key` - API Key (String)
2. `test-database-password` - Database password (String)
3. `test-database-credentials` - Database credentials (JSON)
4. `test-api-config` - API configuration (JSON)
5. `test-jwt-config` - JWT configuration (JSON)
6. `test-access-token` - Access token (String)
7. `test-email-config` - Email configuration (JSON)
8. `test-encryption-key` - Encryption key (String)
9. `test-cache-config` - Cache configuration (JSON)
10. `test-webhook-url` - Webhook URL (String)

### 2. `manage-test-secrets.sh`
Script to manage test secrets (list, view, delete).

**Available commands:**
```bash
# List all test secrets
./scripts/manage-test-secrets.sh list

# View a secret value
./scripts/manage-test-secrets.sh view test-api-key

# Delete a specific secret
./scripts/manage-test-secrets.sh delete test-api-key

# Delete all test secrets
./scripts/manage-test-secrets.sh delete-all

# Show help
./scripts/manage-test-secrets.sh help
```

## 🔧 Prerequisites

### 1. AWS CLI
Install AWS CLI:
```bash
# Ubuntu/Debian
sudo apt-get install awscli

# macOS
brew install awscli

# Or download from: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html
```

### 2. AWS Configuration
Configure your AWS credentials:
```bash
aws configure
```

**Required information:**
- AWS Access Key ID
- AWS Secret Access Key
- Default region: `us-east-1` (or your preferred region)
- Default output format: `json`

### 3. AWS Permissions
Make sure your AWS account has the following permissions:
```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "secretsmanager:CreateSecret",
                "secretsmanager:UpdateSecret",
                "secretsmanager:DescribeSecret",
                "secretsmanager:GetSecretValue",
                "secretsmanager:DeleteSecret",
                "secretsmanager:ListSecrets"
            ],
            "Resource": "*"
        }
    ]
}
```

## 🚀 How to Use

### 1. Create Test Secrets
```bash
# Run the creation script
./scripts/create-test-secrets.sh
```

### 2. Test in Filter
In Policy Studio, configure the AWS Secrets Manager filter:

**Basic configuration:**
- **Secret Name**: `test-api-key` (or any other created secret)
- **AWS Region**: `us-east-1`
- **Credential Type**: `Local` (if configured locally)

**Advanced configuration:**
- **Max Retries**: `3`
- **Retry Delay**: `1000` (ms)

### 3. Verify Result
The filter should return the secret value in the `aws.secretsmanager.value` variable.

## 📊 Secret Types

### Simple Secrets (String)
- `test-api-key`
- `test-database-password`
- `test-access-token`
- `test-encryption-key`
- `test-webhook-url`

### JSON Secrets
- `test-database-credentials`
- `test-api-config`
- `test-jwt-config`
- `test-email-config`
- `test-cache-config`

## 🔍 Usage Examples

### 1. Test Simple Secret
```bash
# Create secret
./scripts/create-test-secrets.sh

# In filter, use:
# Secret Name: test-api-key
# Expected result: sk-1234567890abcdef1234567890abcdef1234567890abcdef
```

### 2. Test JSON Secret
```bash
# In filter, use:
# Secret Name: test-database-credentials
# Expected result: {"host":"test-db.example.com","port":5432,"database":"testdb","username":"testuser","password":"TestPassword123!"}
```

### 3. List Available Secrets
```bash
./scripts/manage-test-secrets.sh list
```

### 4. View Secret Value
```bash
./scripts/manage-test-secrets.sh view test-api-key
```

## 🧹 Cleanup

### Delete Specific Secret
```bash
./scripts/manage-test-secrets.sh delete test-api-key
```

### Delete All Test Secrets
```bash
./scripts/manage-test-secrets.sh delete-all
```

## ⚠️ Important

- **Costs**: Secrets Manager has associated costs. Delete secrets after testing.
- **Security**: Created secrets are for testing only. Do not use in production.
- **Region**: Scripts use `us-east-1` by default. Adjust as needed.

## 🔗 Useful Links

- [AWS Secrets Manager Documentation](https://docs.aws.amazon.com/secretsmanager/)
- [AWS CLI Documentation](https://docs.aws.amazon.com/cli/latest/userguide/)
- [AWS Secrets Manager Pricing](https://aws.amazon.com/secrets-manager/pricing/) 