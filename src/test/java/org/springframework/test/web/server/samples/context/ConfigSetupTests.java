package org.springframework.test.web.server.samples.context;

import static org.springframework.test.web.server.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.server.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.server.MockMvc;
import org.springframework.test.web.server.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Keesun Baik
 * @author Rossen Stoyanchev
 */
public class ConfigSetupTests {

	private String warRootDir = "src/test/resources/META-INF/web-resources";

	private boolean isClasspathRelative = false;

	@Test
	public void classAndLocation() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.annotationConfigSetup(WebConfig.class)
				.locations("classpath:/org/springframework/test/web/server/samples/context/ConfigSetupTests-context.xml")
				.webAppRootDir(warRootDir, isClasspathRelative)
				.build();

		mockMvc.perform(get("/test")).andExpect(status().isOk()).andExpect(content().string("success"));
	}

	@Test
	public void profile() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.annotationConfigSetup(WebConfig.class)
				.locations("classpath:/org/springframework/test/web/server/samples/context/ConfigSetupTests-context.xml")
				.activeProfiles("flintstones")
				.webAppRootDir(warRootDir, isClasspathRelative)
				.build();
		
		mockMvc.perform(get("/fred")).andExpect(status().isOk()).andExpect(content().string("yabadabadoo"));
	}
	
    
    @Configuration
    @EnableWebMvc
    static class WebConfig extends WebMvcConfigurerAdapter { }

    @SuppressWarnings("unused")
	@Controller
	private static class TestController {
		@RequestMapping("/test")
		public @ResponseBody String test(){
			return "success";
		}
	}

    @SuppressWarnings("unused")
	@Controller
	private static class FlintstonesController {
		@RequestMapping("/fred")
		public @ResponseBody String respond(){
			return "yabadabadoo";
		}
	}

}
