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

package clangtidy.tidy.tools;

import clangtidy.Options;
import clangtidy.tidy.ToolController;
import com.intellij.openapi.ui.ComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides support for the loop-convert modernization.
 */
public class ModernizeLoopConvert implements ToolController {
	public final static String POPERTY_NAME_MINCONFIDENCE		= "MinConfidence";

	public enum MinConfidenceType {
		safe,
		reasonable,
		risky,
	}

	private ComboBox cmbMinConfidence;
	private JPanel root;


	private MinConfidenceType minConfidenceType;


	public ModernizeLoopConvert() {
		minConfidenceType = Options.getToolProperty(this, POPERTY_NAME_MINCONFIDENCE, MinConfidenceType.reasonable);
		initValues();
	}


	private void createUIComponents() {
		cmbMinConfidence = new ComboBox();
	}


	private void initValues() {
		for(MinConfidenceType type : MinConfidenceType.values()) {
			cmbMinConfidence.addItem(type);
		}

		cmbMinConfidence.setSelectedItem(minConfidenceType);
		cmbMinConfidence.addItemListener(this::onMinConfidenceSelected);
	}


	private void onMinConfidenceSelected(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.SELECTED) {
			if (e.getItem() instanceof MinConfidenceType) {
				minConfidenceType = (MinConfidenceType)e.getItem();
			}
		}
	}



	@NotNull
	@Override
	public String getName() {
		return "modernize-loop-convert";
	}

	@Nullable
	@Override
	public JComponent getConfigPanel() {
		return root;
	}

	@Override
	public void OnConfigAccepted() {
		Options.setToolProperty(this, POPERTY_NAME_MINCONFIDENCE, minConfidenceType);
	}

	@Nullable
	@Override
	public Map<String, String> getConfigParameters() {
		Map<String,String> params = new HashMap<>(1);
		params.put(POPERTY_NAME_MINCONFIDENCE, minConfidenceType.toString());
		return params;
	}
}
