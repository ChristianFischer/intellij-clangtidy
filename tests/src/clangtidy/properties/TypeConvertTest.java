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

package clangtidy.properties;

import clangtidy.tidy.tools.RiskLevel;
import clangtidy.util.properties.TypeConverter;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests converting strings into various types.
 */
public class TypeConvertTest {

	@Test
	public void testInteger() {
		Integer i = TypeConverter.convertTo(Integer.class, "42");
		assertEquals(new Integer(42), i);
	}


	@Test
	public void testInt() {
		int i = TypeConverter.convertTo(int.class, "42");
		assertEquals(42, i);
	}


	@Test
	public void testBoolean() {
		boolean b = TypeConverter.convertTo(boolean.class, "true");
		assertEquals(true, b);
	}


	@Test
	public void testFloat() {
		float f = TypeConverter.convertTo(float.class, "12.34");
		assertEquals(12.34f, f, 0.00001f);
	}


	@Test
	public void testEnum() {
		RiskLevel risk = TypeConverter.convertTo(RiskLevel.class, "risky");
		assertEquals(risk, RiskLevel.risky);
	}


	@Test(expected = IllegalArgumentException.class)
	public void testEnumFail() {
		TypeConverter.convertTo(RiskLevel.class, "nothing");
	}
}
