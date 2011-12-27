package org.springframework.test.web.server.samples.context;

import org.junit.Test;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.server.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;

/**
 * @author Keesun Baik
 */
public class ConfigSetupTests {

	String warRootDir = "src/test/resources/META-INF/web-resources";
	boolean isClasspathRelative = false;

	@Test
	public void ClassesAndLocations() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.annotationConfigSetup(WebConfig.class)
				.locations("classpath:/org/springframework/test/web/server/samples/context/ConfigSetupTests-context.xml")
				.locations("classpath:/org/springframework/test/web/server/samples/context/Additional-context.xml")
				.configureWebAppRootDir(warRootDir, isClasspathRelative)
				.build();

		mockMvc.perform(get("/test")).andExpect(status().isOk()).andExpect(content().string("success"));
	}



    @Test
    public void applyInitializers(){
		SampleApplicationContextInitializer sampleInitializer = new SampleApplicationContextInitializer();

		MockMvc mockMvc = MockMvcBuilders.annotationConfigSetup(WebConfig.class)
				.configureWebAppRootDir(warRootDir, isClasspathRelative)
				.initializers(sampleInitializer)
				.build();

		assertThat(sampleInitializer.isInitialized, is(true));
    }

    @Controller
	private static class TestController {
		@RequestMapping("/test")
		public @ResponseBody String test(){
			return "success";
		}
	}

    /**
     * a Fixture for testing applyInitializers().
     */
	class SampleApplicationContextInitializer implements ApplicationContextInitializer<ConfigurableWebApplicationContext> {

		boolean isInitialized = false;

		public boolean isInitialized() {
			return isInitialized;
		}

		public void initialize(ConfigurableWebApplicationContext configurableWebApplicationContext) {
			this.isInitialized = true;
		}
	}

}
