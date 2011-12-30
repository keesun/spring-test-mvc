/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.web.server.setup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockRequestDispatcher;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.server.MockMvc;
import org.springframework.util.Assert;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;

/**
 * A {@link MockMvc} builder that helps to configure and initialize a WebApplicationContext
 * context before looking up Spring MVC components in it.
 * 
 * <p>The WebApplicationContext can be configured with the path to the web application root 
 * directory (classpath or file system relative), specific profiles can be activated, or 
 * {@link ApplicationContextInitializer}s applied.
 * 
 * @author Rossen Stoyanchev
 * @author Keesun Baik
 */
public class ContextMockMvcBuilder extends ContextMockMvcBuilderSupport {

	private String webResourceBasePath = "";

	private final List<Class<?>> classes = new ArrayList<Class<?>>();

	private final List<String> locations = new ArrayList<String>();
	
	private final List<String> profiles = new ArrayList<String>(); 

	private ResourceLoader webResourceLoader = new FileSystemResourceLoader();

	/**
     * Protected constructor. Not intended for direct instantiation.
     * @see MockMvcBuilders#annotationConfigSetup(Class...)
     * @see MockMvcBuilders#xmlConfigSetup(String...)
     * @param classes one or more @{@link Configuration} classes
	 */
	protected ContextMockMvcBuilder() {
	}

    /**
	 * Specify the location of the web application root directory. 
	 * <p>If {@code isClasspathRelative} is "false" the directory is interpreted either as being 
	 * relative to the JVM working directory (e.g. "src/main/webapp") or as a fully qualified 
	 * file system path (e.g. "file:///home/user/webapp"). 
	 * <p>Otherwise if {@code isClasspathRelative} is "true" the directory should be relative 
	 * to the classpath (e.g. "org/examples/myapp/config"). 
	 *  
	 * @param warRootDir the Web application root directory (should not end with a slash)
	 */
	public ContextMockMvcBuilder webAppRootDir(String warRootDir, boolean isClasspathRelative) {
		this.webResourceBasePath = warRootDir;
		this.webResourceLoader = isClasspathRelative ? new DefaultResourceLoader() : new FileSystemResourceLoader();
		return this;
	}
	
	/**
	 * TODO
	 */
	public ContextMockMvcBuilder classes(Class<?>... configClasses) {
		this.classes.addAll(Arrays.asList(configClasses));
		return this;
	}

	/**
	 * TODO
	 */
	public ContextMockMvcBuilder locations(String... configLocations) {
		this.locations.addAll(Arrays.asList(configLocations));
		return this;
	}
	
	/**
	 * Activate the given profiles before the application context is "refreshed".
	 */
	public ContextMockMvcBuilder activeProfiles(String...profiles) {
		this.profiles.addAll(Arrays.asList(profiles));
		return this;
	}
	
	@Override
	protected ServletContext initServletContext() {
		return new MockServletContext(this.webResourceBasePath, this.webResourceLoader) {
			// Required for DefaultServletHttpRequestHandler...
			public RequestDispatcher getNamedDispatcher(String path) {
				return (path.equals("default")) ? new MockRequestDispatcher(path) : super.getNamedDispatcher(path); 
			}			
		};
	}

	@Override
	protected final WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		
		Assert.isTrue(!this.classes.isEmpty() || !this.locations.isEmpty(), 
				"At least one @Configuration class or XML config location is required");
		
		StandardEnvironment environment = new StandardEnvironment();
		if (!this.profiles.isEmpty()) {
			environment.setActiveProfiles(this.profiles.toArray(new String[this.profiles.size()]));
		}

		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		if(!classes.isEmpty()) {
			AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(beanFactory);
			reader.setEnvironment(environment);
			reader.register(classes.toArray(new Class<?>[classes.size()]));
		}
		if(!locations.isEmpty()) {
			XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
			reader.setEnvironment(environment);
			reader.loadBeanDefinitions(locations.toArray(new String[locations.size()]));
		}
		
		ConfigurableWebApplicationContext applicationContext = new GenericWebApplicationContext(beanFactory);
		applicationContext.setServletContext(servletContext);
		applicationContext.setEnvironment(environment);
		applicationContext.refresh();
		
		return applicationContext;
	}

}
