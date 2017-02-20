/*
 * Copyright (c) 2017 wetransform GmbH
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     wetransform GmbH <http://www.wetransform.to>
 */

package eu.esdihumboldt.hale.common.instance.index.spatial;

import java.util.Collection;

import de.fhg.igd.geom.BoundingBox;
import de.fhg.igd.geom.Localizable;
import de.fhg.igd.geom.Verifier;
import de.fhg.igd.geom.indices.RTree;

/**
 * Spatial index service using an {@link RTree} to maintain the index.
 * 
 * @author Florian Esser
 */
public class RTreeSpatialIndexService implements SpatialIndexService<Localizable, BoundingBox> {

	private final RTree<Localizable> index;

	/**
	 * Verifier to determine whether the bounding box of a given
	 * {@link Localizable} insersects or covers a given {@link BoundingBox}.
	 */
	public static final Verifier<Localizable, BoundingBox> MATCH_TILE_VERIFIER = new Verifier<Localizable, BoundingBox>() {

		@Override
		public boolean verify(Localizable first, BoundingBox second) {
			return second.intersectsOrCovers(first.getBoundingBox());
		}
	};

	/**
	 * Creates an index service using an {@link RTree} with the given page size.
	 * 
	 * @param pageSize the page size of the RTree, must be >= 4.
	 * @see de.fhg.igd.geom.indices.RTree
	 */
	public RTreeSpatialIndexService(int pageSize) {
		this.index = new RTree<>(pageSize);
	}

	/**
	 * @see eu.esdihumboldt.hale.common.instance.index.spatial.SpatialIndexService#insert(de.fhg.igd.geom.Localizable)
	 */
	@Override
	public void insert(Localizable reference) {
		index.insert(reference);
	}

	/**
	 * @see eu.esdihumboldt.hale.common.instance.index.spatial.SpatialIndexService#retrieve(de.fhg.igd.geom.Localizable)
	 */
	@Override
	public Collection<Localizable> retrieve(BoundingBox box) {
		return index.query(box, MATCH_TILE_VERIFIER);
	}

	/**
	 * @see eu.esdihumboldt.hale.common.instance.index.spatial.SpatialIndexService#retrieve(de.fhg.igd.geom.Localizable,
	 *      de.fhg.igd.geom.Verifier)
	 */
	@Override
	public Collection<Localizable> retrieve(BoundingBox box,
			Verifier<? super Localizable, BoundingBox> verifier) {
		return index.query(box, verifier);
	}

}
