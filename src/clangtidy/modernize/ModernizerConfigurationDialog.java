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
package clangtidy.modernize;

import clangtidy.Options;
import clangtidy.tidy.ToolCollection;
import clangtidy.tidy.ToolController;
import clangtidy.util.properties.PropertyInstance;
import clangtidy.util.properties.ui.PropertiesTable;
import clangtidy.util.properties.ui.PropertiesTableModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.table.JBTable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI class to provide configuration for clang-tidy based refactoring.
 */
public class ModernizerConfigurationDialog extends DialogWrapper {
	private JPanel root;
	private CheckBoxList<ToolController> listTools;
	private JBTable tToolProperties;
	private JPanel toolsConfigContainer;
	private JTextPane txtCurrentDescription;

	private ToolController			currentTool;
	private PropertiesTableModel	currentToolProperties;

	private Project					project;


	public ModernizerConfigurationDialog(@Nullable Project project) {
		super(project);
		setTitle("Select Checks to Be Applied");

		this.project	= project;

		init();
		initContent();
	}


	private void createUIComponents() {
		listTools = new CheckBoxList<>();
		listTools.addListSelectionListener(this::onToolSelected);

		tToolProperties = new PropertiesTable();
		tToolProperties.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tToolProperties.getSelectionModel().addListSelectionListener(this::onToolPropertySelected);
	}


	@Nullable
	@Override
	protected JComponent createCenterPanel() {
		return root;
	}


	protected void initContent() {
		setCurrentDescriptionText(null);

		listTools.setPaintBusy(true);
		ToolCollection.requestAvailableTools(availableTransforms -> {
			Collections.sort(
					availableTransforms,
					(ToolController a, ToolController b) -> a.getName().compareTo(b.getName())
			);

			// restore defaults on all tools
			for(ToolController tool : availableTransforms) {
				tool.onRestoreDefaults();
			}

			EventQueue.invokeLater(() -> {
				for(ToolController tool : availableTransforms) {
					String name = tool.getDisplayName();
					boolean enabled = Options.isToolEnabled(tool);
					listTools.addItem(tool, name, enabled);
				}

				listTools.setPaintBusy(false);
			});
		});
	}


	protected void onToolSelected(ListSelectionEvent e) {
		int selectedIndex = listTools.getSelectedIndex();
		if (selectedIndex != -1) {
			setSelectedTool(listTools.getItemAt(selectedIndex));
		}
		else {
			setSelectedTool(null);
		}
	}


	protected void onToolPropertySelected(ListSelectionEvent e) {
		if (currentToolProperties != null) {
			int index = tToolProperties.getSelectedRow();

			if (index >= 0 && index < currentToolProperties.getRowCount()) {
				PropertyInstance property = currentToolProperties.getProperty(index);
				setCurrentDescriptionText(property.getDescriptor().getDescription());
			}
			else {
				setCurrentDescriptionText(null);
			}
		}
	}


	protected void setSelectedTool(ToolController tool) {
		if (currentTool != tool) {
			currentTool = tool;

			if (tool != null) {
				currentToolProperties = PropertiesTableModel.create(tool);
				tToolProperties.setModel(currentToolProperties);
				setCurrentDescriptionText(currentTool.getDescription());

				toolsConfigContainer.revalidate();
				toolsConfigContainer.repaint();
			}
		}
	}


	public void setCurrentDescriptionText(String text) {
		if (text != null && !text.isEmpty()) {
			txtCurrentDescription.setText(text);
			txtCurrentDescription.setVisible(true);
		}
		else {
			txtCurrentDescription.setText("");
			txtCurrentDescription.setVisible(false);
		}
	}


	public int countSelectedTools() {
		int count = 0;

		for(int i=listTools.getModel().getSize(); --i>=0;) {
			if (listTools.isItemSelected(i)) {
				++count;
			}
		}

		return count;
	}


	public ToolController[] getSelectedTools() {
		List<ToolController> selectedTools = new ArrayList<>();

		for(int i=0; i<listTools.getModel().getSize(); i++) {
			ToolController tool     = listTools.getItemAt(i);
			boolean      enabled  = listTools.isItemSelected(i);

			if (tool != null) {
				Options.setToolEnabled(tool, enabled);
				tool.OnConfigAccepted();
			}

			if (enabled) {
				selectedTools.add(tool);
			}
		}

		return selectedTools.toArray(new ToolController[selectedTools.size()]);
	}


	@Nullable
	@Override
	protected ValidationInfo doValidate() {
		ValidationInfo info = super.doValidate();
		if (info != null) {
			return info;
		}

		if (countSelectedTools() == 0) {
			return new ValidationInfo("No tools selected");
		}

		return null;
	}


	@Override
	protected void doOKAction() {
		super.doOKAction();
	}
}
