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

import com.intellij.openapi.util.io.FileUtil;
import de.wieselbau.util.yaml.YamlReader;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Base class to test clang-tidy result files
 */
public abstract class AbstractTidyResultsYamlReaderTest
{
	private File yamlFile;

	protected AbstractTidyResultsYamlReaderTest(File file) {
		yamlFile = file;
	}


	@Test
	public void testFromFile() throws Exception {
		YamlReader yaml = new YamlReader(yamlFile);
		testYamlResults(yaml);
	}


	@Test
	public void testFromStringStream() throws Exception {
		String yamlContent = FileUtil.loadFile(yamlFile);
		testStringContent(yamlContent);
	}


	@Test
	public void testLineEndingsUnix() throws Exception {
		String yamlContent = FileUtil.loadFile(yamlFile);
		testStringContent(toUnix(yamlContent));
	}


	@Test
	public void testLineEndingsWindows() throws Exception {
		String yamlContent = FileUtil.loadFile(yamlFile);
		testStringContent(toWindows(yamlContent));
	}


	@Test
	public void testLineEndingsMacOs() throws Exception {
		String yamlContent = FileUtil.loadFile(yamlFile);
		testStringContent(toMacOs(yamlContent));
	}


	static String toUnix(String string) {
		return string.replace("\r\n", "\n").replace("\r", "\n");
	}

	static String toWindows(String string) {
		return toUnix(string).replace("\n", "\r\n");
	}

	static String toMacOs(String string) {
		return toUnix(string).replace("\n", "\r");
	}


	private void testStringContent(String content) throws Exception {
		assertNotNull(content);
		testStream(new ByteArrayInputStream(content.getBytes()));
	}


	private void testStream(InputStream in) throws Exception {
		YamlReader yaml = new YamlReader(in);
		testYamlResults(yaml);
	}


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
		assertTrue(map.get("Replacements") instanceof List);

		List replacements = (List)map.get("Replacements");
		assertNotNull(replacements);

		testReplacementsList(replacements);
	}


	protected abstract void testReplacementsList(List replacements) throws Exception;
}