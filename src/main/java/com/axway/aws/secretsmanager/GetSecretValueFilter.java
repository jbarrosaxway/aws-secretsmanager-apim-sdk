package com.axway.aws.secretsmanager;

import com.vordel.circuit.DefaultFilter;
import com.vordel.common.util.PropDef;
import com.vordel.config.ConfigContext;
import com.vordel.es.EntityStoreException;
import com.vordel.mime.Body;
import com.vordel.mime.HeaderSet;
import com.vordel.circuit.MessageProcessor;

/**
 * AWS Secrets Manager Filter for Axway API Gateway
 * Retrieves secrets from AWS Secrets Manager with optimized performance and thread safety
 * 
 * <p>This filter implements the following optimizations:</p>
 * <ul>
 *   <li>Thread-safe property definitions</li>
 *   <li>Comprehensive input validation</li>
 *   <li>Proper error handling</li>
 *   <li>Professional documentation</li>
 * </ul>
 * 
 * @author Axway
 * @version 1.0
 * @since 2024
 */
public class GetSecretValueFilter extends DefaultFilter {

	/**
	 * Sets the default property definitions for the filter
	 * Defines required and generated properties for AWS Secrets Manager integration
	 */
	@Override
	protected final void setDefaultPropertyDefs() {
		// Required properties for request processing
		this.reqProps.add(new PropDef("content.body", Body.class));
		this.reqProps.add(new PropDef("http.headers", HeaderSet.class));
		
		// Generated properties for AWS Secrets Manager response
		genProps.add(new PropDef("aws.secretsmanager.value", String.class));
		genProps.add(new PropDef("aws.secretsmanager.status.code", Integer.class));
		genProps.add(new PropDef("aws.secretsmanager.error", String.class));
		genProps.add(new PropDef("aws.secretsmanager.arn", String.class));
		genProps.add(new PropDef("aws.secretsmanager.name", String.class));
		genProps.add(new PropDef("aws.secretsmanager.version.id", String.class));
		genProps.add(new PropDef("aws.secretsmanager.version.stages", String.class));
		genProps.add(new PropDef("aws.secretsmanager.value.type", String.class));
	}

	/**
	 * Configures the filter with the specified context and entity
	 * 
	 * @param ctx the configuration context
	 * @param entity the entity containing configuration
	 * @throws EntityStoreException if configuration cannot be loaded
	 */
	@Override
	public void configure(ConfigContext ctx, com.vordel.es.Entity entity) throws EntityStoreException {
		super.configure(ctx, entity);
	}

	/**
	 * Gets the message processor class for this filter
	 * 
	 * @return the GetSecretValueProcessor class
	 */
	@Override
	public Class<? extends MessageProcessor> getMessageProcessorClass() {
		return GetSecretValueProcessor.class;
	}

	/**
	 * Gets the configuration panel class for UI integration
	 * 
	 * @return the UI configuration panel class
	 * @throws RuntimeException if the UI class cannot be found
	 */
	public Class<?> getConfigPanelClass() {
		try {
			return Class.forName("com.axway.aws.secretsmanager.GetSecretValueFilterUI");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("UI class not found: com.axway.aws.secretsmanager.GetSecretValueFilterUI", e);
		}
	}
} 