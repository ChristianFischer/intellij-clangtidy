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

package de.wieselbau.clion.clangtidy.actions.refactor;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.DiffRequestFactory;
import com.intellij.diff.InvalidDiffRequestException;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.merge.MergeRequest;
import com.intellij.diff.merge.MergeResult;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.Side;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import de.wieselbau.clion.clangtidy.tidy.FixFileEntry;
import de.wieselbau.clion.clangtidy.tidy.FixProjectHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A helper class which presents a merge dialog to the user,
 * so he may select single changes from a clang-tidy result.
 */
public class MergeFixesHelper {

	public static boolean merge(@NotNull FixProjectHelper helper, @NotNull FixFileEntry entry, @Nullable Consumer<MergeResult> onResult) {
		DiffRequest request = createDiffRequest(helper, entry);
		if (request != null) {
			DiffManager.getInstance().showDiff(helper.getProject(), request);

			return true;
		}

		return false;
	}


	public static DiffRequest createDiffRequest(@NotNull FixProjectHelper helper, @NotNull FixFileEntry entry) {
		VirtualFile file     = entry.getFile();
		FileType    fileType = file.getFileType();
		Document    document = FileDocumentManager.getInstance().getDocument(file);

		if (document != null) {
			String patchedContentString  = entry.createPatchedContent();
			DocumentContent documentContent = DiffContentFactory.getInstance().create(helper.getProject(), document);
			DocumentContent patchedContent  = DiffContentFactory.getInstance().create(patchedContentString, fileType);

			SimpleDiffRequest request = new SimpleDiffRequest(
					DiffRequestFactory.getInstance().getTitle(file),
					documentContent,
					patchedContent,
					"Current file",
					"Result of clang-tidy"
			);

			request.putUserData(DiffUserDataKeys.MASTER_SIDE,          Side.LEFT);
			request.putUserData(DiffUserDataKeys.PREFERRED_FOCUS_SIDE, Side.LEFT);

			return request;
		}

		return null;
	}


	public static boolean mergeThreeSide(@NotNull FixProjectHelper helper, @NotNull FixFileEntry entry, @Nullable Consumer<MergeResult> onResult) {
		try {
			Document document = FileDocumentManager.getInstance().getDocument(entry.getFile());

			if (document != null) {
				String patchedContentString  = entry.createPatchedContent();
				String originalContentString = document.getText();
				List<String> content = new ArrayList<>();
				content.add(originalContentString);
				content.add(originalContentString);
				content.add(patchedContentString);

				String title = DiffRequestFactory.getInstance().getTitle(entry.getFile());
				List<String> titles = new ArrayList<>();
				titles.add(title);
				titles.add("Merge Result");
				titles.add("Result of clang-tidy");

				FileType fileType = entry.getFile().getFileType();

				MergeRequest request = DiffRequestFactory.getInstance().createMergeRequest(
						helper.getProject(),
						fileType,
						document,
						content,
						title,
						titles,
						onResult
				);

				DiffManager.getInstance().showMerge(helper.getProject(), request);

				return true;
			}
		}
		catch (InvalidDiffRequestException e) {
			Logger.getInstance(MergeFixesHelper.class).error(e);
		}

		return false;
	}

}
