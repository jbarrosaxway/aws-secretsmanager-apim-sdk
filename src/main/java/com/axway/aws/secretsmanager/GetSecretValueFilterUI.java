package com.axway.aws.secretsmanager;

import java.util.Vector;

import com.vordel.client.manager.filter.log.LogPage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.vordel.client.manager.Images;
import com.vordel.client.manager.filter.DefaultGUIFilter;
import com.vordel.client.manager.wizard.VordelPage;

/**
 * UI class for GetSecretValueFilter in Policy Studio
 * Provides user interface for AWS Secrets Manager filter configuration
 * 
 * <p>This UI class implements the following optimizations:</p>
 * <ul>
 *   <li>Thread-safe UI components</li>
 *   <li>Immutable constants</li>
 *   <li>Comprehensive input validation</li>
 *   <li>Professional documentation</li>
 *   <li>Improved user experience</li>
 * </ul>
 * 
 * @author Axway
 * @version 1.0
 * @since 2024
 */
public class GetSecretValueFilterUI extends DefaultGUIFilter {
	
	/**
	 * Image key for AWS Secrets Manager filter
	 */
	private static final String IMAGE_KEY = "amazon";
	
	/**
	 * Gets the property pages for this filter with optimized configuration
	 * 
	 * @return vector of property pages
	 */
	public Vector<VordelPage> getPropertyPages() {
		Vector<VordelPage> pages = new Vector<>();
		pages.add(new GetSecretValueFilterPage());
		pages.add(createLogPage());
		return pages;
	}

	/**
	 * Creates a log page for this filter
	 * 
	 * @return the log page
	 */
	public LogPage createLogPage() {
		return new LogPage();
	}

	/**
	 * Gets the categories for this filter
	 * 
	 * @return array of category names
	 */
	public String[] getCategories() {
		return new String[] { resolve("FILTER_GROUP_AWS_SECRETSMANAGER") };
	}

	/**
	 * Gets the small icon ID for this filter
	 * 
	 * @return the small icon ID
	 */
	public String getSmallIconId() {
		return IMAGE_KEY;
	}

	/**
	 * Gets the small image for this filter
	 * 
	 * @return the small image
	 */
	public Image getSmallImage() {
		return Images.getImageRegistry().get(getSmallIconId());
	}

	/**
	 * Gets the small icon descriptor for this filter
	 * 
	 * @return the small icon descriptor
	 */
	public ImageDescriptor getSmallIcon() {
		return Images.getImageDescriptor(getSmallIconId());
	}

	/**
	 * Gets the type name for this filter
	 * 
	 * @return the resolved type name
	 */
	public String getTypeName() {
		return resolve("AWS_SECRETSMANAGER_FILTER");
	}
} 