/*
 * Copyright (C) 2017
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

package de.wieselbau.clion.clangtidy.results;

import de.wieselbau.clion.clangtidy.tidy.Fix;
import de.wieselbau.clion.clangtidy.tidy.ScannerResult;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;

import static de.wieselbau.clion.clangtidy.TestUtils.getTestFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * Test a completely empty result set
 */
public class ResultsEmptyTest extends AbstractTidyResultsTest {

	@Test
	public void parseResults() {
		ScannerResult result = parseResultsFromFile(getTestFile("yaml/empty_results.yaml"));
		assertNotNull(result);

		List<Fix> fixes = result.getFixes();
		assertNotNull(fixes);

		testFixesList(fixes);
	}


	private void testFixesList(@NotNull List<Fix> fixes) {
		assertEquals(0, fixes.size());
	}
}
