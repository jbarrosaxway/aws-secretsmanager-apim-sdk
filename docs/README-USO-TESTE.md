# 🔐 AWS Secrets Manager - Testing and Usage Guide

This guide demonstrates how to use test scripts to create and manage secrets in AWS, and how to test the AWS Secrets Manager filter in Axway API Gateway.

## 📋 Index

- [🚀 Quick Start](#-quick-start)
- [🔧 Prerequisites](#-prerequisites)
- [📊 Test Secrets Created](#-test-secrets-created)
- [🧪 How to Test the Filter](#-how-to-test-the-filter)
- [🔍 Practical Examples](#-practical-examples)
- [📝 Logs and Debug](#-logs-and-debug)
- [🧹 Cleanup](#-cleanup)

## 🚀 Quick Start

### 1. Create Test Secrets
```bash
# Run the creation script
./scripts/create-test-secrets.sh
```

### 2. Test in Policy Studio
1. Open **Policy Studio**
2. Search for **"AWS Secrets Manager Filter"** in the palette
3. Configure:
   - **Secret Name**: `test-api-key`
   - **AWS Region**: `us-east-1`
   - **Credential Type**: `Local`
4. Test the filter

### 3. Verify Result
The secret value will be available in: `aws.secretsmanager.value`

## 🔧 Prerequisites

### 1. AWS CLI Configured
```bash
# Check if installed
aws --version

# Configure credentials
aws configure
```

### 2. AWS Permissions
Make sure you have the following permissions:
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

### 3. Filter Installed
- JAR installed in Axway API Gateway
- YAML configured in Policy Studio
- Runtime Dependencies configured

## 📊 Test Secrets Created

### **Simple Secrets (String)**
| Name | Type | Value |
|------|------|-------|
| `test-api-key` | String | `sk-1234567890abcdef1234567890abcdef1234567890abcdef` |
| `test-database-password` | String | `MySuperSecretPassword123!` |
| `test-access-token` | String | Valid JWT Token |
| `test-encryption-key` | String | `encryption-key-32-chars-long-1234567890abcdef` |
| `test-webhook-url` | String | `https://webhook.example.com/notify/1234567890abcdef` |

### **JSON Secrets**
| Name | Type | Structure |
|------|------|-----------|
| `test-database-credentials` | JSON | Database credentials |
| `test-api-config` | JSON | API configuration |
| `test-jwt-config` | JSON | JWT configuration |
| `test-email-config` | JSON | Email configuration |
| `test-cache-config` | JSON | Cache configuration |

## 🧪 How to Test the Filter

### **Step 1: Basic Configuration**

In Policy Studio, configure the **AWS Secrets Manager** filter:

```
Secret Name: test-api-key
AWS Region: us-east-1
Credential Type: Local
Max Retries: 3
Retry Delay: 1000
```

### **Step 2: Advanced Configuration**

For JSON secrets, you can use:

```
Secret Name: test-database-credentials
AWS Region: us-east-1
Credential Type: Local
Client Configuration: Default AWS Client Configuration
```

### **Step 3: Verify Result**

After execution, check the message variables:

- `aws.secretsmanager.value` - Secret value
- `aws.secretsmanager.status.code` - Status code (200 = success)
- `aws.secretsmanager.arn` - Secret ARN
- `aws.secretsmanager.name` - Secret name
- `aws.secretsmanager.error` - Error (if any)

## 🔍 Practical Examples

### **Example 1: Simple Secret**

**Configuration:**
```
Secret Name: test-api-key
AWS Region: us-east-1
```

**Expected Result:**
```
aws.secretsmanager.value = sk-1234567890abcdef1234567890abcdef1234567890abcdef
aws.secretsmanager.status.code = 200
```

### **Example 2: JSON Secret**

**Configuration:**
```
Secret Name: test-database-credentials
AWS Region: us-east-1
```

**Expected Result:**
```json
aws.secretsmanager.value = {
    "host": "test-db.example.com",
    "port": 5432,
    "database": "testdb",
    "username": "testuser",
    "password": "TestPassword123!"
}
```

### **Example 3: Configuration Secret**

**Configuration:**
```
Secret Name: test-api-config
AWS Region: us-east-1
```

**Expected Result:**
```json
aws.secretsmanager.value = {
    "baseUrl": "https://api.example.com",
    "timeout": 30000,
    "retryAttempts": 3,
    "apiKey": "api-key-1234567890abcdef"
}
```

## 📝 Logs and Debug

### **Success Logs**
```
Success in the AWS Secrets Manager filter
```

### **Debug Logs**
```
=== Secrets Manager Invocation Debug ===
Secret Name: test-api-key
Region: us-east-1
Credential Type: local
Max Retries: 3
Retry Delay: 1000

=== Secrets Manager Response ===
Secret Value: Retrieved (length: 51)
```

### **Error Logs**
```
Error in the AWS Secrets Manager filter: Secret not found
Failed in the AWS Secrets Manager filter
```

### **Error Variables**
- `aws.secretsmanager.error` - Error description
- `aws.secretsmanager.status.code` - Error code

## 🧹 Cleanup

### **Delete Specific Secret**
```bash
./scripts/manage-test-secrets.sh delete test-api-key
```

### **List Secrets**
```bash
./scripts/manage-test-secrets.sh list
```

### **View Secret**
```bash
./scripts/manage-test-secrets.sh view test-api-key
```

### **Delete All Secrets**
```bash
./scripts/manage-test-secrets.sh delete-all
```

## 🔧 Useful Commands

### **Check Created Secrets**
```bash
aws secretsmanager list-secrets --region us-east-1 --query 'SecretList[?starts_with(Name, `test-`)].Name' --output table
```

### **Test AWS Credentials**
```bash
aws sts get-caller-identity
```

### **Check Region**
```bash
aws configure get region
```

## ⚠️ Important

### **Costs**
- Secrets Manager has associated costs
- Delete secrets after testing
- Use `./scripts/manage-test-secrets.sh delete-all`

### **Security**
- Secrets are for testing only
- Do not use in production
- Use IAM Roles in production

### **Region**
- Scripts use `us-east-1` by default
- Adjust as needed
- Verify the region is correct

## 🚀 Next Steps

1. **Test all created secrets**
2. **Experiment with different configurations**
3. **Test error scenarios**
4. **Implement in production**
5. **Configure monitoring**

## 📞 Support

If you encounter problems:

1. **Check filter logs**
2. **Test AWS credentials** with `aws sts get-caller-identity`
3. **Confirm secret exists** with `aws secretsmanager describe-secret`
4. **Verify region** configuration
5. **Test with AWS CLI** first

---

**🎉 Now you have a complete test data set to test the AWS Secrets Manager filter!** 