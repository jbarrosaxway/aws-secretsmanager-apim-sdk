#!/bin/bash

# Script to create test secrets in AWS
# Used to test the AWS Secrets Manager Filter

set -e

echo "🔐 AWS Secrets Manager - Creating Test Secrets"
echo "=================================================="
echo ""

# Verificar se AWS CLI está instalado
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI não está instalado"
    echo "📋 Instale o AWS CLI: https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html"
    exit 1
fi

# Verificar se está configurado
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS CLI não está configurado"
    echo "📋 Configure com: aws configure"
    exit 1
fi

echo "✅ AWS CLI configurado"
echo "👤 Usuário atual: $(aws sts get-caller-identity --query 'Arn' --output text)"
echo ""

# Definir região padrão
DEFAULT_REGION="us-east-1"
echo "🌍 Região padrão: $DEFAULT_REGION"
echo ""

# Function to create secret
create_secret() {
    local secret_name="$1"
    local secret_value="$2"
    local region="${3:-$DEFAULT_REGION}"
    
    echo "🔐 Creating secret: $secret_name"
    
    # Check if secret already exists
    if aws secretsmanager describe-secret --secret-id "$secret_name" --region "$region" &> /dev/null; then
        echo "⚠️  Secret already exists: $secret_name"
        echo "🔄 Updating value..."
        aws secretsmanager update-secret --secret-id "$secret_name" --secret-string "$secret_value" --region "$region"
    else
        echo "✅ Creating new secret..."
        aws secretsmanager create-secret --name "$secret_name" --secret-string "$secret_value" --region "$region"
    fi
    
    echo "✅ Secret created/updated: $secret_name"
    echo ""
}

# Function to create JSON secret
create_json_secret() {
    local secret_name="$1"
    local json_value="$2"
    local region="${3:-$DEFAULT_REGION}"
    
    echo "🔐 Creating JSON secret: $secret_name"
    
    # Check if secret already exists
    if aws secretsmanager describe-secret --secret-id "$secret_name" --region "$region" &> /dev/null; then
        echo "⚠️  Secret already exists: $secret_name"
        echo "🔄 Updating value..."
        aws secretsmanager update-secret --secret-id "$secret_name" --secret-string "$json_value" --region "$region"
    else
        echo "✅ Creating new secret..."
        aws secretsmanager create-secret --name "$secret_name" --secret-string "$json_value" --region "$region"
    fi
    
    echo "✅ JSON Secret created/updated: $secret_name"
    echo ""
}

echo "🚀 Creating test secrets..."
echo ""

# 1. Secret simples - String
create_secret "test-api-key" "sk-1234567890abcdef1234567890abcdef1234567890abcdef"

# 2. Secret simples - Senha
create_secret "test-database-password" "MySuperSecretPassword123!"

# 3. Secret JSON - Credenciais de banco
create_json_secret "test-database-credentials" '{
    "host": "test-db.example.com",
    "port": 5432,
    "database": "testdb",
    "username": "testuser",
    "password": "TestPassword123!"
}'

# 4. Secret JSON - Configuração de API
create_json_secret "test-api-config" '{
    "baseUrl": "https://api.example.com",
    "timeout": 30000,
    "retryAttempts": 3,
    "apiKey": "api-key-1234567890abcdef"
}'

# 5. Secret JSON - Configuração de JWT
create_json_secret "test-jwt-config" '{
    "issuer": "https://auth.example.com",
    "audience": "https://api.example.com",
    "secretKey": "jwt-secret-key-1234567890abcdef",
    "expirationTime": 3600
}'

# 6. Secret simples - Token de acesso
create_secret "test-access-token" "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

# 7. Secret JSON - Configuração de email
create_json_secret "test-email-config" '{
    "smtpHost": "smtp.example.com",
    "smtpPort": 587,
    "username": "noreply@example.com",
    "password": "EmailPassword123!",
    "fromAddress": "noreply@example.com"
}'

# 8. Secret simples - Chave de criptografia
create_secret "test-encryption-key" "encryption-key-32-chars-long-1234567890abcdef"

# 9. Secret JSON - Configuração de cache
create_json_secret "test-cache-config" '{
    "redisHost": "redis.example.com",
    "redisPort": 6379,
    "redisPassword": "RedisPassword123!",
    "ttl": 3600
}'

# 10. Secret simples - Webhook URL
create_secret "test-webhook-url" "https://webhook.example.com/notify/1234567890abcdef"

echo "🎉 Test secrets created successfully!"
echo ""
echo "📋 List of created secrets:"
echo "1. test-api-key (String)"
echo "2. test-database-password (String)"
echo "3. test-database-credentials (JSON)"
echo "4. test-api-config (JSON)"
echo "5. test-jwt-config (JSON)"
echo "6. test-access-token (String)"
echo "7. test-email-config (JSON)"
echo "8. test-encryption-key (String)"
echo "9. test-cache-config (JSON)"
echo "10. test-webhook-url (String)"
echo ""
echo "🔧 To test in filter:"
echo "- Use the secret name (ex: test-api-key)"
echo "- Region: $DEFAULT_REGION"
echo "- Credential Type: Local (if configured locally)"
echo ""
echo "📖 To list all secrets:"
echo "aws secretsmanager list-secrets --region $DEFAULT_REGION"
echo ""
echo "🗑️  To delete a secret:"
echo "aws secretsmanager delete-secret --secret-id SECRET_NAME --region $DEFAULT_REGION" 