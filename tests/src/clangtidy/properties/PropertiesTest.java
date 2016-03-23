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

import clangtidy.util.properties.*;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

/**
 * Testing access to class properties via properties API
 */
public class PropertiesTest {
	public static abstract class MyTestBase {
		@Property String baseclassproperty;

		public String getBaseclassproperty() {
			return baseclassproperty;
		}
	}

	public static class MyTestClass extends MyTestBase {
		@Property
		private String inaccessible;

		@Property
		private String readonly;

		@Property
		private String readwrite;

		@Property
		public String fullaccess;

		@Property
		private String withdefaultvalue = "DefaultValue";

		@Property
		private String nodescription;

		@Property
		@Description("PropertyDescription")
		private String withdescription;


		public String getReadonly() {
			return readonly;
		}

		public String getReadwrite() {
			return readwrite;
		}

		public void setReadwrite(String readwrite) {
			this.readwrite = readwrite;
		}

		public String getWithdefaultvalue() {
			return withdefaultvalue;
		}

		public void setWithdefaultvalue(String withdefaultvalue) {
			this.withdefaultvalue = withdefaultvalue;
		}
	}


	private ClassDescriptor<MyTestClass>		classdesc;


	@Before
	public void setUp() throws Exception {
		classdesc = ClassDescriptor.create(MyTestClass.class);
	}


	@Test
	public void testClassDesc() {
		assertNotNull(classdesc);
		assertEquals(8, classdesc.getProperties().length);
	}


	@Test
	public void testDescriptions() {
		PropertyDescriptor nodescription = classdesc.findProperty("nodescription");
		assertNotNull(nodescription);
		assertEquals(null, nodescription.getDescription());

		PropertyDescriptor withdescription = classdesc.findProperty("withdescription");
		assertNotNull(withdescription);
		assertEquals("PropertyDescription", withdescription.getDescription());
	}


	@Test
	public void testBaseClassProperty() {
		PropertyDescriptor baseclassproperty = classdesc.findProperty("baseclassproperty");
		assertNotNull(baseclassproperty);
	}


	@Test
	public void testInaccessibleProperty() {
		PropertyDescriptor inaccessible = classdesc.findProperty("inaccessible");
		assertNotNull(inaccessible);
		assertFalse(inaccessible.isReadable());
		assertFalse(inaccessible.isEditable());
	}


	@Test
	public void testReadonlyProperty() {
		PropertyDescriptor readonly = classdesc.findProperty("readonly");
		assertNotNull(readonly);
		assertTrue(readonly.isReadable());
		assertFalse(readonly.isEditable());
	}


	@Test
	public void testReadWriteProperty() {
		PropertyDescriptor readwrite = classdesc.findProperty("readwrite");
		assertNotNull(readwrite);
		assertTrue(readwrite.isReadable());
		assertTrue(readwrite.isEditable());
	}


	@Test
	public void testFullAccessProperty() {
		PropertyDescriptor fullaccess = classdesc.findProperty("fullaccess");
		assertNotNull(fullaccess);
		assertTrue(fullaccess.isReadable());
		assertTrue(fullaccess.isEditable());
	}


	@Test
	public void testGetValue1() throws InvocationTargetException, IllegalAccessException {
		ClassPropertyDescriptor<String> readwrite = classdesc.findProperty("readwrite", String.class);
		assertNotNull(readwrite);

		MyTestClass instance = new MyTestClass();

		ClassPropertyInstance<MyTestClass,String> property = ClassPropertyInstance.create(instance, readwrite);
		assertEquals(null, property.get());
	}


	@Test
	public void testGetValue2() throws InvocationTargetException, IllegalAccessException {
		ClassPropertyDescriptor<String> withdefaultvalue = classdesc.findProperty("withdefaultvalue", String.class);
		assertNotNull(withdefaultvalue);

		MyTestClass instance = new MyTestClass();

		ClassPropertyInstance<MyTestClass,String> property = ClassPropertyInstance.create(instance, withdefaultvalue);
		assertEquals("DefaultValue", instance.getWithdefaultvalue());
		assertEquals("DefaultValue", property.get());
	}


	@Test(expected = IllegalAccessException.class)
	public void testIllegalAccessException() throws InvocationTargetException, IllegalAccessException {
		ClassPropertyDescriptor<String> inaccessible = classdesc.findProperty("inaccessible", String.class);
		assertNotNull(inaccessible);

		MyTestClass instance = new MyTestClass();

		ClassPropertyInstance<MyTestClass,String> property = ClassPropertyInstance.create(instance, inaccessible);
		Object value = property.get();
	}


	@Test
	public void testSetValue() throws InvocationTargetException, IllegalAccessException {
		ClassPropertyDescriptor<String> readwrite = classdesc.findProperty("readwrite", String.class);
		assertNotNull(readwrite);

		MyTestClass instance = new MyTestClass();

		ClassPropertyInstance<MyTestClass,String> property = ClassPropertyInstance.create(instance, readwrite);
		assertEquals(null, instance.getReadwrite());
		assertEquals(null, property.get());

		property.set("New Value");
		assertEquals("New Value", instance.getReadwrite());
		assertEquals("New Value", property.get());
	}
}
