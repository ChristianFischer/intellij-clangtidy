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

package de.wieselbau.clion.clangtidy.tidy;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * This utility class stores all fixes, which belongs to a specific file
 * and provides functionality to apply these fixes to the file's document
 * or create a patched version of the file's content.
 */
public class FixFileEntry {
	public enum Scope {
		Selection,
		Project,
		External,
	}

	public enum Result {
		Successful,
		Failed,
	}

	private VirtualFile			file;
	private List<Issue>			issues;
	private List<Fix.Change>	changes;
	private Scope				scope;
	private Result				result;
	private boolean				selected;
	private boolean				prepared;


	public FixFileEntry(@NotNull VirtualFile file) {
		this.file		= file;
		this.issues		= new ArrayList<>();
		this.changes	= new ArrayList<>();
		this.selected	= false;
		this.prepared	= false;
	}



	public VirtualFile getFile() {
		return file;
	}

	public void addIssue(@NotNull Issue issue) {
		issues.add(issue);
	}

	public List<Issue> getIssues() {
		return Collections.unmodifiableList(issues);
	}

	public void addChange(@NotNull Fix.Change change) {
		changes.add(change);
	}

	public List<Fix.Change> getChanges() {
		return Collections.unmodifiableList(changes);
	}

	public void setScope(@NotNull Scope scope) {
		this.scope = scope;
	}

	public Scope getScope() {
		return scope;
	}

	public void setResult(@NotNull Result result) {
		this.result = result;
	}

	public Result getResult() {
		return result;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isSelected() {
		return selected;
	}




	private static boolean areSequencesEqual(CharSequence s1, CharSequence s2) {
		int length1 = s1.length();
		int length2 = s2.length();

		if (length1 != length2) {
			return false;
		}

		for(int i=0; i<length1; i++) {
			if (s1.charAt(i) != s2.charAt(i)) {
				return false;
			}
		}

		return true;
	}


	public void patch(@NotNull CharSequence content, @NotNull BiConsumer<TextRange,String> consumer) {
		prepare();

		int offset = 0;

		for(Fix.Change change : changes) {
			int startOffset          = offset + change.getTextRange().getStartOffset();
			int endOffsetOriginal    = offset + change.getTextRange().getEndOffset();
			int endOffsetReplacement = offset + change.getTextRange().getStartOffset() + change.getReplacement().length();

			if ((startOffset >= content.length()) || (endOffsetOriginal >= content.length())) {
				throw new IndexOutOfBoundsException("Cannot apply fix " + change.toString());
			}

			boolean hasOriginalContent = (
					(endOffsetOriginal < content.length())
				&&	(areSequencesEqual(CharBuffer.wrap(content, startOffset, endOffsetOriginal), change.getOriginal()))
			);

			boolean hasReplacementContent = (
					(endOffsetReplacement < content.length())
				&&	(areSequencesEqual(CharBuffer.wrap(content, startOffset, endOffsetReplacement), change.getReplacement()))
			);

			boolean canBeApplied = false;

			if (startOffset < endOffsetOriginal && hasOriginalContent) {
				canBeApplied = true;
			}

			if (startOffset < endOffsetReplacement && !hasReplacementContent) {
				canBeApplied = true;
			}

			if (canBeApplied) {
				consumer.accept(
						TextRange.create(startOffset, endOffsetOriginal),
						change.getReplacement()
				);

				// check if change is within the content char sequence
				assert areSequencesEqual(
						CharBuffer.wrap(content, startOffset, endOffsetReplacement),
						change.getReplacement()
				);
			}

			// offsets will have changed after applying other changes
			offset -= change.getTextRange().getLength();
			offset += change.getReplacement().length();
		}
	}


	public String createPatchedContent(@NotNull CharSequence original) {
		prepare();

		final StringBuilder content = new StringBuilder(original);

		patch(
				content,
				(TextRange range, String replacement) -> {
					content.replace(
							range.getStartOffset(),
							range.getEndOffset(),
							replacement
					);
				}
		);

		return content.toString();
	}


	/**
	 * Applies this entries fixes to it's file.
	 * When everything was Ok, the function will return {@link Result#Successful}. The new result will be stored
	 * within the entry.
	 * If the entry already has a result assigned, it will be skipped and apply will return {@code null}.
	 * @param project    The current project.
	 * @return {@link Result#Successful} when applied all changes successfully.
	 *         {@code null} if this entry was skipped.
	 */
	public @Nullable Result apply(@NotNull Project project) {
		if (getResult() != null) {
			return null;
		}

		prepare();

		WriteCommandAction.runWriteCommandAction(
				project,
				"clang-tidy",
				null,
				() -> {
					Document document = FileDocumentManager.getInstance().getDocument(file);
					if (document == null) {
						return;
					}

					patch(
							document.getCharsSequence(),
							(TextRange range, String replacement) -> {
								document.replaceString(
										range.getStartOffset(),
										range.getEndOffset(),
										replacement
								);
							}
					);
				}
		);

		setResult(Result.Successful);

		return Result.Successful;
	}



	private void prepare() {
		if (prepared) {
			return;
		}

		// ensure, all fixes are sorted in ascending order
		changes.sort(Comparator.comparingInt(change -> change.getTextRange().getStartOffset()));

		// since Intellij uses only \n for linebreaks, but clang-tidy is using the file's native
		// linebreak style for it's offsets, we have to convert them into \n linebreak offsets
		try(InputStream in = file.getInputStream()) {
			List<Integer> ignorableLineFeeds = new ArrayList<>();
			List<Integer> lineOffsets        = new ArrayList<>();
			StringBuilder content            = new StringBuilder();

			{
				int currentOffset = 0;
				int lastByte      = -1;
				int b;

				lineOffsets.add(0);

				while((b = in.read()) != -1) {
					content.append((char)b);
					++currentOffset;

					if (b == '\n') {
						lineOffsets.add(currentOffset);

						// after a combination of \r and \n, the \r has to be ignored
						if (lastByte == '\r') {
							ignorableLineFeeds.add(currentOffset);
						}
					}

					lastByte = b;
				}
			}

			for(Fix.Change change : changes) {
				final int startOffset = change.getTextRange().getStartOffset();
				final int endOffset   = change.getTextRange().getEndOffset();

				long startOffsetCorrection = ignorableLineFeeds.stream().filter((Integer i) -> (i <= startOffset)).count();
				long endOffsetCorrection   = ignorableLineFeeds.stream().filter((Integer i) -> (i <= endOffset)).count();

				change.setTextRange(TextRange.create(
						(int)(startOffset - startOffsetCorrection),
						(int)(endOffset   - endOffsetCorrection)
				));

				change.setOriginal(content.substring(startOffset, endOffset).replace("\r\n", "\n").replace('\r', '\n'));
				assert(change.getOriginal().length() == change.getTextRange().getLength());

				/*
				// try to assign issues to each fix
				for(Issue issue : issues) {
					if (lineOffsets.size() >= issue.getLineNumber()) {
						int lineOffset = lineOffsets.get(issue.getLineNumber() - 1);
						int offset     = lineOffset + issue.getLineColumn() - 1;

						if (change.getTextRange().getStartOffset() == offset) {
							change.setIssue(issue);
						}
					}
				}
				*/
			}

			prepared = true;
		}
		catch(IOException e) {
			Logger.getInstance(this.getClass()).error(e);
		}
	}
}
