/*
 * $Header: /home/cvs/jakarta-struts/src/share/org/apache/struts/action/DynaActionForm.java,v 1.14 2004/03/14 06:23:42 sraeburn Exp $
 * $Revision: 1.14 $
 * $Date: 2004/03/14 06:23:42 $
 *
 * Copyright 2000-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.struts.action;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;
import org.apache.commons.beanutils.DynaProperty;
import org.apache.struts.config.FormBeanConfig;
import org.apache.struts.config.FormPropertyConfig;


/**
 * <p>Specialized subclass of <code>ActionForm</code> that allows the creation
 * of form beans with dynamic sets of properties, without requiring the
 * developer to create a Java class for each type of form bean.</p>
 *
 * <p><strong>USAGE NOTE</strong> - Since Struts 1.1, the
 * <code>reset</code> method no longer initializes property values to those
 * specified in <code>&lt;form-property&gt;</code> elements in the Struts
 * module configuration file.  If you wish to utilize that behavior, the
 * simplest solution is to subclass <code>DynaActionForm</code> and call
 * the <code>initialize</code> method inside it.</p>
 *
 * @version $Revision: 1.14 $ $Date: 2004/03/14 06:23:42 $
 * @since Struts 1.1
 */
public class DynaActionForm extends ActionForm implements DynaBean {


    // ----------------------------------------------------- Instance Variables


    /**
     * <p>The <code>DynaActionFormClass</code> with which we are associated.
     * </p>
     */
    protected DynaActionFormClass dynaClass = null;


    /**
     * <p>The set of property values for this <code>DynaActionForm</code>,
     * keyed by property name.</p>
     */
    protected HashMap dynaValues = new HashMap();


    // ----------------------------------------------------- ActionForm Methods


    /**
     * <p>Initialize all bean properties to their initial values, as specified
     * in the {@link FormPropertyConfig} elements associated with the
     * definition of this <code>DynaActionForm</code>.</p>
     *
     * @param mapping The mapping used to select this instance
     */
    public void initialize(ActionMapping mapping) {

        String name = mapping.getName();
        if (name == null) {
            return;
        }
        FormBeanConfig config =
            mapping.getModuleConfig().findFormBeanConfig(name);
        if (config == null) {
            return;
        }

        initialize(config);
    }

    public void initialize(FormBeanConfig config) {

        FormPropertyConfig props[] = config.findFormPropertyConfigs();
        for (int i = 0; i < props.length; i++) {
            set(props[i].getName(), props[i].initial());
        }

    }


    // :FIXME: Is there any point in retaining these reset methods
    // since they now simply replicate the superclass behavior?

    /**
     * <p>Reset bean properties to their default state, as needed.
     * This method is called before the properties are repopulated by
     * the controller.</p>
     *
     * <p>The default implementation attempts to forward to the HTTP
     * version of this method.</p>
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    public void reset(ActionMapping mapping, ServletRequest request) {

        try {
            reset(mapping, (HttpServletRequest) request);
        } catch (ClassCastException e) {
            ;
        }

    }


    /**
     * <p>Reset bean properties to their default state, as needed.  This method is
     * called before the properties are repopulated by the controller.</p>
     *
     * <p>The default implementation (since Struts 1.1) does nothing.
     * Subclasses may override this method to reset bean properties to
     * default values, or the <code>initialize</code> method may be used to
     * initialize property values to those provided in the form property
     * configuration information (which was the behavior of
     * this method in some release candidates).</p>
     *
     * @param mapping The mapping used to select this instance
     * @param request The servlet request we are processing
     */
    public void reset(ActionMapping mapping, HttpServletRequest request) {

        ;       // Default implementation does nothing

    }


    // ------------------------------------------------------- DynaBean Methods


    /**
     * <p>Indicates if the specified mapped property contain a value for the
     * specified key value.</p>
     *
     * @param name Name of the property to check
     * @param key Name of the key to check
     *
     * @exception IllegalArgumentException if there is no property
     *  of the specified name
     */
    public boolean contains(String name, String key) {

        Object value = dynaValues.get(name);
        if (value == null) {
            throw new NullPointerException
                ("No mapped value for '" + name + "(" + key + ")'");
        } else if (value instanceof Map) {
            return (((Map) value).containsKey(key));
        } else {
            throw new IllegalArgumentException
                ("Non-mapped property for '" + name + "(" + key + ")'");
        }

    }


    /**
     * <p>Return the value of a simple property with the specified name.</p>
     *
     * @param name Name of the property whose value is to be retrieved
     *
     * @exception IllegalArgumentException if there is no property
     *  of the specified name
     * @exception NullPointerException if the type specified for the
     *  property is invalid
     */
    public Object get(String name) {

        // Return any non-null value for the specified property
        Object value = dynaValues.get(name);
        if (value != null) {
            return (value);
        }

        // Return a null value for a non-primitive property
        Class type = getDynaProperty(name).getType();
        if (type == null) {
            throw new NullPointerException
                ("The type for property " + name + " is invalid");
        }
        if (!type.isPrimitive()) {
            return (value);
        }

        // Manufacture default values for primitive properties
        if (type == Boolean.TYPE) {
            return (Boolean.FALSE);
        } else if (type == Byte.TYPE) {
            return (new Byte((byte) 0));
        } else if (type == Character.TYPE) {
            return (new Character((char) 0));
        } else if (type == Double.TYPE) {
            return (new Double(0.0));
        } else if (type == Float.TYPE) {
            return (new Float((float) 0.0));
        } else if (type == Integer.TYPE) {
            return (new Integer(0));
        } else if (type == Long.TYPE) {
            return (new Long(0));
        } else if (type == Short.TYPE) {
            return (new Short((short) 0));
        } else {
            return (null);
        }

    }


    /**
     * <p>Return the value of an indexed property with the specified name.
     * </p>
     *
     * @param name Name of the property whose value is to be retrieved
     * @param index Index of the value to be retrieved
     *
     * @exception IllegalArgumentException if there is no property
     *  of the specified name
     * @exception IllegalArgumentException if the specified property
     *  exists, but is not indexed
     * @exception IndexOutOfBoundsException if the specified index
     *  is outside the range of the underlying property
     * @exception NullPointerException if no array or List has been
     *  initialized for this property
     */
    public Object get(String name, int index) {

        Object value = dynaValues.get(name);
        if (value == null) {
            throw new NullPointerException
                ("No indexed value for '" + name + "[" + index + "]'");
        } else if (value.getClass().isArray()) {
            return (Array.get(value, index));
        } else if (value instanceof List) {
            return ((List) value).get(index);
        } else {
            throw new IllegalArgumentException
                ("Non-indexed property for '" + name + "[" + index + "]'");
        }

    }


    /**
     * <p>Return the value of a mapped property with the specified name,
     * or <code>null</code> if there is no value for the specified key.
     * </p>
     *
     * @param name Name of the property whose value is to be retrieved
     * @param key Key of the value to be retrieved
     *
     * @exception IllegalArgumentException if there is no property
     *  of the specified name
     * @exception IllegalArgumentException if the specified property
     *  exists, but is not mapped
     */
    public Object get(String name, String key) {

        Object value = dynaValues.get(name);
        if (value == null) {
            throw new NullPointerException
                ("No mapped value for '" + name + "(" + key + ")'");
        } else if (value instanceof Map) {
            return (((Map) value).get(key));
        } else {
            throw new IllegalArgumentException
                ("Non-mapped property for '" + name + "(" + key + ")'");
        }

    }


    /**
     * <p>Return the value of a <code>String</code> property with the specified
     * name. This is equivalent to calling
     * <code>(String) dynaForm.get(name)</code>.</p>
     *
     * @param name Name of the property whose value is to be retrieved
     *
     * @throws IllegalArgumentException if there is no property
     *  of the specified name
     * @throws NullPointerException if the type specified for the
     *  property is invalid
     * @throws ClassCastException if the property is not a String.
     * @since Struts 1.2
     */
    public String getString(String name) {

        return (String) this.get(name);

    }


    /**
     * <p>Return the value of a <code>String[]</code> property with the
     * specified name. This is equivalent to calling
     * <code>(String[]) dynaForm.get(name)</code>.</p>
     *
     * @param name Name of the property whose value is to be retrieved
     *
     * @throws IllegalArgumentException if there is no property
     *  of the specified name
     * @throws NullPointerException if the type specified for the
     *  property is invalid
     * @throws ClassCastException if the property is not a String[].
     * @since Struts 1.2
     */
    public String[] getStrings(String name) {

        return (String[]) this.get(name);

    }


    /**
     * <p>Return the <code>DynaClass</code> instance that describes the set
     * of properties available for this <code>DynaBean</code>.</p>
     */
    public DynaClass getDynaClass() {

        return (this.dynaClass);

    }


    /**
     * <p>Returns the <code>Map</code> containing the property values.  This is
     * done mostly to facilitate accessing the <code>DynaActionForm</code>
     * through JavaBeans accessors, in order to use the JavaServer Pages
     * Standard Tag Library (JSTL).</p>
     *
     * <p>For instance, the normal JSTL EL syntax for accessing an
     * <code>ActionForm</code> would be something like this:
     * <pre>
     *  ${formbean.prop}</pre>
     * The JSTL EL syntax for accessing a <code>DynaActionForm</code> looks
     * something like this (because of the presence of this
     * <code>getMap()</code> method):
     * <pre>
     *  ${dynabean.map.prop}</pre>
     * </p>
     */
    public Map getMap() {

        return (dynaValues);

    }


    /**
     * <p>Remove any existing value for the specified key on the
     * specified mapped property.</p>
     *
     * @param name Name of the property for which a value is to
     *  be removed
     * @param key Key of the value to be removed
     *
     * @exception IllegalArgumentException if there is no property
     *  of the specified name
     */
    public void remove(String name, String key) {

        Object value = dynaValues.get(name);
        if (value == null) {
            throw new NullPointerException
                ("No mapped value for '" + name + "(" + key + ")'");
        } else if (value instanceof Map) {
            ((Map) value).remove(key);
        } else {
            throw new IllegalArgumentException
                ("Non-mapped property for '" + name + "(" + key + ")'");
        }

    }


    /**
     * <p>Set the value of a simple property with the specified name.</p>
     *
     * @param name Name of the property whose value is to be set
     * @param value Value to which this property is to be set
     *
     * @exception ConversionException if the specified value cannot be
     *  converted to the type required for this property
     * @exception IllegalArgumentException if there is no property
     *  of the specified name
     * @exception NullPointerException if the type specified for the
     *  property is invalid
     * @exception NullPointerException if an attempt is made to set a
     *  primitive property to null
     */
    public void set(String name, Object value) {

        DynaProperty descriptor = getDynaProperty(name);
        if (descriptor.getType() == null) {
            throw new NullPointerException
                ("The type for property " + name + " is invalid");
        }
        if (value == null) {
            if (descriptor.getType().isPrimitive()) {
                throw new NullPointerException
                    ("Primitive value for '" + name + "'");
            }
        } else if (!isDynaAssignable(descriptor.getType(), value.getClass())) {
            throw new ConversionException
                ("Cannot assign value of type '" +
                 value.getClass().getName() +
                 "' to property '" + name + "' of type '" +
                 descriptor.getType().getName() + "'");
        }
        dynaValues.put(name, value);

    }


    /**
     * <p>Set the value of an indexed property with the specified name.</p>
     *
     * @param name Name of the property whose value is to be set
     * @param index Index of the property to be set
     * @param value Value to which this property is to be set
     *
     * @exception ConversionException if the specified value cannot be
     *  converted to the type required for this property
     * @exception IllegalArgumentException if there is no property
     *  of the specified name
     * @exception IllegalArgumentException if the specified property
     *  exists, but is not indexed
     * @exception IndexOutOfBoundsException if the specified index
     *  is outside the range of the underlying property
     */
    public void set(String name, int index, Object value) {

        Object prop = dynaValues.get(name);
        if (prop == null) {
            throw new NullPointerException
                ("No indexed value for '" + name + "[" + index + "]'");
        } else if (prop.getClass().isArray()) {
            Array.set(prop, index, value);
        } else if (prop instanceof List) {
            try {
                ((List) prop).set(index, value);
            } catch (ClassCastException e) {
                throw new ConversionException(e.getMessage());
            }
        } else {
            throw new IllegalArgumentException
                ("Non-indexed property for '" + name + "[" + index + "]'");
        }

    }


    /**
     * <p>Set the value of a mapped property with the specified name.</p>
     *
     * @param name Name of the property whose value is to be set
     * @param key Key of the property to be set
     * @param value Value to which this property is to be set
     *
     * @exception ConversionException if the specified value cannot be
     *  converted to the type required for this property
     * @exception IllegalArgumentException if there is no property
     *  of the specified name
     * @exception IllegalArgumentException if the specified property
     *  exists, but is not mapped
     */
    public void set(String name, String key, Object value) {

        Object prop = dynaValues.get(name);
        if (prop == null) {
            throw new NullPointerException
                ("No mapped value for '" + name + "(" + key + ")'");
        } else if (prop instanceof Map) {
            ((Map) prop).put(key, value);
        } else {
            throw new IllegalArgumentException
                ("Non-mapped property for '" + name + "(" + key + ")'");
        }

    }


    // --------------------------------------------------------- Public Methods


    /**
     * <p>Render a String representation of this object.</p>
     */
    public String toString() {

        StringBuffer sb = new StringBuffer("DynaActionForm[dynaClass=");
        sb.append(getDynaClass().getName());
        DynaProperty props[] = getDynaClass().getDynaProperties();
        if (props == null) {
            props = new DynaProperty[0];
        }
        for (int i = 0; i < props.length; i++) {
            sb.append(',');
            sb.append(props[i].getName());
            sb.append('=');
            Object value = get(props[i].getName());
            if (value == null) {
                sb.append("<NULL>");
            } else if (value.getClass().isArray()) {
                int n = Array.getLength(value);
                sb.append("{");
                for (int j = 0; j < n; j++) {
                    if (j > 0) {
                        sb.append(',');
                    }
                    sb.append(Array.get(value, j));
                }
                sb.append("}");
            } else if (value instanceof List) {
                int n = ((List) value).size();
                sb.append("{");
                for (int j = 0; j < n; j++) {
                    if (j > 0) {
                        sb.append(',');
                    }
                    sb.append(((List) value).get(j));
                }
                sb.append("}");
            } else if (value instanceof Map) {
                int n = 0;
                Iterator keys = ((Map) value).keySet().iterator();
                sb.append("{");
                while (keys.hasNext()) {
                    if (n > 0) {
                        sb.append(',');
                    }
                    n++;
                    String key = (String) keys.next();
                    sb.append(key);
                    sb.append('=');
                    sb.append(((Map) value).get(key));
                }
                sb.append("}");
            } else {
                sb.append(value);
            }
        }
        sb.append("]");
        return (sb.toString());

    }


    // -------------------------------------------------------- Package Methods


    /**
     * <p>Set the <code>DynaActionFormClass</code> instance with which we are
     * associated.</p>
     *
     * @param dynaClass The DynaActionFormClass instance for this bean
     */
    void setDynaActionFormClass(DynaActionFormClass dynaClass) {

        this.dynaClass = dynaClass;

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * <p>Return the property descriptor for the specified property name.</p>
     *
     * @param name Name of the property for which to retrieve the descriptor
     *
     * @exception IllegalArgumentException if this is not a valid property
     *  name for our DynaClass
     */
    protected DynaProperty getDynaProperty(String name) {

        DynaProperty descriptor = getDynaClass().getDynaProperty(name);
        if (descriptor == null) {
            throw new IllegalArgumentException
                ("Invalid property name '" + name + "'");
        }
        return (descriptor);

    }


    /**
     * <p>Indicates if an object of the source class is assignable to the
     * destination class.</p>
     *
     * @param dest Destination class
     * @param source Source class
     */
    protected boolean isDynaAssignable(Class dest, Class source) {

        if (dest.isAssignableFrom(source) ||
            ((dest == Boolean.TYPE) && (source == Boolean.class)) ||
            ((dest == Byte.TYPE) && (source == Byte.class)) ||
            ((dest == Character.TYPE) && (source == Character.class)) ||
            ((dest == Double.TYPE) && (source == Double.class)) ||
            ((dest == Float.TYPE) && (source == Float.class)) ||
            ((dest == Integer.TYPE) && (source == Integer.class)) ||
            ((dest == Long.TYPE) && (source == Long.class)) ||
            ((dest == Short.TYPE) && (source == Short.class))) {
            return (true);
        } else {
            return (false);
        }

    }


}
