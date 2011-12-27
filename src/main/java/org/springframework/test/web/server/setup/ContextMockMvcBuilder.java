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

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
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

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	private ConfigurableWebApplicationContext applicationContext;

	private String webResourceBasePath = "";

	private ResourceLoader webResourceLoader = new FileSystemResourceLoader();

	private List<Class<?>> configClassList = new ArrayList<Class<?>>();

	private List<String> configLocationList = new ArrayList<String>();

	private ApplicationContextInitializer<? extends ConfigurableWebApplicationContext>[] initializers;

	/**
     * Protected constructor. Not intended for direct instantiation.
     * @see MockMvcBuilders#annotationConfigSetup(Class...)
     * @see MockMvcBuilders#xmlConfigSetup(String...)
	 */
	public ContextMockMvcBuilder(ConfigurableWebApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public ContextMockMvcBuilder() {
	}

	public ContextMockMvcBuilder(Class<?>... configClasses) {
		addClasses(configClasses);
	}

	public ContextMockMvcBuilder(String... configLocations) {
		addLocations(configLocations);
	}

	public ContextMockMvcBuilder classes(Class<?>... configClasses) {
		addClasses(configClasses);
		return this;
	}

	public ContextMockMvcBuilder locations(String... configLocations) {
		addLocations(configLocations);
		return this;
	}

	private void addClasses(Class<?>[] configClasses) {
		Assert.notEmpty(configClasses, "At least one @Configuration class is required");
		this.configClassList.addAll(Arrays.asList(configClasses));
	}

	private void addLocations(String[] configLocations) {
		Assert.notEmpty(configLocations, "At least one XML config location is required");
		this.configLocationList.addAll(Arrays.asList(configLocations));
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
	public ContextMockMvcBuilder configureWebAppRootDir(String warRootDir, boolean isClasspathRelative) {
		this.webResourceBasePath = warRootDir;
		this.webResourceLoader = isClasspathRelative ? new DefaultResourceLoader() : new FileSystemResourceLoader();
		return this;
	}
	
	/**
	 * Activate the given profiles before the application context is "refreshed".
	 */
	public ContextMockMvcBuilder activateProfiles(String...profiles) {
		this.applicationContext.getEnvironment().setActiveProfiles(profiles);
		return this;
	}

	public <T extends ConfigurableWebApplicationContext> ContextMockMvcBuilder initializers(ApplicationContextInitializer<T>... initializers) {
		this.initializers = initializers;
		return this;
	}

	    /**
	 * Apply the given {@link ApplicationContextInitializer}s before the application context is "refreshed".
	 */
	@SuppressWarnings("unchecked")
	protected <T extends ConfigurableWebApplicationContext>
			ContextMockMvcBuilder applyInitializers(ApplicationContextInitializer<T>... initializers) {
		if(initializers != null) {
			for (ApplicationContextInitializer<T> initializer : initializers) {
			initializer.initialize((T) this.applicationContext);
			}
        }
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
	protected WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		if(this.applicationContext == null) {
			DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
			if(!configClassList.isEmpty()) {
				AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(beanFactory);
				reader.register(configClassList.toArray(new Class<?>[configClassList.size()]));
			}
			if(!configLocationList.isEmpty()) {
				XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
				reader.loadBeanDefinitions(configLocationList.toArray(new String[configLocationList.size()]));
			}
			this.applicationContext = new GenericWebApplicationContext(beanFactory);
		}
		this.applicationContext.setServletContext(servletContext);
		applyInitializers(this.initializers);
		this.applicationContext.refresh();
		return this.applicationContext;
	}

}
