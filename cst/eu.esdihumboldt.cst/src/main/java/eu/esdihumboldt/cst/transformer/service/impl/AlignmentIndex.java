/*
 * HUMBOLDT: A Framework for Data Harmonisation and Service Integration.
 * EU Integrated Project #030962                  01.10.2006 - 30.09.2010
 * 
 * For more information on the project, please refer to the this web site:
 * http://www.esdi-humboldt.eu
 * 
 * LICENSE: For information on the license under which this program is 
 * available, please refer to http:/www.esdi-humboldt.eu/license.html#core
 * (c) the HUMBOLDT Consortium, 2007 to 2010.
 */

package eu.esdihumboldt.cst.transformer.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.opengis.feature.type.FeatureType;

import eu.esdihumboldt.cst.align.IAlignment;
import eu.esdihumboldt.cst.align.ICell;
import eu.esdihumboldt.goml.align.Alignment;
import eu.esdihumboldt.goml.align.Entity;
import eu.esdihumboldt.goml.omwg.ComposedProperty;
import eu.esdihumboldt.goml.omwg.FeatureClass;
import eu.esdihumboldt.goml.omwg.Property;

/**
 * The {@link AlignmentIndex} is used by the {@link SchemaTranslationController}
 * to access information about a given {@link Alignment} in convenient ways.
 * 
 * @author Thorsten Reitz 
 * @partner 01 / Fraunhofer Institute for Computer Graphics Research
 * @version $Id$ 
 */
public class AlignmentIndex {
	
	private static Logger _log = Logger.getLogger(AlignmentIndex.class);
	
	private final IAlignment alignment;
	
	Set<String> targetTypes = new HashSet<String>();
	Set<String> sourceTypes = new HashSet<String>();
	Map<String, List<ICell>> cellsFtIndex = 
		new HashMap<String, List<ICell>>();
	
	// constructors ............................................................

	public AlignmentIndex(IAlignment al) {
		this.alignment = al;
		for (ICell c : this.alignment.getMap()) {
			try {
				if (c.getEntity1().getClass().isAssignableFrom(FeatureClass.class)) {
					this.sourceTypes.add(c.getEntity1().getAbout().getAbout());
				}
				if (c.getEntity2().getClass().isAssignableFrom(FeatureClass.class)) {
					this.targetTypes.add(c.getEntity2().getAbout().getAbout());
				}
			} catch (Exception e) {
				_log.error("An Entity's name could not be parsed to an URL: ", e);
			}
		}
		this.determineCellsPerFeatureType(al);
	}
	
	// main methods ............................................................
	
	/**
	 * @param key
	 *         a String identifying a {@link FeatureType}
	 * @return a {@link List} of {@link String}s that denote all
	 *         {@link FeatureType}s that are mapped to the {@link FeatureType}
	 *         identified by key. If key identifies a source {@link FeatureType}
	 *         , mapped target {@link FeatureType}s will be returned and vice
	 *         versa.
	 */
	public Set<String> getAllMappedFeatureTypes(String key){
		Set<String> result = new HashSet<String>();
		for (ICell cell : this.cellsFtIndex.get(key)) {
			if (this.isSourceType(key)) {
				result.add(this.getKeyFromEntity((Entity) cell.getEntity2()));
			}
			else if (this.isTargetType(key)) {
				result.add(this.getKeyFromEntity((Entity) cell.getEntity1()));
			}
			else {
				throw new RuntimeException("Unknown Entity String.");
			}
		}
		return result;
	}
	
	/**
	 * @param key {@link URL} identifying a {@link FeatureType}
	 * @return a {@link List} of all {@link ICell}s that contain the given 
	 * {@link Entity} URL. If a {@link FeatureType} URL is passed, it will also 
	 * return all property {@link ICell}s below.
	 */
	public List<ICell> getCellsPerEntity(String key) {
		//FIXME: how to handle entities that are defined on inheritance level?
		return this.cellsFtIndex.get(key);
	}
	
	/**
	 * @param key {@link URL} identifying a {@link FeatureType}
	 * @return true if the given {@link URL} identifies a known target 
	 * {@link FeatureType}.
	 */
	public boolean isTargetType(String key) {
		return this.targetTypes.contains(key);
	}
	
	/**
	 * @param key {@link URL} identifying a {@link FeatureType} 
	 * @return true if the given {@link URL} identifies a known source 
	 * {@link FeatureType}.
	 */
	public boolean isSourceType(String key) {
		return this.sourceTypes.contains(key);
	}
	
	/**
	 * @return a {@link Set} of {@link URL}s with all target {@link FeatureType} 
	 * identifiers.
	 */
	public Set<String> getTargetTypes() {
		return this.targetTypes;
	}
	
	/**
	 * @return a {@link Set} of {@link URL}s with all source {@link FeatureType} 
	 * identifiers.
	 */
	public Set<String> getSourceTypes() {
		return this.sourceTypes;
	}
	
	// helper and initialization methods .......................................
	
	/**
	 * @param alignment
	 * @return a Map that gives, for each source and target FeatureType, the 
	 * cells that are relevant.
	 */
	private void determineCellsPerFeatureType(IAlignment alignment) {
		
		// step through all cells
		for (ICell cell : alignment.getMap()) {
			List<ICell> thisCellList = null;
			
			// determine FT key, i.e. ignore Property names.
			Entity e1 = (Entity) cell.getEntity1();
			Entity e2 = (Entity) cell.getEntity2();
			String key1 = this.getKeyFromEntity(e1);
			String key2 = this.getKeyFromEntity(e2);
			
			// add Cells to source FeatureTypes
			if (cellsFtIndex.containsKey(key1)) {
				thisCellList = cellsFtIndex.get(key1);
				thisCellList.add(cell);
			}
			else {
				thisCellList = new ArrayList<ICell>();
				thisCellList.add(cell);
				cellsFtIndex.put(key1, thisCellList);
			}
			
			// add Cells to target FeatureTypes
			if (cellsFtIndex.containsKey(key2)) {
				thisCellList = cellsFtIndex.get(key2);
				thisCellList.add(cell);
			}
			else {
				thisCellList = new ArrayList<ICell>();
				thisCellList.add(cell);
				cellsFtIndex.put(key2, thisCellList);
			}
		}
	}
	
	/**
	 * This method will return a {@link FeatureType} key for both 
	 * {@link FeatureType} keys and {@link Property} keys.
	 * @param key the input key
	 * @return a {@link String} for a {@link FeatureType} key.
	 */
	public static String getFeatureTypeKey(String key, String namespace) {
		if (!namespace.endsWith("/")) {
			namespace = namespace + "/";
		}
		String localpart = key.replace(namespace, "");
		if (localpart.contains("/")) {
			String[] localpartSubstrings = localpart.split("\\/");
			return namespace + localpartSubstrings[0];
		}
		else {
			return key;
		}
	}

	/**
	 * @param ftName a URL identifying a {@link FeatureType} for which
	 * {@link ICell}s containing an Equivalence Relation is looked after.
	 * @return a {@link List} with all {@link ICell}s containing an 
	 * Equivalence relation, or an empty list if no such ICell is encountered.
	 */
	public List<ICell> getRenameCell(String ftName) {
		List<ICell> result = new ArrayList<ICell>();
		for (ICell cell : this.getCellsPerEntity(ftName)) {
			if (ICell.RelationType.Equivalence.equals(cell.getRelation())) {
				result.add(cell);
			}
		}
		return result;
	}
	
	private String getKeyFromEntity(Entity e) {
		String key = null;
		if (e.getClass().isAssignableFrom(FeatureClass.class)) {
			key = e.getAbout().getAbout();
		}
		else if (e.getClass().isAssignableFrom(Property.class)) {
			key = ((Property)e).getNamespace() + "/" 
					+ ((Property)e).getFeatureClassName();
		}
		else if (e.getClass().isAssignableFrom(ComposedProperty.class)) {
			// FIXME determine if multiple FTs are involved, and if yes, register all of them!
			Property firstProperty = ((ComposedProperty)e).getCollection().get(0);
			key = firstProperty.getNamespace() + "/" 
					+ firstProperty.getFeatureClassName();
		}
		return key;
	}
	
}
