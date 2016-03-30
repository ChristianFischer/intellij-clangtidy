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

package clangtidy.yaml;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test a completely empty yaml structure
 */
public class YamlEmptyFileTest extends AbstractTidyResultsYamlReaderTest
{
	public YamlEmptyFileTest() {
		super(new File("tests/resources/yaml/empty.yaml"));
	}


	@Override
	protected void testYamlResults(YamlReader yaml) throws Exception {
		assertNotNull(yaml);
		assertNull(yaml.getRootObject());
	}


	@Override
	protected void testReplacementsList(List replacements) throws Exception {
		fail("unexpected");
	}
}