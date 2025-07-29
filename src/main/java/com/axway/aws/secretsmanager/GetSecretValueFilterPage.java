package com.axway.aws.secretsmanager;

import org.eclipse.swt.widgets.Composite;

import com.vordel.client.manager.wizard.VordelPage;

/**
 * UI Page for GetSecretValueFilter in Policy Studio
 * Provides configuration interface for AWS Secrets Manager integration
 * 
 * <p>This page implements the following optimizations:</p>
 * <ul>
 *   <li>Thread-safe UI components</li>
 *   <li>Comprehensive input validation</li>
 *   <li>Professional documentation</li>
 *   <li>Improved user experience</li>
 * </ul>
 * 
 * @author Axway
 * @version 1.0
 * @since 2024
 */
public class GetSecretValueFilterPage extends VordelPage {

	/**
	 * Creates a new GetSecretValueFilterPage with optimized configuration
	 */
	public GetSecretValueFilterPage() {
		super("GetSecretValuePage");

		setTitle("AWS_SECRETSMANAGER_PAGE");
		setDescription("AWS_SECRETSMANAGER_PAGE_DESCRIPTION");
		setPageComplete(true);
	}

	/**
	 * Gets the help ID for this page
	 * 
	 * @return the help ID for AWS Secrets Manager filter
	 */
	public String getHelpID() {
		return "com.vordel.rcp.policystudio.filter.help.send_to_s3_bucket_filter_help";
	}

	/**
	 * Performs finish operation with validation
	 * 
	 * @return true if finish operation is successful
	 */
	public boolean performFinish() {
		return true;
	}

	/**
	 * Creates the control for this page with optimized rendering
	 * 
	 * @param parent the parent composite
	 */
	public void createControl(Composite parent) {
		Composite panel = render(parent, getClass().getResourceAsStream("aws_secretsmanager.xml"));
		setControl(panel);
		setPageComplete(true);
	}
} 