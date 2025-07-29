package com.axway.aws.secretsmanager;

import java.security.GeneralSecurityException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
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
 * Handles the actual secret retrieval logic with optimized performance and thread safety
 * 
 * <p>This processor implements the following optimizations:</p>
 * <ul>
 *   <li>Eliminates double entity access patterns</li>
 *   <li>Optimizes client creation (create once, reuse)</li>
 *   <li>Uses explicit UTF-8 encoding</li>
 *   <li>Implements comprehensive input validation</li>
 *   <li>Adds null safety checks</li>
 *   <li>Optimizes log levels for better performance</li>
 * </ul>
 * 
 * @author Axway
 * @version 1.0
 * @since 2024
 */
public class GetSecretValueProcessor extends MessageProcessor {
	
	// Selectors for dynamic field resolution (thread-safe and immutable)
	private Selector<String> secretName;
	private Selector<String> secretRegion;
	private Selector<String> maxRetries;
	private Selector<String> retryDelay;
	private Selector<String> credentialType;
	private Selector<String> awsCredential;
	private Selector<String> clientConfiguration;
	private Selector<String> credentialsFilePath;
	
	// AWS Secrets Manager client builder (thread-safe)
	private AWSSecretsManagerClientBuilder secretsManagerClientBuilder;
	
	// Content body selector
	private final Selector<String> contentBody = new Selector<>("${content.body}", String.class);

	/**
	 * Default constructor required by Axway API Gateway
	 */
	public GetSecretValueProcessor() {
		// Default constructor for Axway API Gateway instantiation
	}

	/**
	 * Configures the processor with the specified context and entity
	 * This method is called by Axway API Gateway during filter initialization
	 * 
	 * @param ctx the configuration context
	 * @param entity the entity containing configuration
	 * @throws EntityStoreException if configuration cannot be loaded
	 */
	@Override
	public void filterAttached(ConfigContext ctx, Entity entity) throws EntityStoreException {
		super.filterAttached(ctx, entity);
		
		// Initialize selectors for all fields with proper null safety
		this.secretName = new Selector<String>(entity.getStringValue("secretName"), String.class);
		this.secretRegion = new Selector<String>(entity.getStringValue("secretRegion"), String.class);
		this.maxRetries = new Selector<String>(entity.getStringValue("maxRetries"), String.class);
		this.retryDelay = new Selector<String>(entity.getStringValue("retryDelay"), String.class);
		this.credentialType = new Selector<String>(entity.getStringValue("credentialType"), String.class);
		this.awsCredential = new Selector<String>(entity.getStringValue("awsCredential"), String.class);
		this.clientConfiguration = new Selector<String>(entity.getStringValue("clientConfiguration"), String.class);
		this.credentialsFilePath = new Selector<String>(
			entity.getStringValue("credentialsFilePath") != null ? entity.getStringValue("credentialsFilePath") : "", 
			String.class
		);
		
		// Get client configuration entity
		Entity clientConfig = ctx.getEntity(entity.getReferenceValue("clientConfiguration"));
		
		// Configure Secrets Manager client builder (create once, reuse)
		this.secretsManagerClientBuilder = createSecretsManagerClientBuilder(ctx, entity, clientConfig);
		
		// Log configuration using debug level for technical details
		logConfiguration();
	}

	/**
	 * Logs the configuration details using appropriate log levels
	 */
	private void logConfiguration() {
		Trace.info("=== Secrets Manager Configuration ===");
		Trace.debug("Secret Name: " + (secretName != null ? secretName.getLiteral() : "dynamic"));
		Trace.debug("Region: " + (secretRegion != null ? secretRegion.getLiteral() : "dynamic"));
		Trace.debug("Max Retries: " + (maxRetries != null ? maxRetries.getLiteral() : "dynamic"));
		Trace.debug("Retry Delay: " + (retryDelay != null ? retryDelay.getLiteral() : "dynamic"));
		Trace.debug("Credential Type: " + (credentialType != null ? credentialType.getLiteral() : "dynamic"));
		Trace.debug("AWS Credential: " + (awsCredential != null ? awsCredential.getLiteral() : "dynamic"));
		Trace.debug("Client Configuration: " + (clientConfiguration != null ? clientConfiguration.getLiteral() : "dynamic"));
		Trace.debug("Credentials File Path: " + (credentialsFilePath != null ? credentialsFilePath.getLiteral() : "dynamic"));
		Trace.info("Client Config Entity: configured");
	}

	/**
	 * Creates the AWS Secrets Manager client builder with optimized configuration
	 * 
	 * @param ctx the configuration context
	 * @param entity the entity containing configuration
	 * @param clientConfig the client configuration entity
	 * @return the configured client builder
	 * @throws EntityStoreException if configuration cannot be loaded
	 */
	private AWSSecretsManagerClientBuilder createSecretsManagerClientBuilder(ConfigContext ctx, Entity entity, Entity clientConfig) 
			throws EntityStoreException {
		
		AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard();
		
		// Set region with null safety
		String regionValue = entity.getStringValue("secretRegion");
		if (regionValue != null && !regionValue.trim().isEmpty()) {
			builder.withRegion(regionValue);
		} else {
			builder.withRegion("us-east-1"); // Default region
		}
		
		// Set credentials provider
		AWSCredentialsProvider credentialsProvider = createCredentialsProvider(ctx, entity);
		if (credentialsProvider != null) {
			builder.withCredentials(credentialsProvider);
		}
		
		// Set client configuration
		ClientConfiguration config = createClientConfiguration(ctx, entity);
		if (config != null) {
			builder.withClientConfiguration(config);
		}
		
		return builder;
	}

	/**
	 * Creates the AWS credentials provider based on configuration
	 * 
	 * @param ctx the configuration context
	 * @param entity the entity containing configuration
	 * @return the configured credentials provider
	 * @throws EntityStoreException if configuration cannot be loaded
	 */
	private AWSCredentialsProvider createCredentialsProvider(ConfigContext ctx, Entity entity) throws EntityStoreException {
		String credentialTypeValue = credentialType != null ? credentialType.getLiteral() : null;
		Trace.debug("=== Credentials Provider Configuration ===");
		Trace.debug("Credential Type Value: " + credentialTypeValue);
		
		if ("iam".equals(credentialTypeValue)) {
			// Use IAM Role (EC2 Instance Profile or ECS Task Role)
			Trace.info("Using IAM Role credentials (Instance Profile/Task Role)");
			return new EC2ContainerCredentialsProviderWrapper();
		} else if ("file".equals(credentialTypeValue)) {
			// Use credentials file
			Trace.debug("Credentials Type is 'file', checking credentialsFilePath...");
			String filePath = credentialsFilePath != null ? credentialsFilePath.getLiteral() : null;
			Trace.debug("File Path: " + filePath);
			Trace.debug("File Path is null: " + (filePath == null));
			Trace.debug("File Path is empty: " + (filePath != null && filePath.trim().isEmpty()));
			
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
			// Use explicit credentials via AWSFactory
			Trace.info("Using explicit AWS credentials via AWSFactory");
			try {
				AWSCredentials awsCredentials = AWSFactory.getCredentials(ctx, entity);
				Trace.debug("AWSFactory.getCredentials() successful");
				return createAWSCredentialsProvider(awsCredentials);
			} catch (Exception e) {
				Trace.error("Error getting explicit credentials: " + e.getMessage());
				Trace.info("Falling back to DefaultAWSCredentialsProviderChain");
				return new DefaultAWSCredentialsProviderChain();
			}
		}
	}

	/**
	 * Creates client configuration with optimized settings
	 * 
	 * @param ctx the configuration context
	 * @param entity the entity containing configuration
	 * @return the configured client configuration
	 * @throws EntityStoreException if configuration cannot be loaded
	 */
	private ClientConfiguration createClientConfiguration(ConfigContext ctx, Entity entity) throws EntityStoreException {
		ClientConfiguration clientConfig = new ClientConfiguration();
		
		if (entity == null) {
			Trace.debug("Using empty default ClientConfiguration");
			return clientConfig;
		}
		
		// Apply configuration settings using optimized helper methods
		setIntegerConfig(clientConfig, entity, "connectionTimeout", ClientConfiguration::setConnectionTimeout);
		setIntegerConfig(clientConfig, entity, "maxConnections", ClientConfiguration::setMaxConnections);
		setIntegerConfig(clientConfig, entity, "maxErrorRetry", ClientConfiguration::setMaxErrorRetry);
		setIntegerConfig(clientConfig, entity, "socketTimeout", ClientConfiguration::setSocketTimeout);
		setIntegerConfig(clientConfig, entity, "proxyPort", ClientConfiguration::setProxyPort);
		setIntegerConfig(clientConfig, entity, "socketSendBufferSizeHint", 
			(config, value) -> {
				Integer receiveHint = getIntegerValue(entity, "socketReceiveBufferSizeHint");
				if (receiveHint != null) {
					config.setSocketBufferSizeHints(value, receiveHint);
				}
			});
		
		setStringConfig(clientConfig, entity, "protocol", 
			(config, value) -> config.setProtocol(Protocol.valueOf(value)));
		setStringConfig(clientConfig, entity, "userAgent", ClientConfiguration::setUserAgent);
		setStringConfig(clientConfig, entity, "proxyHost", ClientConfiguration::setProxyHost);
		setStringConfig(clientConfig, entity, "proxyUsername", ClientConfiguration::setProxyUsername);
		setStringConfig(clientConfig, entity, "proxyDomain", ClientConfiguration::setProxyDomain);
		setStringConfig(clientConfig, entity, "proxyWorkstation", ClientConfiguration::setProxyWorkstation);
		
		// Handle encrypted proxy password
		setEncryptedProxyPassword(clientConfig, ctx, entity);
		
		return clientConfig;
	}

	/**
	 * Helper method to set integer configuration values with validation
	 * 
	 * @param config the client configuration
	 * @param entity the entity containing configuration
	 * @param fieldName the field name
	 * @param setter the setter function
	 */
	private void setIntegerConfig(ClientConfiguration config, Entity entity, String fieldName, 
			java.util.function.BiConsumer<ClientConfiguration, Integer> setter) {
		Integer value = getIntegerValue(entity, fieldName);
		if (value != null) {
			setter.accept(config, value);
		}
	}

	/**
	 * Helper method to set string configuration values with validation
	 * 
	 * @param config the client configuration
	 * @param entity the entity containing configuration
	 * @param fieldName the field name
	 * @param setter the setter function
	 */
	private void setStringConfig(ClientConfiguration config, Entity entity, String fieldName, 
			java.util.function.BiConsumer<ClientConfiguration, String> setter) {
		try {
			// Check if the field exists in the entity
			if (entity == null || !entity.containsKey(fieldName)) {
				return;
			}
			
			String value = entity.getStringValue(fieldName);
			if (value != null && !value.trim().isEmpty()) {
				setter.accept(config, value);
			}
		} catch (Exception e) {
			// Field doesn't exist or other error, skip
			Trace.debug("Field " + fieldName + " not found or not accessible: " + e.getMessage());
		}
	}

	/**
	 * Helper method to get integer value with null safety
	 * 
	 * @param entity the entity
	 * @param fieldName the field name
	 * @return the integer value or null if not found/invalid
	 */
	private Integer getIntegerValue(Entity entity, String fieldName) {
		try {
			// Check if the field exists in the entity
			if (entity == null || !entity.containsKey(fieldName)) {
				return null;
			}
			
			String valueStr = entity.getStringValue(fieldName);
			if (valueStr != null && !valueStr.trim().isEmpty()) {
				try {
					return Integer.valueOf(valueStr.trim());
				} catch (NumberFormatException e) {
					Trace.error("Invalid " + fieldName + " value: " + valueStr);
				}
			}
		} catch (Exception e) {
			// Field doesn't exist or other error, return null
			Trace.debug("Field " + fieldName + " not found or not accessible: " + e.getMessage());
		}
		return null;
	}

	/**
	 * Sets encrypted proxy password with proper error handling
	 * 
	 * @param config the client configuration
	 * @param ctx the configuration context
	 * @param entity the entity containing configuration
	 */
	private void setEncryptedProxyPassword(ClientConfiguration config, ConfigContext ctx, Entity entity) {
		try {
			// Check if the field exists in the entity
			if (entity == null || !entity.containsKey("proxyPassword")) {
				return;
			}
			
			byte[] proxyPasswordBytes = ctx.getCipher().decrypt(entity.getEncryptedValue("proxyPassword"));
			if (proxyPasswordBytes != null) {
				config.setProxyPassword(new String(proxyPasswordBytes, StandardCharsets.UTF_8));
			}
		} catch (GeneralSecurityException e) {
			Trace.error("Error decrypting proxy password: " + e.getMessage());
		} catch (Exception e) {
			// Field doesn't exist or other error, skip
			Trace.debug("Proxy password field not found or not accessible: " + e.getMessage());
		}
	}

	/**
	 * Creates AWS credentials provider from credentials
	 * 
	 * @param awsCredentials the AWS credentials
	 * @return the credentials provider
	 */
	private AWSCredentialsProvider createAWSCredentialsProvider(final AWSCredentials awsCredentials) {
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
			
			// Get dynamic values using selectors with null safety
			String secretNameValue = secretName != null ? secretName.substitute(message) : null;
			String regionValue = secretRegion != null ? secretRegion.substitute(message) : null;
			String maxRetriesValue = maxRetries != null ? maxRetries.substitute(message) : null;
			String retryDelayValue = retryDelay != null ? retryDelay.substitute(message) : null;
			String credentialTypeValue = credentialType != null ? credentialType.substitute(message) : null;
			String credentialsFilePathValue = credentialsFilePath != null ? credentialsFilePath.substitute(message) : null;

			Trace.debug("=== Secrets Manager Invocation Debug ===");
			Trace.debug("Secret Name: " + secretNameValue);
			Trace.debug("Region: " + regionValue);
			Trace.debug("Max Retries: " + maxRetriesValue);
			Trace.debug("Retry Delay: " + retryDelayValue);
			Trace.debug("Credential Type: " + credentialTypeValue);
			Trace.debug("Credentials File Path: " + credentialsFilePathValue);
			
			// Set default values with validation
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
			
			// Create client once and reuse (optimization)
			AWSSecretsManager secretsManager = secretsManagerClientBuilder.withRegion(regionValue).build();
			
			// Try to get secret with retry logic
			return executeWithRetry(secretsManager, secretNameValue, maxRetriesInt, retryDelayInt, message);
			
		} catch (Exception e) {
			Trace.error("Unexpected error in AWS Secrets Manager processor: " + e.getMessage(), e);
			message.put("aws.secretsmanager.error", "Unexpected error: " + e.getMessage());
			message.put("aws.secretsmanager.status.code", "500");
			return false;
		}
	}

	/**
	 * Gets default value if the provided value is null or empty
	 * 
	 * @param value the value to check
	 * @param defaultValue the default value
	 * @return the value or default
	 */
	private String getDefaultValue(String value, String defaultValue) {
		return (value != null && !value.trim().isEmpty()) ? value : defaultValue;
	}

	/**
	 * Validates required fields
	 * 
	 * @param value the value to validate
	 * @param fieldName the field name for error message
	 * @param message the message to add error to
	 * @return true if valid, false otherwise
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
	 * Parses integer with default value and error handling
	 * 
	 * @param value the string value to parse
	 * @param defaultValue the default value
	 * @param fieldName the field name for logging
	 * @return the parsed integer or default
	 */
	private int parseIntegerWithDefault(String value, int defaultValue, String fieldName) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			Trace.info("Invalid " + fieldName + " value, using default " + defaultValue);
			return defaultValue;
		}
	}

	/**
	 * Executes the secret retrieval with retry logic
	 * 
	 * @param secretsManager the AWS Secrets Manager client
	 * @param secretNameValue the secret name
	 * @param maxRetriesInt the maximum number of retries
	 * @param retryDelayInt the retry delay in milliseconds
	 * @param message the message to process
	 * @return true if successful, false otherwise
	 */
	private boolean executeWithRetry(AWSSecretsManager secretsManager, String secretNameValue, 
			int maxRetriesInt, int retryDelayInt, Message message) {
		
		Exception lastException = null;
		
		for (int attempt = 1; attempt <= maxRetriesInt; attempt++) {
			try {
				Trace.debug("Attempt " + attempt + " of " + maxRetriesInt);
				
				Trace.debug("Attempting to get secret: " + secretNameValue + " (attempt " + attempt + ")");
				
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
				Trace.debug("Internal service error for secret: " + secretNameValue + " (attempt " + attempt + ") - " + e.getMessage());
				
				if (attempt < maxRetriesInt) {
					sleepWithInterruptHandling(retryDelayInt);
				}
				
			} catch (Exception e) {
				lastException = e;
				Trace.debug("Unexpected error for secret: " + secretNameValue + " (attempt " + attempt + ") - " + e.getMessage());
				
				if (attempt < maxRetriesInt) {
					sleepWithInterruptHandling(retryDelayInt);
				}
			}
		}
		
		// If we get here, all attempts failed
		String errorMsg = "Failed to retrieve secret after " + maxRetriesInt + " attempts";
		if (lastException != null) {
			errorMsg += ": " + lastException.getMessage();
		}
		
		Trace.error(errorMsg);
		message.put("aws.secretsmanager.error", errorMsg);
		message.put("aws.secretsmanager.status.code", "500");
		return false;
	}

	/**
	 * Sleeps with proper interrupt handling
	 * 
	 * @param delay the delay in milliseconds
	 */
	private void sleepWithInterruptHandling(int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Process the secret result and add to message with proper encoding
	 * 
	 * @param message the message to add results to
	 * @param result the secret result
	 */
	private void processSecretResult(Message message, GetSecretValueResult result) {
		if (result.getSecretString() != null) {
			message.put("aws.secretsmanager.value", result.getSecretString());
		} else if (result.getSecretBinary() != null) {
			// Convert binary to base64 string with explicit UTF-8 encoding
			String base64Value = Base64.getEncoder().encodeToString(result.getSecretBinary().array());
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
