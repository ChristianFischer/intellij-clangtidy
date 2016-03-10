/**
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
package clangtidy.yaml;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test for a clang-tidy result on unix
 */
public class YamlResultsReaderUnixTest extends AbstractTidyResultsYamlReaderTest
{
	public YamlResultsReaderUnixTest() {
		super(new File("tests/resources/yaml/results_unix.yaml"));
	}


	@Override
	protected void testReplacementsList(List replacements) throws Exception {
		assertEquals(3, replacements.size());

		testReplacement0(replacements.get(0));
		testReplacement1(replacements.get(1));
		testReplacement2(replacements.get(2));
	}


	private void testReplacement0(Object object) throws Exception {
		assertTrue(object instanceof Map);

		@SuppressWarnings("unchecked")
		Map<String,Object> replacement = (Map<String,Object>)object;
		assertEquals(4, replacement.size());

		// FilePath
		assertTrue(replacement.containsKey("FilePath"));
		assertTrue(replacement.get("FilePath") instanceof String);
		assertEquals("/home/sweet/home/my/SourceFile.cpp", replacement.get("FilePath"));

		// Offset
		assertTrue(replacement.containsKey("Offset"));
		assertTrue(replacement.get("Offset") instanceof Double);
		assertEquals(284.0, replacement.get("Offset"));

		// Length
		assertTrue(replacement.containsKey("Length"));
		assertTrue(replacement.get("Length") instanceof Double);
		assertEquals(19.0, replacement.get("Length"));

		// ReplacementText
		assertTrue(replacement.containsKey("ReplacementText"));
		assertTrue(replacement.get("ReplacementText") instanceof String);
		assertEquals("(int i : arr)", replacement.get("ReplacementText"));
	}


	private void testReplacement1(Object object) throws Exception {
		assertTrue(object instanceof Map);

		@SuppressWarnings("unchecked")
		Map<String,Object> replacement = (Map<String,Object>)object;
		assertEquals(4, replacement.size());

		// FilePath
		assertTrue(replacement.containsKey("FilePath"));
		assertTrue(replacement.get("FilePath") instanceof String);
		assertEquals("/home/sweet/home/my/SourceFile.cpp", replacement.get("FilePath"));

		// Offset
		assertTrue(replacement.containsKey("Offset"));
		assertTrue(replacement.get("Offset") instanceof Double);
		assertEquals(328.0, replacement.get("Offset"));

		// Length
		assertTrue(replacement.containsKey("Length"));
		assertTrue(replacement.get("Length") instanceof Double);
		assertEquals(6.0, replacement.get("Length"));

		// ReplacementText
		assertTrue(replacement.containsKey("ReplacementText"));
		assertTrue(replacement.get("ReplacementText") instanceof String);
		assertEquals("i", replacement.get("ReplacementText"));
	}


	private void testReplacement2(Object object) throws Exception {
		assertTrue(object instanceof Map);

		@SuppressWarnings("unchecked")
		Map<String,Object> replacement = (Map<String,Object>)object;
		assertEquals(4, replacement.size());

		// FilePath
		assertTrue(replacement.containsKey("FilePath"));
		assertTrue(replacement.get("FilePath") instanceof String);
		assertEquals("/home/123/0.h", replacement.get("FilePath"));

		// Offset
		assertTrue(replacement.containsKey("Offset"));
		assertTrue(replacement.get("Offset") instanceof Double);
		assertEquals(456.0, replacement.get("Offset"));

		// Length
		assertTrue(replacement.containsKey("Length"));
		assertTrue(replacement.get("Length") instanceof Double);
		assertEquals(4.0, replacement.get("Length"));

		// ReplacementText
		assertTrue(replacement.containsKey("ReplacementText"));
		assertTrue(replacement.get("ReplacementText") instanceof String);
		assertEquals("nullptr", replacement.get("ReplacementText"));
	}
}