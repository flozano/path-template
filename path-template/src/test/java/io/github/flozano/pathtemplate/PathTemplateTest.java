package io.github.flozano.pathtemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class PathTemplateTest {

	@Test
	public void testSimpleNPE() {
		try {
			PathTemplate.render("a/{b}", null);
			fail();
		} catch (NullPointerException e) {
			// OK
		}
	}

	@Test
	public void testEmptyMapVariables() {
		try {
			PathTemplate.render("a/{b}", Map.of());
			fail();
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void testMissingVariableInMap() {
		try {
			PathTemplate.render("a/{b}/{c}", Map.of("b", "value"));
			fail();
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

	@Test
	public void testExtraVariablesInMap() {
		assertEquals("a/value", PathTemplate.render("a/{b}", Map.of("b", "value", "c", "unused")));
	}

	@Test
	public void testRepeatedVariables() {
		assertEquals("value/value/VALUE", PathTemplate.render("{b}/{b#another}/{b#third?uc}", Map.of("b", "value")));
	}

	@Test
	public void testBracesInVariableValues() {
		assertEquals("a/{value}", PathTemplate.render("a/{b}", Map.of("b", "{value}")));
	}

	@Test
	public void testMapWithNullValues() {
		Map<String, String> values = new HashMap<>();
		values.put("b", null);
		assertEquals("a/", PathTemplate.render("a/{b}", values));
	}

	@Test
	public void testSimpleVariablesWithMultipleAppearances() {
		assertEquals("whatever/123/123",
				PathTemplate.render("whatever/{xxx#firstappearance}/{xxx#second}", Map.of("xxx", "123")));
	}

	@Test
	public void testSimpleVariables() {
		assertEquals("whatever/123/456",
				PathTemplate.render("whatever/{xxx}/{yyy}", Map.of("xxx", "123", "yyy", "456")));

		assertEquals("/whatever/123/456",
				PathTemplate.render("/whatever/{xxx}/{yyy}", Map.of("xxx", "123", "yyy", "456")));
	}

	@Test
	public void testVariablesWithSlashes() {
		assertEquals("/whatever/123/456/789",
				PathTemplate.render("/whatever/{xxx}/{yyy?slashok}", Map.of("xxx", "123", "yyy", "456/789")));
	}

	@Test
	public void testVariablesWithLc() {
		assertEquals("whatever/name/surname",
				PathTemplate.render("whatever/{xxx?lc}/{yyy?lc}", Map.of("xxx", "NAME", "yyy", "SURNAME")));
	}

	@Test
	public void testVariablesWithUc() {
		assertEquals("whatever/NAME/SURNAME",
				PathTemplate.render("whatever/{xxx?uc}/{yyy?uc}", Map.of("xxx", "name", "yyy", "surname")));
	}

	@Test
	public void testVariablesWithUcfirst() {
		assertEquals("whatever/Name/Surname/S",
				PathTemplate.render("whatever/{xxx?ucfirst}/{yyy?ucfirst}/{zzz?ucfirst}",
						Map.of("xxx", "name", "yyy", "surname", "zzz", "s")));
	}

	@Test
	public void testVariablesWithLcfirst() {
		assertEquals("whatever/nAME/sURNAME",
				PathTemplate.render("whatever/{xxx?lcfirst}/{yyy?lcfirst}", Map.of("xxx", "NAME", "yyy", "SURNAME")));
	}

	@Test
	public void testBadTemplates() {
		try {
			PathTemplate.render("a/{b}/", Map.of("b", "123"));
			fail();
		} catch (IllegalArgumentException e) {
			// OK
		}

		try {
			PathTemplate.render("/a/{b}/", Map.of("b", "123"));
			fail();
		} catch (IllegalArgumentException e) {
			// OK
		}

		try {
			PathTemplate.render("//a/{b}/", Map.of("b", "123"));
			fail();
		} catch (IllegalArgumentException e) {
			// OK
		}

		try {
			PathTemplate.render("/a/{b}//", Map.of("b", "123"));
			fail();
		} catch (IllegalArgumentException e) {
			// OK
		}

		try {
			PathTemplate.render("/a/{b?uc?lc}", Map.of("b", "123"));
			fail();
		} catch (IllegalArgumentException e) {
			// OK
		}
	}

}
