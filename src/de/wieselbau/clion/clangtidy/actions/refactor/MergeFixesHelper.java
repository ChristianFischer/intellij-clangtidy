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

import com.intellij.diff.*;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.DiffRequestProducer;
import com.intellij.diff.chains.DiffRequestProducerException;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.merge.MergeRequest;
import com.intellij.diff.merge.MergeResult;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.diff.util.DiffUserDataKeys;
import com.intellij.diff.util.Side;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.LineSeparator;
import com.intellij.util.containers.ContainerUtil;
import de.wieselbau.clion.clangtidy.tidy.FixFileEntry;
import de.wieselbau.clion.clangtidy.tidy.FixProjectHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class which presents a merge dialog to the user,
 * so he may select single changes from a clang-tidy result.
 */
public class MergeFixesHelper {

	public static boolean merge(@NotNull FixProjectHelper helper, @NotNull FixFileEntry entry) {
		DiffRequest request = createDiffRequest(helper, entry);
		if (request != null) {
			DiffManager.getInstance().showDiff(helper.getProject(), request);

			return true;
		}

		return false;
	}


	public static boolean merge(@NotNull FixProjectHelper helper, @NotNull List<FixFileEntry> entries) {
		DiffRequestChain requests = createDiffRequestChain(helper, entries);
		if (requests != null) {
			DiffManager.getInstance().showDiff(helper.getProject(), requests, DiffDialogHints.MODAL);

			return true;
		}

		return false;
	}


	public static DiffRequest createDiffRequest(@NotNull FixProjectHelper helper, @NotNull FixFileEntry entry) {
		VirtualFile file     = entry.getFile();
		Document    document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () ->
				FileDocumentManager.getInstance().getDocument(file)
		);

		if (document != null) {
			DocumentContent documentContent = DiffContentFactory.getInstance().create(helper.getProject(), document);
			DocumentContent patchedContent  = new PatchedDocumentContent(entry);

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
			Document document = ApplicationManager.getApplication().runReadAction((Computable<Document>) () ->
					FileDocumentManager.getInstance().getDocument(entry.getFile())
			);

			if (document != null) {
				String patchedContentString  = entry.createPatchedContent(document.getImmutableCharSequence());
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


	public static DiffRequestChain createDiffRequestChain(@NotNull FixProjectHelper helper, @NotNull List<FixFileEntry> entries) {
		return new MyDiffRequestChain(helper, entries);
	}


	private static class MyDiffRequestChain extends UserDataHolderBase implements DiffRequestChain {
		private final @NotNull FixProjectHelper				helper;
		private final @NotNull List<DiffRequestProducer>	producers;
		private int index = 0;


		public MyDiffRequestChain(@NotNull FixProjectHelper helper, @NotNull List<FixFileEntry> entries) {
			this.helper		= helper;
			this.producers	= ContainerUtil.map(
					entries,
					entry -> new MyDiffRequestProducer(helper, entry)
			);
		}


		@NotNull
		@Override
		public List<? extends DiffRequestProducer> getRequests() {
			return producers;
		}

		@Override
		public int getIndex() {
			return index;
		}

		@Override
		public void setIndex(int index) {
			assert index>=0 && index<producers.size();
			this.index = index;
		}
	}


	private static class MyDiffRequestProducer implements DiffRequestProducer {
		private final @NotNull FixProjectHelper		helper;
		private final @NotNull FixFileEntry			entry;


		public MyDiffRequestProducer(@NotNull FixProjectHelper helper, @NotNull FixFileEntry entry) {
			this.helper = helper;
			this.entry  = entry;
		}


		@NotNull
		@Override
		public String getName() {
			return entry.getFile().getName();
		}

		@NotNull
		@Override
		public DiffRequest process(@NotNull UserDataHolder context, @NotNull ProgressIndicator indicator)
				throws DiffRequestProducerException, ProcessCanceledException {
			DiffRequest request = createDiffRequest(helper, entry);
			if (request == null) {
				throw new DiffRequestProducerException("Cannot load diff for file '" + entry.getFile() + "'");
			}

			return request;
		}
	}


	private static class PatchedDocumentContent extends UserDataHolderBase implements DocumentContent {
		private @NotNull	FixFileEntry		entry;
		private @Nullable	Document			patchedDocument;
		private @Nullable	LineSeparator		lineSeparator;


		public PatchedDocumentContent(@NotNull FixFileEntry entry) {
			this.entry = entry;
		}


		@NotNull
		@Override
		public Document getDocument() {
			if (patchedDocument == null) {
				AccessToken lock = ApplicationManager.getApplication().acquireReadActionLock();
				try {
					Document originalDocument = ApplicationManager.getApplication().runReadAction((Computable<Document>) () ->
							FileDocumentManager.getInstance().getDocument(entry.getFile())
					);

					if (originalDocument == null) {
						throw new NullPointerException("Document could not be loaded");
					}

					// get the document content as immutable char sequence
					CharSequence originalDocumentContent = originalDocument.getImmutableCharSequence();

					// try to detect the line separator from the original file
					if (lineSeparator == null) {
						String lsString = LoadTextUtil.detectLineSeparator(entry.getFile(), true);

						if (lsString != null) {
							lineSeparator = LineSeparator.fromString(lsString);
						}
					}

					// could not detect line separator by file?
					if (lineSeparator == null) {
						lineSeparator = StringUtil.detectSeparators(originalDocumentContent);
					}

					String patchedText = entry.createPatchedContent(originalDocumentContent);
				//	patchedText = StringUtil.convertLineSeparators(patchedText, lineSeparator.getSeparatorString());

					patchedDocument = EditorFactory.getInstance().createDocument(
							StringUtil.convertLineSeparators(patchedText)
					);

					// result needs to be readonly
					patchedDocument.setReadOnly(true);
				}
				finally {
					lock.finish();
				}
			}

			return patchedDocument;
		}

		@Nullable
		@Override
		public VirtualFile getHighlightFile() {
			return entry.getFile();
		}

		@Nullable
		@Override
		public OpenFileDescriptor getOpenFileDescriptor(int offset) {
			return null;
		}

		@Nullable
		@Override
		public LineSeparator getLineSeparator() {
			getDocument(); // ensure document was opened
			return lineSeparator;
		}

		@Nullable
		@Override
		public Charset getCharset() {
			return entry.getFile().getCharset();
		}

		@Nullable
		@Override
		public FileType getContentType() {
			return entry.getFile().getFileType();
		}

		@Nullable
		@Override
		public OpenFileDescriptor getOpenFileDescriptor() {
			return null;
		}

		@Override
		public void onAssigned(boolean isAssigned) {
		}
	}
}
