#!/bin/bash

# Script to manage test secrets in AWS
# Used to list, view and delete test secrets

set -e

echo "🔐 AWS Secrets Manager - Test Secrets Manager"
echo "========================================================"
echo ""

# Verificar se AWS CLI está instalado
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI não está instalado"
    exit 1
fi

# Verificar se está configurado
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS CLI não está configurado"
    exit 1
fi

# Definir região padrão
DEFAULT_REGION="us-east-1"

# Function to list secrets
list_secrets() {
    echo "📋 Listing test secrets..."
    echo ""
    
    aws secretsmanager list-secrets --region "$DEFAULT_REGION" --query 'SecretList[?starts_with(Name, `test-`)].{Name:Name,CreatedDate:CreatedDate,LastModifiedDate:LastModifiedDate}' --output table
}

# Function to view a secret
view_secret() {
    local secret_name="$1"
    
    if [ -z "$secret_name" ]; then
        echo "❌ Secret name not provided"
        echo "📋 Usage: $0 view SECRET_NAME"
        exit 1
    fi
    
    echo "🔍 Viewing secret: $secret_name"
    echo ""
    
    # Tentar obter o valor do secret
    if aws secretsmanager get-secret-value --secret-id "$secret_name" --region "$DEFAULT_REGION" --query 'SecretString' --output text 2>/dev/null; then
        echo ""
        echo "✅ Secret found and value displayed"
    else
        echo "❌ Secret not found or error accessing"
    fi
}

# Function to delete a secret
delete_secret() {
    local secret_name="$1"
    
    if [ -z "$secret_name" ]; then
        echo "❌ Secret name not provided"
        echo "📋 Usage: $0 delete SECRET_NAME"
        exit 1
    fi
    
    echo "🗑️  Deleting secret: $secret_name"
    echo "⚠️  This action cannot be undone!"
    echo ""
    
    read -p "Are you sure you want to delete the secret '$secret_name'? (y/N): " confirm
    
    if [[ $confirm =~ ^[Yy]$ ]]; then
        aws secretsmanager delete-secret --secret-id "$secret_name" --region "$DEFAULT_REGION" --force-delete-without-recovery
        echo "✅ Secret deleted: $secret_name"
    else
        echo "❌ Operation cancelled"
    fi
}

# Function to delete all test secrets
delete_all_test_secrets() {
    echo "🗑️  Deleting all test secrets..."
    echo "⚠️  This action cannot be undone!"
    echo ""
    
    read -p "Are you sure you want to delete ALL test secrets? (y/N): " confirm
    
    if [[ $confirm =~ ^[Yy]$ ]]; then
        echo "🔍 Searching for test secrets..."
        
        # List all secrets that start with "test-"
        secrets=$(aws secretsmanager list-secrets --region "$DEFAULT_REGION" --query 'SecretList[?starts_with(Name, `test-`)].Name' --output text)
        
        if [ -z "$secrets" ]; then
            echo "ℹ️  No test secrets found"
            return
        fi
        
        echo "📋 Secrets found:"
        echo "$secrets" | tr ' ' '\n'
        echo ""
        
        for secret in $secrets; do
            echo "🗑️  Deleting: $secret"
            aws secretsmanager delete-secret --secret-id "$secret" --region "$DEFAULT_REGION" --force-delete-without-recovery
        done
        
        echo "✅ All test secrets have been deleted"
    else
        echo "❌ Operation cancelled"
    fi
}

# Function to show help
show_help() {
    echo "📋 Usage: $0 [COMMAND] [ARGUMENTS]"
    echo ""
    echo "Available commands:"
    echo "  list                    - List all test secrets"
echo "  view SECRET_NAME        - View the value of a secret"
echo "  delete SECRET_NAME      - Delete a specific secret"
echo "  delete-all              - Delete all test secrets"
echo "  help                    - Show this help"
    echo ""
    echo "Examples:"
echo "  $0 list"
echo "  $0 view test-api-key"
echo "  $0 delete test-api-key"
echo "  $0 delete-all"
    echo ""
    echo "Default region: $DEFAULT_REGION"
}

# Processar argumentos
case "${1:-help}" in
    "list")
        list_secrets
        ;;
    "view")
        view_secret "$2"
        ;;
    "delete")
        delete_secret "$2"
        ;;
    "delete-all")
        delete_all_test_secrets
        ;;
    "help"|*)
        show_help
        ;;
esac 