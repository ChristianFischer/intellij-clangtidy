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

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;


/**
 * Test for a clang-tidy result on unix
 */
public class ResultsUnixTest extends AbstractTidyResultsTest {

	@Test
	public void parseResults() {
		ScannerResult result = parseResultsFromFile(new File("tests/resources/yaml/results_unix.yaml"));
		assertNotNull(result);

		List<Fix> fixes = result.getFixes();
		assertNotNull(fixes);

		testFixesList(fixes);
	}


	private void testFixesList(@NotNull List<Fix> fixes) {
		assertEquals(3, fixes.size());

		testFix1(fixes.get(0));
		testFix2(fixes.get(1));
		testFix3(fixes.get(2));
	}


	private void testFix1(@NotNull Fix fix) {
		assertNull(fix.getDiagnosticName());
		assertEquals(1, fix.getChanges().size());
		testFix1Change1(fix.getChanges().get(0));
	}

	private void testFix1Change1(@NotNull Fix.Change change) {
		assertNotNull(change.getFile());
		assertEquals("/home/sweet/home/my/SourceFile.cpp", asUnixPath(change.getFile()));

		assertNotNull(change.getTextRange());
		assertEquals(284, change.getTextRange().getStartOffset());
		assertEquals(19, change.getTextRange().getLength());

		assertEquals("(int i : arr)", change.getReplacement());
	}


	private void testFix2(@NotNull Fix fix) {
		assertNull(fix.getDiagnosticName());
		assertEquals(1, fix.getChanges().size());
		testFix2Change1(fix.getChanges().get(0));
	}

	private void testFix2Change1(@NotNull Fix.Change change) {
		assertNotNull(change.getFile());
		assertEquals("/home/sweet/home/my/SourceFile.cpp", asUnixPath(change.getFile()));

		assertNotNull(change.getTextRange());
		assertEquals(328, change.getTextRange().getStartOffset());
		assertEquals(6, change.getTextRange().getLength());

		assertEquals("i", change.getReplacement());
	}


	private void testFix3(@NotNull Fix fix) {
		assertNull(fix.getDiagnosticName());
		assertEquals(1, fix.getChanges().size());
		testFix3Change1(fix.getChanges().get(0));
	}

	private void testFix3Change1(@NotNull Fix.Change change) {
		assertNotNull(change.getFile());
		assertEquals("/home/123/0.h", asUnixPath(change.getFile()));

		assertNotNull(change.getTextRange());
		assertEquals(456, change.getTextRange().getStartOffset());
		assertEquals(4, change.getTextRange().getLength());

		assertEquals("nullptr", change.getReplacement());
	}
}
