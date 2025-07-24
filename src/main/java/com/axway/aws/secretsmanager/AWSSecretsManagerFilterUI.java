package com.axway.aws.secretsmanager;

import java.util.Vector;

import com.vordel.client.manager.filter.log.LogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.vordel.client.manager.Images;
import com.vordel.client.manager.filter.DefaultGUIFilter;
import com.vordel.client.manager.wizard.VordelPage;

public class AWSSecretsManagerFilterUI extends DefaultGUIFilter {
	public Vector<VordelPage> getPropertyPages() {
		Vector<VordelPage> pages = new Vector<>();
		pages.add(new AWSSecretsManagerFilterPage());
		pages.add(createLogPage());
		return pages;
	}

	public LogPage createLogPage() {
		return new LogPage();
	}

	public String[] getCategories() {
		return new String[] { resolve("FILTER_GROUP_AWS_SECRETSMANAGER") };
	}

	private static final String IMAGE_KEY = "amazon";

	public String getSmallIconId() {
		return IMAGE_KEY;
	}

	public Image getSmallImage() {
		return Images.getImageRegistry().get(getSmallIconId());
	}

	public ImageDescriptor getSmallIcon() {
		return Images.getImageDescriptor(getSmallIconId());
	}


	public String getTypeName() {
		return resolve("AWS_SECRETSMANAGER_FILTER");
	}
} 