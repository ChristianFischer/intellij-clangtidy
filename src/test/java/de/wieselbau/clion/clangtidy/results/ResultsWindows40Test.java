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
 * Test for a clang-tidy 4.0 result on windows
 */
public class ResultsWindows40Test extends AbstractTidyResultsTest {

	@Test
	public void parseResults() {
		ScannerResult result = parseResultsFromFile(getTestFile("yaml/results_windows_4.0.yaml"));
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
		assertEquals("modernize-use-equals-default", fix.getDiagnosticName());
		assertEquals(1, fix.getChanges().size());
		testFix1Change1(fix.getChanges().get(0));
	}

	private void testFix1Change1(@NotNull Fix.Change change) {
		assertNotNull(change.getFile());
		assertEquals("D:\\Path\\to\\my\\SourceFile.cpp", asWindowsPath(change.getFile()));

		assertNotNull(change.getTextRange());
		assertEquals(31, change.getTextRange().getStartOffset());
		assertEquals(8, change.getTextRange().getLength());

		assertEquals("= default;", change.getReplacement());
	}


	private void testFix2(@NotNull Fix fix) {
		assertEquals("modernize-redundant-void-arg", fix.getDiagnosticName());
		assertEquals(1, fix.getChanges().size());
		testFix2Change1(fix.getChanges().get(0));
	}

	private void testFix2Change1(@NotNull Fix.Change change) {
		assertNotNull(change.getFile());
		assertEquals("D:\\Path\\to\\my\\SourceFile.cpp", asWindowsPath(change.getFile()));

		assertNotNull(change.getTextRange());
		assertEquals(57, change.getTextRange().getStartOffset());
		assertEquals(4, change.getTextRange().getLength());

		assertEquals("", change.getReplacement());
	}


	private void testFix3(@NotNull Fix fix) {
		assertEquals("modernize-loop-convert", fix.getDiagnosticName());
		assertEquals(2, fix.getChanges().size());
		testFix3Change1(fix.getChanges().get(0));
		testFix3Change2(fix.getChanges().get(1));
	}

	private void testFix3Change1(@NotNull Fix.Change change) {
		assertNotNull(change.getFile());
		assertEquals("D:\\Path\\to\\my\\SourceFile.cpp", asWindowsPath(change.getFile()));

		assertNotNull(change.getTextRange());
		assertEquals(284, change.getTextRange().getStartOffset());
		assertEquals(19, change.getTextRange().getLength());

		assertEquals("(int i : arr)", change.getReplacement());
	}

	private void testFix3Change2(@NotNull Fix.Change change) {
		assertNotNull(change.getFile());
		assertEquals("D:\\Path\\to\\my\\SourceFile.cpp", asWindowsPath(change.getFile()));

		assertNotNull(change.getTextRange());
		assertEquals(328, change.getTextRange().getStartOffset());
		assertEquals(6, change.getTextRange().getLength());

		assertEquals("i", change.getReplacement());
	}
}
