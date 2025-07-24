package com.axway.aws.secretsmanager;

import org.eclipse.swt.widgets.Composite;

import com.vordel.client.manager.wizard.VordelPage;

public class AWSSecretsManagerFilterPage extends VordelPage {

	public AWSSecretsManagerFilterPage() {
		super("AWSSecretsManagerPage");


		setTitle(resolve("AWS_SECRETSMANAGER_PAGE"));
		setDescription(resolve("AWS_SECRETSMANAGER_PAGE_DESCRIPTION"));
		setPageComplete(true);
	}

	public String getHelpID() {
		 return "com.vordel.rcp.policystudio.filter.help.send_to_s3_bucket_filter_help";
	}

	public boolean performFinish() {
		return true;
	}

	public void createControl(Composite parent) {
		Composite panel = render(parent, getClass().getResourceAsStream("aws_secretsmanager.xml"));
		setControl(panel);
		setPageComplete(true);
	}
} 