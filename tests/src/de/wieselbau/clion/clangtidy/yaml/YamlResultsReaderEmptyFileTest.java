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

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Test for a clang-tidy empty result
 */
public class YamlResultsReaderEmptyFileTest extends AbstractTidyResultsYamlReaderTest
{
	public YamlResultsReaderEmptyFileTest() {
		super(new File("tests/resources/yaml/empty_results.yaml"));
	}


	@Override
	protected void testYamlResults(YamlReader yaml) throws Exception {
		assertNotNull(yaml);
		assertNotNull(yaml.getRootObject());

		// we expect the root object to be a map
		assertTrue(yaml.getRootObject() instanceof Map);

		@SuppressWarnings("unchecked")
		Map<String,Object> map = (Map<String,Object>)yaml.getRootObject();

		// we expect two elements, MainSourceFile and Replacements
		assertEquals(2, map.size());
		assertTrue(map.containsKey("MainSourceFile"));
		assertTrue(map.containsKey("Replacements"));

		// MainSourceFile is expected to be empty
		assertEquals("", map.get("MainSourceFile"));

		// Replacements is expected to be a list
		assertNull(map.get("Replacements"));
	}


	@Override
	protected void testReplacementsList(List replacements) throws Exception {
		fail("List should be empty.");
	}
}