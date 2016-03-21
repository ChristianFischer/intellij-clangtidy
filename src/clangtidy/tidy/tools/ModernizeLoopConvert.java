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
import clangtidy.util.properties.Description;
import clangtidy.util.properties.Property;
import org.jetbrains.annotations.NotNull;

/**
 * Provides support for the loop-convert modernization.
 */
public class ModernizeLoopConvert extends AbstractToolController {
	@Property
	@Description("Describes the maximum risk for possible modifications.")
	public RiskLevel			MinConfidence		= RiskLevel.reasonable;

	@Property
	public Integer				MaxCopySize			= 16;

	@Property
	public String				NamingStyle			= "CamelCase";


	public ModernizeLoopConvert() {
	}

	@NotNull
	@Override
	public String getName() {
		return "modernize-loop-convert";
	}


	public RiskLevel getMinConfidence() {
		return MinConfidence;
	}

	public void setMinConfidence(RiskLevel minConfidence) {
		firePropertyChange("MinConfidence", this.MinConfidence, this.MinConfidence = minConfidence);
	}


	@Override
	public void onRestoreDefaults() {
		MinConfidence	= Options.getToolProperty(this, "MinConfidence",	RiskLevel.reasonable);
		MaxCopySize		= Options.getToolProperty(this, "MaxCopySize",		16);
		NamingStyle		= Options.getToolProperty(this, "NamingStyle",		"CamelCase");
	}
}
