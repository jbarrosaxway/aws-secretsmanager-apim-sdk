package com.axway.aws.secretsmanager;

import java.security.GeneralSecurityException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
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
import com.vordel.common.Dictionary;
import com.vordel.config.Circuit;
import com.vordel.config.ConfigContext;
import com.vordel.el.Selector;
import com.vordel.es.Entity;
import com.vordel.es.EntityStoreException;
import com.vordel.es.ESPK;
import com.vordel.security.util.SecureString;
import com.vordel.trace.Trace;
import java.io.File;
import java.nio.ByteBuffer;

public class AWSSecretsManagerProcessor extends MessageProcessor {
	
	// Selectors for dynamic field resolution (following Lambda pattern)
	protected Selector<String> secretName;
	protected Selector<String> secretRegion;
	protected Selector<String> maxRetries;
	protected Selector<String> retryDelay;
	protected Selector<String> credentialType;
	protected Selector<String> awsCredential;
	protected Selector<String> clientConfiguration;
	protected Selector<String> credentialsFilePath;
	protected Selector<String> awsProfile;
	
	// AWS Secrets Manager client builder
	protected AWSSecretsManagerClientBuilder secretsManagerClientBuilder;
	
	// Content body selector
	private Selector<String> contentBody = new Selector<>("${content.body}", String.class);

	public AWSSecretsManagerProcessor() {
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
		this.awsProfile = new Selector(entity.getStringValue("awsProfile") != null ? entity.getStringValue("awsProfile") : "", String.class);
		
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
		Trace.info("AWS Profile: " + (awsProfile != null ? awsProfile.getLiteral() : "dynamic"));
		Trace.info("Client Config Entity: " + (clientConfig != null ? "configured" : "default"));
	}

	private AWSSecretsManagerClientBuilder getSecretsManagerClientBuilder(ConfigContext ctx, Entity entity, Entity clientConfig) 
			throws EntityStoreException {
		
		AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard();
		
		// Set region
		String regionValue = entity.getStringValue("secretRegion");
		if (regionValue != null && !regionValue.trim().isEmpty()) {
			builder.withRegion(regionValue);
		} else {
			builder.withRegion("us-east-1"); // Default region
		}
		
		// Set credentials provider
		AWSCredentialsProvider credentialsProvider = getCredentialsProvider(ctx, entity);
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

	private AWSCredentialsProvider getCredentialsProvider(ConfigContext ctx, Entity entity) throws EntityStoreException {
		String credentialTypeValue = entity.getStringValue("credentialType");
		
		if ("local".equals(credentialTypeValue)) {
			// Local credentials (access key + secret key)
			String awsCredentialValue = entity.getStringValue("awsCredential");
			if (awsCredentialValue != null && !awsCredentialValue.trim().isEmpty()) {
				String[] parts = awsCredentialValue.split(":");
				if (parts.length == 2) {
					return new AWSStaticCredentialsProvider(
						new BasicAWSCredentials(parts[0].trim(), parts[1].trim()));
				} else if (parts.length == 3) {
					return new AWSStaticCredentialsProvider(
						new BasicSessionCredentials(parts[0].trim(), parts[1].trim(), parts[2].trim()));
				}
			}
			Trace.info("Invalid local credentials format, using default provider chain");
			return new DefaultAWSCredentialsProviderChain();
			
		} else if ("file".equals(credentialTypeValue)) {
			// Credentials from file
			String credentialsFilePathValue = entity.getStringValue("credentialsFilePath");
			if (credentialsFilePathValue != null && !credentialsFilePathValue.trim().isEmpty()) {
				try {
					return new com.amazonaws.auth.PropertiesFileCredentialsProvider(credentialsFilePathValue);
				} catch (Exception e) {
					Trace.info("Failed to load credentials from file: " + credentialsFilePathValue + ", using default provider chain");
				}
			}
			return new DefaultAWSCredentialsProviderChain();
			
		} else if ("iam".equals(credentialTypeValue)) {
			// IAM Role (use default provider chain)
			return new DefaultAWSCredentialsProviderChain();
			
		} else if ("profile".equals(credentialTypeValue)) {
			// AWS Profile
			String awsProfileValue = entity.getStringValue("awsProfile");
			if (awsProfileValue != null && !awsProfileValue.trim().isEmpty()) {
				return new ProfileCredentialsProvider(awsProfileValue);
			}
			Trace.info("Profile name not specified, using default provider chain");
			return new DefaultAWSCredentialsProviderChain();
			
		} else {
			// Default: use provider chain
			return new DefaultAWSCredentialsProviderChain();
		}
	}

	private ClientConfiguration createClientConfiguration(ConfigContext ctx, Entity entity) throws EntityStoreException {
		ClientConfiguration config = new ClientConfiguration();
		
		// Set default values
		config.setConnectionTimeout(5000);
		config.setSocketTimeout(10000);
		config.setMaxConnections(50);
		config.setProtocol(Protocol.HTTPS);
		
		// Apply custom configuration if provided
		String clientConfigurationValue = entity.getStringValue("clientConfiguration");
		if (clientConfigurationValue != null && !clientConfigurationValue.trim().isEmpty()) {
			String[] lines = clientConfigurationValue.split("\n");
			for (String line : lines) {
				line = line.trim();
				if (line.startsWith("protocol=")) {
					String protocol = line.substring("protocol=".length()).trim();
					if ("http".equalsIgnoreCase(protocol)) {
						config.setProtocol(Protocol.HTTP);
					} else if ("https".equalsIgnoreCase(protocol)) {
						config.setProtocol(Protocol.HTTPS);
					}
				} else if (line.startsWith("connectionTimeout=")) {
					try {
						int timeout = Integer.parseInt(line.substring("connectionTimeout=".length()).trim());
						config.setConnectionTimeout(timeout);
					} catch (NumberFormatException e) {
						Trace.info("Invalid connectionTimeout value: " + line);
					}
				} else if (line.startsWith("socketTimeout=")) {
					try {
						int timeout = Integer.parseInt(line.substring("socketTimeout=".length()).trim());
						config.setSocketTimeout(timeout);
					} catch (NumberFormatException e) {
						Trace.info("Invalid socketTimeout value: " + line);
					}
				} else if (line.startsWith("maxConnections=")) {
					try {
						int maxConn = Integer.parseInt(line.substring("maxConnections=".length()).trim());
						config.setMaxConnections(maxConn);
					} catch (NumberFormatException e) {
						Trace.info("Invalid maxConnections value: " + line);
					}
				}
			}
		}
		
		return config;
	}

	private boolean containsKey(Entity entity, String fieldName) {
		try {
			return entity.getStringValue(fieldName) != null;
		} catch (Exception e) {
			return false;
		}
	}

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
			// Get dynamic values
			String secretNameValue = secretName != null ? secretName.substitute(message) : null;
			String regionValue = secretRegion != null ? secretRegion.substitute(message) : null;
			String maxRetriesValue = maxRetries != null ? maxRetries.substitute(message) : "3";
			String retryDelayValue = retryDelay != null ? retryDelay.substitute(message) : "1000";
			
			// Set default region if not specified
			if (regionValue == null || regionValue.trim().isEmpty()) {
				regionValue = "us-east-1";
			}
			
			Trace.info("=== Secrets Manager Invocation Debug ===");
			Trace.info("Secret Name: " + secretNameValue);
			Trace.info("Region: " + regionValue);
			Trace.info("Max Retries: " + maxRetriesValue);
			Trace.info("Retry Delay: " + retryDelayValue);
			
			// Validate required fields
			if (secretNameValue == null || secretNameValue.trim().isEmpty()) {
				Trace.error("Secret name not specified");
				message.put("aws.secretsmanager.error", "Secret name not specified");
				return false;
			}
			
			// Parse retry configuration
			int maxRetriesInt = 3;
			try {
				maxRetriesInt = Integer.parseInt(maxRetriesValue);
			} catch (Exception e) {
				Trace.info("Invalid maxRetries value, using default 3");
			}
			
			int retryDelayInt = 1000;
			try {
				retryDelayInt = Integer.parseInt(retryDelayValue);
			} catch (Exception e) {
				Trace.info("Invalid retryDelay value, using default 1000");
			}
			
			// Build client with current region
			AWSSecretsManagerClientBuilder clientBuilder = AWSSecretsManagerClientBuilder.standard()
				.withRegion(regionValue);
			
			// Apply credentials and configuration
			AWSCredentialsProvider credentialsProvider = getCredentialsProvider(null, null);
			if (credentialsProvider != null) {
				clientBuilder.withCredentials(credentialsProvider);
			}
			
			ClientConfiguration config = createClientConfiguration(null, null);
			if (config != null) {
				clientBuilder.withClientConfiguration(config);
			}
			
			AWSSecretsManager secretsManager = clientBuilder.build();
			
			// Try to get secret with retry logic
			GetSecretValueResult result = null;
			Exception lastException = null;
			
			for (int attempt = 0; attempt <= maxRetriesInt; attempt++) {
				try {
					Trace.info("Attempting to get secret: " + secretNameValue + " (attempt " + (attempt + 1) + ")");
					
					GetSecretValueRequest request = new GetSecretValueRequest()
						.withSecretId(secretNameValue);
					
					result = secretsManager.getSecretValue(request);
					
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
					Trace.info("Internal service error for secret: " + secretNameValue + " (attempt " + (attempt + 1) + ") - " + e.getMessage());
					
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
					Trace.info("Unexpected error for secret: " + secretNameValue + " (attempt " + (attempt + 1) + ") - " + e.getMessage());
					
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
			
			// If we get here, all attempts failed
			String errorMsg = "Failed to retrieve secret after " + (maxRetriesInt + 1) + " attempts";
			if (lastException != null) {
				errorMsg += ": " + lastException.getMessage();
			}
			
			Trace.error(errorMsg);
			message.put("aws.secretsmanager.error", errorMsg);
			message.put("aws.secretsmanager.status.code", "500");
			return false;
			
		} catch (Exception e) {
			Trace.error("Unexpected error in AWS Secrets Manager processor: " + e.getMessage(), e);
			message.put("aws.secretsmanager.error", "Unexpected error: " + e.getMessage());
			message.put("aws.secretsmanager.status.code", "500");
			return false;
		}
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
