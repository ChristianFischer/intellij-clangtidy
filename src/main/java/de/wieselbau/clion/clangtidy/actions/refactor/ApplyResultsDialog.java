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

import com.intellij.ide.presentation.VirtualFilePresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.CheckboxTree;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.tree.TreeUtil;
import de.wieselbau.clion.clangtidy.NotificationFactory;
import de.wieselbau.clion.clangtidy.tidy.ApplyFixesBackgroundTask;
import de.wieselbau.clion.clangtidy.tidy.FixFileEntry;
import de.wieselbau.clion.clangtidy.tidy.FixProjectHelper;
import de.wieselbau.util.filestree.FileNode;
import de.wieselbau.util.filestree.FilesTreeCellRenderer;
import de.wieselbau.util.filestree.FilesTreeModel;
import de.wieselbau.util.filestree.ModuleNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This dialog presents a list of all fixable files found by clang-tidy.
 * The user may select files which should be applied or apply single changes manually.
 */
public class ApplyResultsDialog extends DialogWrapper {
	private JPanel root;
	private JButton btMergeSelected;
	private JButton btApplySelected;
	private Tree listMergeableFiles;

	private Project				project;
	private FixProjectHelper	helper;

	private FilesTreeModel		listMergeableFilesModel;
	private TreeSpeedSearch		listMergeableFilesModelSpeedSearch;


	private static class FixFileEntryNode extends FileNode {
		private FixFileEntry entry;

		public FixFileEntryNode(@NotNull FixFileEntry entry) {
			super(entry.getFile());
			this.entry = entry;
		}

		public FixFileEntry getEntry() {
			return entry;
		}

		@Override
		public void setChecked(boolean checked) {
			entry.setSelected(checked);
		}

		@Override
		public boolean isChecked() {
			return entry.isSelected();
		}

		@Override
		public void customizeRenderer(ColoredTreeCellRenderer renderer, boolean selected, boolean expanded, boolean hasFocus) {
			FixFileEntry.Result result = getEntry().getResult();

			if (result != null) {
				VirtualFile file = getEntry().getFile();
				renderer.setIcon(VirtualFilePresentation.getIcon(file));

				switch (result) {
					case Successful: {
						renderer.append(file.getName(), SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
						renderer.append(" (done)", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
						break;
					}

					case Failed: {
						renderer.append(file.getName(), SimpleTextAttributes.ERROR_ATTRIBUTES);
						break;
					}
				}
			}
			else {
				super.customizeRenderer(renderer, selected, expanded, hasFocus);

				renderer.append(
						" (" + String.valueOf(getEntry().getChanges().size()) + " Changes)",
						SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES
				);
			}
		}
	}


	public ApplyResultsDialog(@NotNull Project project, @NotNull FixProjectHelper helper) {
		super(helper.getProject());

		this.project = project;
		this.helper  = helper;

		init();
		initContent();

		setTitle("Clang-Tidy Results");

		btMergeSelected.addActionListener(e -> onButtonMergeSelected());
		btApplySelected.addActionListener(e -> onButtonApplySelected());
		onListSelectionChanged(null);
	}


	private void createUIComponents() {
		listMergeableFiles = new CheckboxTree();
		listMergeableFiles.addTreeSelectionListener(this::onListSelectionChanged);
		listMergeableFiles.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

		listMergeableFilesModelSpeedSearch = new TreeSpeedSearch(
				listMergeableFiles,
				FilesTreeModel::NodeToString
		);

		TreeUtil.installActions(listMergeableFiles);
	}


	private void initContent() {
		listMergeableFilesModel = new FilesTreeModel();

		// add project files
		{
			List<FixFileEntry> projectEntries = helper
					.getFixes().stream()
					.filter(entry -> entry.getScope() != FixFileEntry.Scope.External)
					.collect(Collectors.toList())
			;

			if (!projectEntries.isEmpty()) {
				ModuleNode module = listMergeableFilesModel.createModule(PlatformIcons.PROJECT_ICON, project.getName());

				listMergeableFilesModel.addFiles(
						projectEntries,
						FixFileEntryNode::new,
						module
				);

				module.sortChildrenRecursive();
			}
		}

		// add external files
		{
			List<FixFileEntry> externalEntries = helper
					.getFixes().stream()
					.filter(entry -> entry.getScope() == FixFileEntry.Scope.External)
					.collect(Collectors.toList())
			;

			if (!externalEntries.isEmpty()) {
				ModuleNode module = listMergeableFilesModel.createModule(PlatformIcons.LIBRARY_ICON, "External files");

				listMergeableFilesModel.addFiles(
						externalEntries,
						FixFileEntryNode::new,
						module
				);

				module.sortChildrenRecursive();
			}
		}

		listMergeableFilesModel.flatten();

		listMergeableFiles.setModel(listMergeableFilesModel);
		listMergeableFiles.setRootVisible(false);

		listMergeableFiles.setCellRenderer(new CheckboxTree.CheckboxTreeCellRenderer(true, true) {
				@Override
				public void customizeRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
					super.customizeRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
					FilesTreeCellRenderer.defaultCustomizeFilesTreeCellRenderer(tree, getTextRenderer(), value, selected, expanded, leaf, row, hasFocus);
					SpeedSearchUtil.applySpeedSearchHighlighting(tree, getTextRenderer(), true, selected);
				}
		});

		// expand all nodes of the tree
		for(int i=0; i<listMergeableFiles.getRowCount(); i++) {
			listMergeableFiles.expandRow(i);
		}
	}


	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return root;
	}


	@Override
	protected void doOKAction() {
		super.doOKAction();

		if (helper.countFilesToBeApplied() > 0) {
			ApplyFixesBackgroundTask.start(project, helper.getFixesSelected());
		}
	}


	protected void onButtonMergeSelected() {
		List<FixFileEntry> entries = getSelectedEntries();
		MergeFixesHelper.merge(helper, entries);
	}


	protected void onButtonApplySelected() {
		List<FixFileEntry> entries = getSelectedEntries();

		/*
		if (entries.size() == 1) {
			FixFileEntry entry = entries.get(0);
			FixFileEntry.Result result = entry.apply(project);
			onEntryResultChanged(entry, result);
		}
		else {
		*/
			ApplyFixesBackgroundTask.start(
					project,
					entries,
					(@NotNull FixFileEntry entry, FixFileEntry.Result result) -> {
						EventQueue.invokeLater(() -> {
							onEntryResultChanged(entry, result);
						});
					}
			);
		/*
		}
		*/
	}


	private void onEntryResultChanged(@NotNull FixFileEntry entry, @Nullable FixFileEntry.Result result) {
		if (result != null) {
			entry.setSelected(false);

			// notify the tree about changed nodes
			{
				TreeNode node = listMergeableFilesModel.findNode(
						FixFileEntryNode.class,
						(FixFileEntryNode n) -> entry == n.getEntry()
				);

				while (node != null) {
					listMergeableFilesModel.nodeChanged(node);
					node = node.getParent();
				}
			}

			switch (result) {
				case Failed: {
					NotificationFactory.notifyScanFailedOnFile(helper.getProject(), entry.getFile());
					break;
				}
			}
		}
	}


	protected void onListSelectionChanged(TreeSelectionEvent e) {
		boolean hasSelection = listMergeableFiles.getSelectionCount() != 0;
		btMergeSelected.setEnabled(hasSelection);
		btApplySelected.setEnabled(hasSelection);
	}


	/**
	 * Get a list of all selected entries.
	 * If a module or directory node is selected, all files under this node will be included.
	 */
	private @NotNull List<FixFileEntry> getSelectedEntries() {
		List<FixFileEntry> files = new ArrayList<>();

		for(TreeNode node : listMergeableFiles.getSelectedNodes(TreeNode.class, null)) {
			collectFilesFromNode(node, files);
		}

		return files;
	}


	private void collectFilesFromNode(final @NotNull TreeNode node, @NotNull List<FixFileEntry> list) {
		if (node instanceof FixFileEntryNode) {
			FixFileEntry entry = ((FixFileEntryNode)node).getEntry();

			if (!list.contains(entry)) {
				list.add(entry);
			}
		}

		for(int i=node.getChildCount(); --i>=0;) {
			collectFilesFromNode(node.getChildAt(i), list);
		}
	}
}
