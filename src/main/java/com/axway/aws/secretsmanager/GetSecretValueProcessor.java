package com.axway.aws.secretsmanager;

import java.security.GeneralSecurityException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.WebIdentityTokenCredentialsProvider;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException;
import com.amazonaws.services.secretsmanager.model.InvalidRequestException;
import com.amazonaws.services.secretsmanager.model.InvalidParameterException;
import com.amazonaws.services.secretsmanager.model.DecryptionFailureException;
import com.amazonaws.services.secretsmanager.model.InternalServiceErrorException;
import com.vordel.circuit.CircuitAbortException;
import com.vordel.circuit.Message;
import com.vordel.circuit.MessageProcessor;
import com.vordel.circuit.aws.AWSFactory;
import com.vordel.config.Circuit;
import com.vordel.config.ConfigContext;
import com.vordel.el.Selector;
import com.vordel.es.Entity;
import com.vordel.es.EntityStoreException;
import com.vordel.trace.Trace;

/**
 * AWS Secrets Manager Processor for Axway API Gateway
 * 
 * IAM Role Configuration:
 * - "iam" credential type: Uses WebIdentityTokenCredentialsProvider only
 *   - AWS SDK automatically handles IRSA (IAM Roles for Service Accounts) and EC2 Instance Profile
 *   - Reads environment variables (AWS_WEB_IDENTITY_TOKEN_FILE, AWS_ROLE_ARN) internally
 *   - Supports both ServiceAccount tokens and EC2 instance metadata
 * 
 * - "file" credential type: Uses ProfileCredentialsProvider with specified file
 * - "local" credential type: Uses AWSFactory for explicit credentials
 */
public class GetSecretValueProcessor extends MessageProcessor {
	
	// Selectors for dynamic field resolution (following Lambda pattern)
	protected Selector<String> secretName;
	protected Selector<String> secretRegion;
	protected Selector<String> maxRetries;
	protected Selector<String> retryDelay;
	protected Selector<String> credentialType;
	protected Selector<String> awsCredential;
	protected Selector<String> clientConfiguration;
	protected Selector<String> credentialsFilePath;
	
	// AWS Secrets Manager client builder
	protected AWSSecretsManagerClientBuilder secretsManagerClientBuilder;
	
	// Content body selector
	private Selector<String> contentBody = new Selector<>("${content.body}", String.class);

	public GetSecretValueProcessor() {
	}

	@Override
	public void filterAttached(ConfigContext ctx, Entity entity) throws EntityStoreException {
		super.filterAttached(ctx, entity);
		
		// Initialize selectors for all fields (following Lambda pattern)
		this.secretName = new Selector(entity.getStringValue("secretName"), String.class);
		this.secretRegion = new Selector(entity.getStringValue("secretRegion"), String.class);
		this.maxRetries = new Selector(entity.getStringValue("maxRetries"), String.class);
		this.retryDelay = new Selector(entity.getStringValue("retryDelay"), String.class);
		this.credentialType = new Selector(entity.getStringValue("credentialType"), String.class);
		this.awsCredential = new Selector(entity.getStringValue("awsCredential"), String.class);
		this.clientConfiguration = new Selector(entity.getStringValue("clientConfiguration"), String.class);
		this.credentialsFilePath = new Selector(entity.getStringValue("credentialsFilePath") != null ? entity.getStringValue("credentialsFilePath") : "", String.class);
		
		// Get client configuration (following Lambda pattern exactly)
		Entity clientConfig = ctx.getEntity(entity.getReferenceValue("clientConfiguration"));
		
		// Configure Secrets Manager client builder (following Lambda pattern)
		this.secretsManagerClientBuilder = getSecretsManagerClientBuilder(ctx, entity, clientConfig);
		
		Trace.info("=== Secrets Manager Configuration (Following Lambda Pattern) ===");
		Trace.info("Secret Name: " + (secretName != null ? secretName.getLiteral() : "dynamic"));
		Trace.info("Region: " + (secretRegion != null ? secretRegion.getLiteral() : "dynamic"));
		Trace.info("Max Retries: " + (maxRetries != null ? maxRetries.getLiteral() : "dynamic"));
		Trace.info("Retry Delay: " + (retryDelay != null ? retryDelay.getLiteral() : "dynamic"));
		Trace.info("Credential Type: " + (credentialType != null ? credentialType.getLiteral() : "dynamic"));
		Trace.info("AWS Credential: " + (awsCredential != null ? awsCredential.getLiteral() : "dynamic"));
		Trace.info("Client Configuration: " + (clientConfiguration != null ? clientConfiguration.getLiteral() : "dynamic"));
		Trace.info("Credentials File Path: " + (credentialsFilePath != null ? credentialsFilePath.getLiteral() : "dynamic"));
		Trace.info("Client Config Entity: " + (clientConfig != null ? "configured" : "default"));
	}

	/**
	 * Creates Secrets Manager client builder following Lambda pattern exactly
	 */
	private AWSSecretsManagerClientBuilder getSecretsManagerClientBuilder(ConfigContext ctx, Entity entity, Entity clientConfig) 
			throws EntityStoreException {
		
		// Get credentials provider based on configuration
		AWSCredentialsProvider credentialsProvider = getCredentialsProvider(ctx, entity);
		
		// Create client builder with credentials and client configuration (following Lambda pattern)
		AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard()
			.withCredentials(credentialsProvider);
		
		// Apply client configuration if available (following Lambda pattern exactly)
		if (clientConfig != null) {
			ClientConfiguration clientConfiguration = createClientConfiguration(ctx, clientConfig);
			builder.withClientConfiguration(clientConfiguration);
			Trace.info("Applied custom client configuration");
		} else {
			Trace.debug("Using default client configuration");
		}
		
		return builder;
	}
	
	/**
	 * Gets the appropriate credentials provider based on configuration
	 */
	private AWSCredentialsProvider getCredentialsProvider(ConfigContext ctx, Entity entity) throws EntityStoreException {
		String credentialTypeValue = credentialType.getLiteral();
		Trace.info("=== Credentials Provider Debug ===");
		Trace.info("Credential Type Value: " + credentialTypeValue);
		
		if ("iam".equals(credentialTypeValue)) {
			// Use IAM Role - WebIdentityTokenCredentialsProvider only
			Trace.info("Using IAM Role credentials - WebIdentityTokenCredentialsProvider");
			Trace.info("Credential Type Value: " + credentialTypeValue);
			
			// Debug IRSA configuration
			Trace.info("=== IRSA Debug ===");
			Trace.info("AWS_WEB_IDENTITY_TOKEN_FILE: " + System.getenv("AWS_WEB_IDENTITY_TOKEN_FILE"));
			Trace.info("AWS_ROLE_ARN: " + System.getenv("AWS_ROLE_ARN"));
			Trace.info("AWS_REGION: " + System.getenv("AWS_REGION"));
			
			// Use WebIdentityTokenCredentialsProvider for IAM role
			Trace.info("✅ Using WebIdentityTokenCredentialsProvider for IAM role");
			return new WebIdentityTokenCredentialsProvider();
		} else if ("file".equals(credentialTypeValue)) {
			// Use credentials file
			Trace.info("Credentials Type is 'file', checking credentialsFilePath...");
			Trace.info("Credential Type Value: " + credentialTypeValue);
			String filePath = credentialsFilePath.getLiteral();
			Trace.info("File Path: " + filePath);
			Trace.info("File Path is null: " + (filePath == null));
			Trace.info("File Path is empty: " + (filePath != null && filePath.trim().isEmpty()));
			if (filePath != null && !filePath.trim().isEmpty()) {
				try {
					Trace.info("Using AWS credentials file: " + filePath);
					// Create ProfileCredentialsProvider with file path and default profile
					return new ProfileCredentialsProvider(filePath, "default");
				} catch (Exception e) {
					Trace.error("Error loading credentials file: " + e.getMessage());
					Trace.info("Falling back to DefaultAWSCredentialsProviderChain");
					return new DefaultAWSCredentialsProviderChain();
				}
			} else {
				Trace.info("Credentials file path not specified, using DefaultAWSCredentialsProviderChain");
				return new DefaultAWSCredentialsProviderChain();
			}
		} else {
			// Use explicit credentials via AWSFactory (following Lambda pattern)
			Trace.info("Using explicit AWS credentials via AWSFactory");
			Trace.info("Credential Type Value: " + credentialTypeValue);
			try {
				AWSCredentials awsCredentials = AWSFactory.getCredentials(ctx, entity);
				Trace.info("AWSFactory.getCredentials() successful");
				return getAWSCredentialsProvider(awsCredentials);
			} catch (Exception e) {
				Trace.error("Error getting explicit credentials: " + e.getMessage());
				Trace.info("Falling back to DefaultAWSCredentialsProviderChain");
				return new DefaultAWSCredentialsProviderChain();
			}
		}
	}
	
	/**
	 * Creates ClientConfiguration from entity (following Lambda pattern exactly)
	 */
	private ClientConfiguration createClientConfiguration(ConfigContext ctx, Entity entity) throws EntityStoreException {
		ClientConfiguration clientConfig = new ClientConfiguration();
		
		if (entity == null) {
			Trace.debug("using empty default ClientConfiguration");
			return clientConfig;
		}
		
		// Apply configuration settings with optimized single access
		setIntegerConfig(clientConfig, entity, "connectionTimeout", ClientConfiguration::setConnectionTimeout);
		setIntegerConfig(clientConfig, entity, "maxConnections", ClientConfiguration::setMaxConnections);
		setIntegerConfig(clientConfig, entity, "maxErrorRetry", ClientConfiguration::setMaxErrorRetry);
		setStringConfig(clientConfig, entity, "protocol", (config, value) -> {
			try {
				config.setProtocol(Protocol.valueOf(value));
			} catch (IllegalArgumentException e) {
				Trace.error("Invalid protocol value: " + value);
			}
		});
		setIntegerConfig(clientConfig, entity, "socketTimeout", ClientConfiguration::setSocketTimeout);
		setStringConfig(clientConfig, entity, "userAgent", ClientConfiguration::setUserAgent);
		setStringConfig(clientConfig, entity, "proxyHost", ClientConfiguration::setProxyHost);
		setIntegerConfig(clientConfig, entity, "proxyPort", ClientConfiguration::setProxyPort);
		setStringConfig(clientConfig, entity, "proxyUsername", ClientConfiguration::setProxyUsername);
		setEncryptedConfig(clientConfig, ctx, entity, "proxyPassword");
		setStringConfig(clientConfig, entity, "proxyDomain", ClientConfiguration::setProxyDomain);
		setStringConfig(clientConfig, entity, "proxyWorkstation", ClientConfiguration::setProxyWorkstation);
		
		// Handle socket buffer size hints (both must exist)
		try {
			Integer sendHint = entity.getIntegerValue("socketSendBufferSizeHint");
			Integer receiveHint = entity.getIntegerValue("socketReceiveBufferSizeHint");
			if (sendHint != null && receiveHint != null) {
				clientConfig.setSocketBufferSizeHints(sendHint, receiveHint);
			}
		} catch (Exception e) {
			// Both fields don't exist, skip silently
		}
		
		return clientConfig;
	}
	
	/**
	 * Optimized method to set integer configuration with single access
	 */
	private void setIntegerConfig(ClientConfiguration config, Entity entity, String fieldName, 
			java.util.function.BiConsumer<ClientConfiguration, Integer> setter) {
		try {
			Integer value = entity.getIntegerValue(fieldName);
			if (value != null) {
				setter.accept(config, value);
			}
		} catch (Exception e) {
			// Field doesn't exist, skip silently
		}
	}
	
	/**
	 * Optimized method to set string configuration with single access
	 */
	private void setStringConfig(ClientConfiguration config, Entity entity, String fieldName, 
			java.util.function.BiConsumer<ClientConfiguration, String> setter) {
		try {
			String value = entity.getStringValue(fieldName);
			if (value != null && !value.trim().isEmpty()) {
				setter.accept(config, value);
			}
		} catch (Exception e) {
			// Field doesn't exist, skip silently
		}
	}
	
	/**
	 * Optimized method to set encrypted configuration
	 */
	private void setEncryptedConfig(ClientConfiguration config, ConfigContext ctx, Entity entity, String fieldName) {
		try {
			byte[] encryptedBytes = ctx.getCipher().decrypt(entity.getEncryptedValue(fieldName));
			config.setProxyPassword(new String(encryptedBytes));
		} catch (GeneralSecurityException e) {
			Trace.error("Error decrypting " + fieldName + ": " + e.getMessage());
		} catch (Exception e) {
			// Field doesn't exist, skip silently
		}
	}
	
	/**
	 * Creates AWSCredentialsProvider (following Lambda pattern)
	 */
	private AWSCredentialsProvider getAWSCredentialsProvider(final AWSCredentials awsCredentials) {
		return new AWSCredentialsProvider() {
			public AWSCredentials getCredentials() {
				return awsCredentials;
			}
			public void refresh() {}
		};
	}

	@Override
	public boolean invoke(Circuit circuit, Message message) throws CircuitAbortException {
		try {
			if (secretsManagerClientBuilder == null) {
				Trace.error("AWS Secrets Manager client builder was not configured");
				message.put("aws.secretsmanager.error", "AWS Secrets Manager client builder was not configured");
				return false;
			}
			
			// Get dynamic values using selectors (following Lambda pattern)
			String secretNameValue = secretName.substitute(message);
			String regionValue = secretRegion.substitute(message);
			String maxRetriesValue = maxRetries.substitute(message);
			String retryDelayValue = retryDelay.substitute(message);
			String credentialTypeValue = credentialType.substitute(message);
			String credentialsFilePathValue = credentialsFilePath.substitute(message);

			Trace.info("=== Secrets Manager Invocation Debug ===");
			Trace.info("Secret Name: " + secretNameValue);
			Trace.info("Region: " + regionValue);
			Trace.info("Max Retries: " + maxRetriesValue);
			Trace.info("Retry Delay: " + retryDelayValue);
			Trace.info("Credential Type: " + credentialTypeValue);
			Trace.info("Credentials File Path: " + credentialsFilePathValue);
			
			// Set default values using helper methods
			maxRetriesValue = getDefaultValue(maxRetriesValue, "3");
			retryDelayValue = getDefaultValue(retryDelayValue, "1000");
			credentialTypeValue = getDefaultValue(credentialTypeValue, "local");
			
			// Validate required fields
			if (!validateRequiredField(secretNameValue, "Secret name", message)) {
				return false;
			}
			
			// Parse retry configuration with validation
			int maxRetriesInt = parseIntegerWithDefault(maxRetriesValue, 3, "maxRetries");
			int retryDelayInt = parseIntegerWithDefault(retryDelayValue, 1000, "retryDelay");
			
			Trace.info("Invoking Secrets Manager with retry configuration: maxRetries=" + maxRetriesInt + ", retryDelay=" + retryDelayInt);
			
			// Try to get secret with retry logic
			return executeWithRetry(secretNameValue, regionValue, maxRetriesInt, retryDelayInt, message);
		} catch (Exception e) {
			Trace.error("Unexpected error in AWS Secrets Manager processor: " + e.getMessage(), e);
			message.put("aws.secretsmanager.error", "Unexpected error: " + e.getMessage());
			message.put("aws.secretsmanager.status.code", "500");
			return false;
		}
	}

	/**
	 * Helper method to get default value if the provided value is null or empty
	 */
	private String getDefaultValue(String value, String defaultValue) {
		return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
	}

	/**
	 * Helper method to validate required fields
	 */
	private boolean validateRequiredField(String value, String fieldName, Message message) {
		if (value == null || value.trim().isEmpty()) {
			Trace.error(fieldName + " not specified");
			message.put("aws.secretsmanager.error", fieldName + " not specified");
			return false;
		}
		return true;
	}

	/**
	 * Helper method to parse integer with default value
	 */
	private int parseIntegerWithDefault(String value, int defaultValue, String fieldName) {
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			Trace.info("Invalid " + fieldName + " value, using default " + defaultValue);
			return defaultValue;
		}
	}

	/**
	 * Helper method to execute with retry logic
	 */
	private boolean executeWithRetry(String secretNameValue, String regionValue, int maxRetriesInt, int retryDelayInt, Message message) {
		Exception lastException = null;
		
		for (int attempt = 1; attempt <= maxRetriesInt; attempt++) {
			try {
				Trace.info("Attempt " + attempt + " of " + maxRetriesInt);
				
				// Create Secrets Manager client with region
				AWSSecretsManager secretsManager = secretsManagerClientBuilder.withRegion(regionValue).build();
				
				Trace.info("Attempting to get secret: " + secretNameValue + " (attempt " + attempt + ")");
				
				GetSecretValueRequest request = new GetSecretValueRequest()
					.withSecretId(secretNameValue);
				
				GetSecretValueResult result = secretsManager.getSecretValue(request);
				
				// Success - process result
				processSecretResult(message, result);
				Trace.info("Successfully retrieved secret: " + secretNameValue);
				return true;
				
			} catch (ResourceNotFoundException e) {
				Trace.error("Secret not found: " + secretNameValue);
				message.put("aws.secretsmanager.error", "Secret not found: " + secretNameValue);
				message.put("aws.secretsmanager.status.code", "404");
				return false;
				
			} catch (InvalidRequestException | InvalidParameterException e) {
				Trace.error("Invalid request for secret: " + secretNameValue + " - " + e.getMessage());
				message.put("aws.secretsmanager.error", "Invalid request: " + e.getMessage());
				message.put("aws.secretsmanager.status.code", "400");
				return false;
				
			} catch (DecryptionFailureException e) {
				Trace.error("Decryption failure for secret: " + secretNameValue + " - " + e.getMessage());
				message.put("aws.secretsmanager.error", "Decryption failure: " + e.getMessage());
				message.put("aws.secretsmanager.status.code", "500");
				return false;
				
			} catch (InternalServiceErrorException e) {
				lastException = e;
				Trace.info("Internal service error for secret: " + secretNameValue + " (attempt " + attempt + ") - " + e.getMessage());
				
				if (attempt < maxRetriesInt) {
					try {
						Thread.sleep(retryDelayInt);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			} catch (Exception e) {
				lastException = e;
				Trace.info("Unexpected error for secret: " + secretNameValue + " (attempt " + attempt + ") - " + e.getMessage());
				
				if (attempt < maxRetriesInt) {
					try {
						Thread.sleep(retryDelayInt);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}
		
		// All retries failed
		Trace.error("Failed to retrieve secret after " + maxRetriesInt + " attempts: " + secretNameValue);
		message.put("aws.secretsmanager.error", "Failed to retrieve secret after " + maxRetriesInt + " attempts");
		message.put("aws.secretsmanager.status.code", "500");
		return false;
	}

	/**
	 * Process the secret result and add to message
	 */
	private void processSecretResult(Message message, GetSecretValueResult result) {
		if (result.getSecretString() != null) {
			message.put("aws.secretsmanager.value", result.getSecretString());
		} else if (result.getSecretBinary() != null) {
			// Convert binary to base64 string if necessary
			String base64Value = java.util.Base64.getEncoder().encodeToString(result.getSecretBinary().array());
			message.put("aws.secretsmanager.value", base64Value);
			message.put("aws.secretsmanager.value.type", "binary");
		}
		
		message.put("aws.secretsmanager.status.code", "200");
		message.put("aws.secretsmanager.arn", result.getARN());
		message.put("aws.secretsmanager.name", result.getName());
		message.put("aws.secretsmanager.version.id", result.getVersionId());
		
		if (result.getVersionStages() != null && !result.getVersionStages().isEmpty()) {
			message.put("aws.secretsmanager.version.stages", String.join(",", result.getVersionStages()));
		}
	}
}
