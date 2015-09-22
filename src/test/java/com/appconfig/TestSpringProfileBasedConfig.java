package com.appconfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

@ActiveProfiles("QA")
@ContextConfiguration("classpath:META-INF/spring/test-spring-configurer.xml")
public class TestSpringProfileBasedConfig extends
		AbstractTestNGSpringContextTests {

	@Autowired
	public SampleClass clazz;

	@Test
	public void testProperties() throws Exception {

		assertNotNull(clazz.getSomeValue1());
		assertNotNull(clazz.getSomeValue2());
		assertNotNull(clazz.getSomeValue4());
		assertNotNull(clazz.getSomeOtherValue());

		assertEquals(clazz.getSomeValue1(), "custom");
		assertEquals(clazz.getSomeValue2(), "value2");
		assertEquals(clazz.getSomeValue4(), "custom-custom2");
		assertEquals(clazz.getSomeOtherValue(), "custom2");

		assertNotNull(clazz.getPropertyValue("property.1.name", String.class));
		assertEquals(clazz.getPropertyValue("property.1.name", String.class),
				"custom");
	}

}
