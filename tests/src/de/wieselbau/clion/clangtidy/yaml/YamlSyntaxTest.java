/*
 * Copyright (C) 2016
 * Christian Fischer
 *
 * https://bitbucket.org/baldur/clion-clangtidy/
 *
 * This plugin is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General
 * Public License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301 USA
 */

package de.wieselbau.clion.clangtidy.yaml;

import de.wieselbau.util.yaml.YamlReader;
import org.junit.Assert;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test basic yaml syntax and parsing functionality.
 */
public class YamlSyntaxTest extends AbstractTidyResultsYamlReaderTest {

	public YamlSyntaxTest() {
		super(new File("tests/resources/yaml/syntax_test.yaml"));
	}


	@Override
	protected void testYamlResults(YamlReader yaml) throws Exception {
		assertNotNull(yaml);
		assertNotNull(yaml.getRootObject());

		// we expect the root object to be a map
		assertTrue(yaml.getRootObject() instanceof Map);

		@SuppressWarnings("unchecked")
		Map<String,Object> map = (Map<String,Object>)yaml.getRootObject();

		assertTrue(map.containsKey("PlainText"));
		assertEquals("JustSomeText",		map.get("PlainText"));

		assertTrue(map.containsKey("QuotedString1"));
		assertEquals("Some other text 1",	map.get("QuotedString1"));

		assertTrue(map.containsKey("QuotedString2"));
		assertEquals("Some other text 2",	map.get("QuotedString2"));

		assertTrue(map.containsKey("EscapedCharacters"));
		assertEquals("contains escaped characters: \n \" \\ \'",					map.get("EscapedCharacters"));

		assertTrue(map.containsKey("NotEscapedCharacters"));
		assertEquals("does not contain escaped characters: \" \\n \\\" \\\\ \\",	map.get("NotEscapedCharacters"));
	}


	@Override
	protected void testReplacementsList(List replacements) throws Exception {
		Assert.fail(); // not used here
	}
}
