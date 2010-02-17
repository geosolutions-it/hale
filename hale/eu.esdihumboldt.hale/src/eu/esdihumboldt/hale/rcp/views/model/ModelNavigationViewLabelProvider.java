/*
 * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
 * EU Integrated Project #030962                 01.10.2006 - 30.09.2010
 * 
 * For more information on the project, please refer to the this web site:
 * http://www.esdi-humboldt.eu
 * 
 * LICENSE: For information on the license under which this program is 
 * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
 * (c) the HUMBOLDT Consortium, 2007 to 2010.
 */
package eu.esdihumboldt.hale.rcp.views.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import eu.esdihumboldt.cst.align.ICell;
import eu.esdihumboldt.cst.align.ext.ITransformation;
import eu.esdihumboldt.cst.transformer.service.rename.RenameFeatureFunction;
import eu.esdihumboldt.goml.align.Entity;
import eu.esdihumboldt.goml.omwg.FeatureClass;
import eu.esdihumboldt.goml.omwg.Property;
import eu.esdihumboldt.hale.models.AlignmentService;
import eu.esdihumboldt.hale.rcp.HALEActivator;
import eu.esdihumboldt.hale.rcp.views.model.TreeObject.TreeObjectType;

/**
 * Provides Images for the elements of the data models in the 
 * ModelNavigationView.
 * 
 * @author Thorsten Reitz, Fraunhofer IGD; Simon Templer, Fraunhofer IGD
 * @version $Id$
 */
public class ModelNavigationViewLabelProvider extends LabelProvider
	implements IColorProvider {
	
	private final Map<RGB, Color> createdColors = new HashMap<RGB, Color>();
	
	@Override
	public String getText(Object obj) {
		return obj.toString();
	}
	
	/**
	 * Returns an adjusted image depending on the type of the object passed in.
	 * @return an Image
	 */
	@Override
	public Image getImage(Object obj) {
		TreeObject to = (TreeObject) obj;
		String imageKey = null;
		
		if (to.getType().equals(TreeObjectType.ROOT)) {
			imageKey = ISharedImages.IMG_DEF_VIEW;
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}
		else if (to.getType().equals(TreeObjectType.ABSTRACT_FT)) {
			imageKey = "abstract_ft.png"; //$NON-NLS-1$
		}
		else if (to.getType().equals(TreeObjectType.CONCRETE_FT)) {
			imageKey = "concrete_ft.png"; //$NON-NLS-1$
		}
		else if (to.getType().equals(TreeObjectType.PROPERTY_TYPE)) {
			//TODO add image for property types
		}
		else if (to.getType().equals(TreeObjectType.STRING_ATTRIBUTE)) {
			imageKey = "string_attribute.png"; //$NON-NLS-1$
		} 
		else if (to.getType().equals(TreeObjectType.NUMERIC_ATTRIBUTE)) {
			imageKey = "number_attribute.png"; //$NON-NLS-1$
		}
		else if (to.getType().equals(TreeObjectType.GEOMETRIC_ATTRIBUTE)) {
			imageKey = "geometry_attribute.png"; //$NON-NLS-1$
		}
		else if (to.getType().equals(TreeObjectType.COMPLEX_ATTRIBUTE)) {
			// TODO add image for complex attributes
		}
		
		if (imageKey == null) {
			// default
			imageKey = ISharedImages.IMG_OBJ_ELEMENT;
			return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
		}
		else  {
			return AbstractUIPlugin.imageDescriptorFromPlugin(
				HALEActivator.PLUGIN_ID, "/icons/" + imageKey).createImage(); //$NON-NLS-1$
		}
	}

	/**
	 * @see IColorProvider#getBackground(Object)
	 */
	@Override
	public Color getBackground(Object element) {
		return getColor(element, true);
	}
	
	/**
	 * @see IColorProvider#getForeground(Object)
	 */
	@Override
	public Color getForeground(Object element) {
		return getColor(element, false);
	}

	/**
	 * Get the element color
	 * 
	 * @param element the element
	 * @param background if the background color (or the foreground color)
	 *   is requested
	 * @return the color
	 */
	private Color getColor(Object element, boolean background) {
		if (element instanceof TreeObject) {
			Entity entity = ((SchemaItem) element).getEntity();
			
			RGB rgb = getColor(entity, background);
			if (rgb != null) {
				Color color = createdColors.get(rgb);
				if (color == null) {
					color = new Color(Display.getDefault(), rgb);
					createdColors.put(rgb, color);
				}
				return color;
			}
		}
		
		// default
		return null;
	}
	
	/**
	 * Get the entity color
	 * 
	 * @param entity the entity
	 * @param background if the background color (or the foreground color)
	 *   is requested
	 * @return the color's RGB values
	 */
	private RGB getColor(Entity entity, boolean background) {
		if (entity == null) return null;
		
		if (background) { // TODO Symbol or gradient?
			if (entity instanceof FeatureClass) {
				FeatureClass entityFC = (FeatureClass) entity;
				if (entityFC.getAttributeValueCondition() != null 
						&& entityFC.getAttributeValueCondition().get(0) != null) {
					return new RGB(250, 250, 30);
				}
			}
			if (entity instanceof Property) {
				Property entityProperty = (Property) entity;
				if (entityProperty.getValueCondition() != null 
						&& entityProperty.getValueCondition().get(0) != null) {
					return new RGB(250, 250, 30);
				}
			}
		}
		
		AlignmentService as = (AlignmentService) PlatformUI.getWorkbench().getService(
				AlignmentService.class);
		List<ICell> cells = as.getCell(entity);
		
		List<ITransformation> transformations = new ArrayList<ITransformation>();
		for (ICell cell : cells) {
			ITransformation trans = cell.getEntity1().getTransformation();
			if (trans != null) {
				transformations.add(trans);
			}
		}
		
		if (background) {
			return getBackgroundColor(transformations);
		}
		else {
			return getForegroundColor(transformations);
		}
	}

	/**
	 * Get the foreground color for an element with the given transformations
	 * 
	 * @param transformations the transformations
	 * @return the color's RGB values, <code>null</code> for the default color
	 */
	protected RGB getForegroundColor(List<ITransformation> transformations) {
		// default color
		return null;
	}

	/**
	 * Get the background color for an element with the given transformations
	 * 
	 * @param transformations the transformations
	 * @return the color's RGB values, <code>null</code> for the default color
	 */
	protected RGB getBackgroundColor(List<ITransformation> transformations) {
		Set<String> names = new HashSet<String>();
		for (ITransformation trans : transformations) {
			names.add(trans.getService().getLocation()); //XXX service is null!
		}
		
		if (!names.isEmpty()) {
			if (names.contains(RenameFeatureFunction.class.getName())) {
				return new RGB(150, 190, 120);
			}
			else {
				// default color
				return new RGB(190, 220, 170);
			}
		}
		
		return null;
	}

	/**
	 * @see BaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		for (Color color : createdColors.values()) {
			color.dispose();
		}
		createdColors.clear();
			
		super.dispose();
	}
}
