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
 * Retrieves secrets from AWS Secrets Manager
 */
public class GetSecretValueFilter extends DefaultFilter {

	@Override
	protected final void setDefaultPropertyDefs() {
		this.reqProps.add(new PropDef("content.body", Body.class));
		this.reqProps.add(new PropDef("http.headers", HeaderSet.class));
		genProps.add(new PropDef("aws.secretsmanager.value", String.class));
		genProps.add(new PropDef("aws.secretsmanager.status.code", Integer.class));
		genProps.add(new PropDef("aws.secretsmanager.error", String.class));
		genProps.add(new PropDef("aws.secretsmanager.arn", String.class));
		genProps.add(new PropDef("aws.secretsmanager.name", String.class));
		genProps.add(new PropDef("aws.secretsmanager.version.id", String.class));
		genProps.add(new PropDef("aws.secretsmanager.version.stages", String.class));
		genProps.add(new PropDef("aws.secretsmanager.value.type", String.class));
	}

	@Override
	public void configure(ConfigContext ctx, com.vordel.es.Entity entity) throws EntityStoreException {
		super.configure(ctx, entity);
	}

	@Override
	public Class<? extends MessageProcessor> getMessageProcessorClass() {
		return GetSecretValueProcessor.class;
	}

	public Class<?> getConfigPanelClass() {
		try {
			return Class.forName("com.axway.aws.secretsmanager.GetSecretValueFilterUI");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("UI class not found", e);
		}
	}

} 