
/*
 * The Unified Mapping Platform (JUMP) is an extensible, interactive GUI 
 * for visualizing and manipulating spatial features with geometry and attributes.
 *
 * Copyright (C) 2003 Vivid Solutions
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * Vivid Solutions
 * Suite #1A
 * 2328 Government Street
 * Victoria BC  V8T 5G5
 * Canada
 *
 * (250)385-6040
 * www.vividsolutions.com
 */

package com.vividsolutions.jump.feature;

import org.locationtech.jts.geom.Envelope;

import java.io.Serializable;
import java.util.*;


/**
 * Default implementation of FeatureCollection.
 */
public class FeatureDataset implements FeatureCollection, Serializable {

    private static final long serialVersionUID = 5573446944516446540L;
    private FeatureSchema featureSchema;

    //<<TODO>> Possibly use hashtable to do spatial indexing [Jon Aquino]
    private List<Feature> features;
    private Envelope envelope = null;

    /**
     * Creates a FeatureDataset, initialized with a group of Features.
     * @param newFeatures an initial group of features to add to this FeatureDataset
     * @param featureSchema the types of the attributes of the features in this collection
     */
    public FeatureDataset(Collection<Feature> newFeatures, FeatureSchema featureSchema) {
        features = new ArrayList<>(newFeatures);
        this.featureSchema = featureSchema;
    }

    /**
     * Creates a FeatureDataset.
     * @param featureSchema the types of the attributes of the features in this collection
     */
    public FeatureDataset(FeatureSchema featureSchema) {
        this(new ArrayList<Feature>(), featureSchema);
    }

    /**
     * Returns the Feature at the given index (zero-based).
     */
    public Feature getFeature(int index) {
        return features.get(index);
    }

    @Override
    public FeatureSchema getFeatureSchema() {
        return featureSchema;
    }

    /**
     * Because the envelope is cached, the envelope may be incorrect if you
     * later change a Feature's geometry using Feature#setGeometry.
     */
    @Override
    public Envelope getEnvelope() {
        if (envelope == null) {
            envelope = new Envelope();

            for (Feature feature : features) {
                envelope.expandToInclude(feature.getGeometry().getEnvelopeInternal());
            }
        }

        return envelope;
    }

    @Override
    public List<Feature> getFeatures() {
        return Collections.unmodifiableList(features);
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * @return a List containing the features whose envelopes intersect the given envelope
     */

    //<<TODO:DESIGN>> Perhaps return value should be a Set, not a List, because order
    //doesn't matter. [Jon Aquino]
    @Override
    public List<Feature> query(Envelope envelope) {
        if (!envelope.intersects(getEnvelope())) {
            return new ArrayList<>();
        }

        //<<TODO:NAMING>> Rename this method to getFeatures(Envelope), to parallel
        //getFeatures() [Jon Aquino]
        List<Feature> queryResult = new ArrayList<>();

        for (Feature feature : features) {
            if (feature.getGeometry().getEnvelopeInternal().intersects(envelope)) {
                queryResult.add(feature);
            }
        }

        return queryResult;
    }

    @Override
    public void add(Feature feature) {
        features.add(feature);
        if (envelope != null) {
            envelope.expandToInclude(feature.getGeometry().getEnvelopeInternal());
        }
    }

    /**
     * Returns whether or not this Feature is in this collection
     * @return true if this feature is in this collection, as determined using
     * Feature#equals
     */
    public boolean contains(Feature feature) {
        return features.contains(feature);
    }

    /**
     * Removes the features which intersect the given envelope
     */
    @Override
    public Collection<Feature> remove(Envelope env) {
        Collection<Feature> features = query(env);
        removeAll(features);

        return features;
    }

    @Override
    public void remove(Feature feature) {
        features.remove(feature);
        invalidateEnvelope();
    }

    /**
     * Removes all features from this collection.
     */
    @Override
    public void clear() {
        invalidateEnvelope();
        features.clear();
    }

    @Override
    public int size() {
        return features.size();
    }

    @Override
    public Iterator<Feature> iterator() {
        return features.iterator();
    }

    /**
     * Clears the cached envelope of this FeatureDataset's Features. Call this method
     * when a Feature's Geometry is modified.
     */
    public void invalidateEnvelope() {
        envelope = null;
    }

    @Override
    public void addAll(Collection<Feature> features) {
        this.features.addAll(features);
        if (envelope != null) {
            for (Feature feature : features) {
                envelope.expandToInclude(feature.getGeometry().getEnvelopeInternal());
            }            
        }
    }
    
    // [michaudm 2009-05-16] creating a map on the fly improves dramatically
    // the removeAll performance if c is large
    // note that the semantic is slightly changed as the FID is used to identify 
    // features to remove rather than object Equality
    // [michaudm 2013-07-13] change HashMap to LinkedHashMap to preserve feature order
    @Override
    public void removeAll(Collection<Feature> c) {
        Map<Integer,Feature> map = new LinkedHashMap<>();
        for (Feature feature : features) {
            map.put(feature.getID(), feature);
        }
        for (Feature feature : c) {
            map.remove(feature.getID());
        }
        features = new ArrayList<>();
        features.addAll(map.values());
        invalidateEnvelope();
    }
}
