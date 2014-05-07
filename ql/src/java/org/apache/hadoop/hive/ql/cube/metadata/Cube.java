package org.apache.hadoop.hive.ql.cube.metadata;
/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/


import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.ql.metadata.Table;

public final class Cube extends AbstractCubeTable {
  private final Set<CubeMeasure> measures;
  private final Set<CubeDimension> dimensions;
  private static final List<FieldSchema> columns = new ArrayList<FieldSchema>();
  private final Map<String, CubeMeasure> measureMap;
  private final Map<String, CubeDimension> dimMap;

  static {
    columns.add(new FieldSchema("dummy", "string", "dummy column"));
  }

  public Cube(String name, Set<CubeMeasure> measures,
      Set<CubeDimension> dimensions) {
    this(name, measures, dimensions, new HashMap<String, String>());
  }

  public Cube(String name, Set<CubeMeasure> measures,
      Set<CubeDimension> dimensions, Map<String, String> properties) {
    this(name, measures, dimensions, properties, 0L);
  }

  public Cube(String name, Set<CubeMeasure> measures,
      Set<CubeDimension> dimensions, Map<String, String> properties,
      double weight) {
    super(name, columns, properties, weight);
    this.measures = measures;
    this.dimensions = dimensions;

    measureMap = new HashMap<String, CubeMeasure>();
    for (CubeMeasure m : measures) {
      measureMap.put(m.getName().toLowerCase(), m);
    }

    dimMap = new HashMap<String, CubeDimension>();
    for (CubeDimension dim : dimensions) {
      dimMap.put(dim.getName().toLowerCase(), dim);
    }

    addProperties();
  }
  public Cube(Table tbl) {
    super(tbl);
    this.measures = getMeasures(getName(), getProperties());
    this.dimensions = getDimensions(getName(), getProperties());
    measureMap = new HashMap<String, CubeMeasure>();
    for (CubeMeasure m : measures) {
      measureMap.put(m.getName().toLowerCase(), m);
    }

    dimMap = new HashMap<String, CubeDimension>();
    for (CubeDimension dim : dimensions) {
      addAllDimsToMap(dim);
    }
  }

  private void addAllDimsToMap(CubeDimension dim) {
    dimMap.put(dim.getName().toLowerCase(), dim);
    if (dim instanceof HierarchicalDimension) {
      for (CubeDimension d : ((HierarchicalDimension)dim).getHierarchy()) {
        addAllDimsToMap(d);
      }
    }
  }
  public Set<CubeMeasure> getMeasures() {
    return measures;
  }

  public Set<CubeDimension> getDimensions() {
    return dimensions;
  }

  public Set<String> getTimedDimensions() {
    String str = getProperties().get(
        MetastoreUtil.getCubeTimedDimensionListKey(getName()));
    if (str != null) {
      Set<String> timedDimensions = new HashSet<String>();
      timedDimensions.addAll(Arrays.asList(StringUtils.split(str, ',')));
      return timedDimensions;
    }
    return null;
  }

  @Override
  public CubeTableType getTableType() {
    return CubeTableType.CUBE;
  }

  @Override
  public Set<String> getStorages() {
    return null;
  }

  @Override
  public void addProperties() {
    super.addProperties();
    getProperties().put(MetastoreUtil.getCubeMeasureListKey(getName()),
        MetastoreUtil.getNamedStr(measures));
    setMeasureProperties(getProperties(), measures);
    getProperties().put(MetastoreUtil.getCubeDimensionListKey(getName()),
        MetastoreUtil.getNamedStr(dimensions));
    setDimensionProperties(getProperties(), dimensions);
  }

  private static void setMeasureProperties(Map<String, String> props,
                                           Set<CubeMeasure> measures) {
    for (CubeMeasure measure : measures) {
      measure.addProperties(props);
    }
  }

  private static void setDimensionProperties(Map<String, String> props,
                                             Set<CubeDimension> dimensions) {
    for (CubeDimension dimension : dimensions) {
      dimension.addProperties(props);
    }
  }

  public static Set<CubeMeasure> getMeasures(String name,
      Map<String, String> props) {
    Set<CubeMeasure> measures = new HashSet<CubeMeasure>();
    String measureStr = props.get(MetastoreUtil.getCubeMeasureListKey(name));
    String[] names = measureStr.split(",");
    for (String measureName : names) {
      String className = props.get(MetastoreUtil.getMeasureClassPropertyKey(
          measureName));
      CubeMeasure measure;
      try {
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor;
        constructor = clazz.getConstructor(String.class, Map.class);
        measure = (CubeMeasure) constructor.newInstance(new Object[]
            {measureName, props});
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid measure", e);
      }
      measures.add(measure);
    }
    return measures;
  }

  public static Set<CubeDimension> getDimensions(String name,
      Map<String, String> props) {
    Set<CubeDimension> dimensions = new HashSet<CubeDimension>();
    String dimStr = props.get(MetastoreUtil.getCubeDimensionListKey(name));
    String[] names = dimStr.split(",");
    for (String dimName : names) {
      String className = props.get(MetastoreUtil.getDimensionClassPropertyKey(
          dimName));
      CubeDimension dim;
      try {
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor;
        constructor = clazz.getConstructor(String.class, Map.class);
        dim = (CubeDimension) constructor.newInstance(new Object[]
            {dimName, props});
      } catch (Exception e) {
        throw new IllegalArgumentException("Invalid dimension", e);
      }
      dimensions.add(dim);
    }
    return dimensions;
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    Cube other = (Cube) obj;
    if (this.getMeasures() == null) {
      if (other.getMeasures() != null) {
        return false;
      }
    } else if (!this.getMeasures().equals(other.getMeasures())) {
      return false;
    }
    if (this.getDimensions() == null) {
      if (other.getDimensions() != null) {
        return false;
      }
    } else if (!this.getDimensions().equals(other.getDimensions())) {
      return false;
    }
    return true;
  }

  public CubeDimension getDimensionByName(String dimension) {
    return dimMap.get(dimension == null ? dimension : dimension.toLowerCase());
  }

  public CubeMeasure getMeasureByName(String measure) {
    return measureMap.get(measure == null ? measure : measure.toLowerCase());
  }

  public CubeColumn getColumnByName(String column) {
    CubeColumn cubeCol = (CubeColumn)getMeasureByName(column);
    if (cubeCol == null) {
      cubeCol = (CubeColumn)getDimensionByName(column);
    }
    return cubeCol;
  }

  /**
   * Alters the measure if already existing or just adds if it is new measure.
   *
   * @param measure
   * @throws HiveException
   */
  public void alterMeasure(CubeMeasure measure) throws HiveException {
    if (measure == null) {
      throw new NullPointerException("Cannot add null measure");
    }

    // Replace measure if already existing
    if (measureMap.containsKey(measure.getName().toLowerCase()))  {
      measures.remove(getMeasureByName(measure.getName()));
      LOG.info("Replacing measure " + getMeasureByName(measure.getName())
        + " with " + measure);
    }

    measures.add(measure);
    measureMap.put(measure.getName().toLowerCase(), measure);
    getProperties().put(MetastoreUtil.getCubeMeasureListKey(getName()),
        MetastoreUtil.getNamedStr(measures));
    measure.addProperties(getProperties());
  }

  /**
   * Alters the dimension if already existing or just adds if it is new dimension
   *
   * @param dimension
   * @throws HiveException
   */
  public void alterDimension(CubeDimension dimension) throws HiveException {
    if (dimension == null) {
      throw new NullPointerException("Cannot add null dimension");
    }

    // Replace dimension if already existing
    if (dimMap.containsKey(dimension.getName().toLowerCase())) {
      dimensions.remove(getDimensionByName(dimension.getName()));
      LOG.info("Replacing dimension " + getDimensionByName(dimension.getName())
        + " with " + dimension);
    }

    dimensions.add(dimension);
    dimMap.put(dimension.getName().toLowerCase(), dimension);
    getProperties().put(MetastoreUtil.getCubeDimensionListKey(getName()),
        MetastoreUtil.getNamedStr(dimensions));
    dimension.addProperties(getProperties());
  }

  /**
   * Remove the dimension with name specified
   *
   * @param dimName
   */
  public void removeDimension(String dimName) {
    if (dimMap.containsKey(dimName.toLowerCase())) {
      LOG.info("Removing dimension " + getDimensionByName(dimName));
      dimensions.remove(getDimensionByName(dimName));
      dimMap.remove(dimName.toLowerCase());
      getProperties().put(MetastoreUtil.getCubeDimensionListKey(getName()),
          MetastoreUtil.getNamedStr(dimensions));
    }
  }

  /**
   * Remove the measure with name specified
   *
   * @param msrName
   */
  public void removeMeasure(String msrName) {
    if (measureMap.containsKey(msrName.toLowerCase())) {
      LOG.info("Removing measure " + getMeasureByName(msrName));
      measures.remove(getMeasureByName(msrName));
      measureMap.remove(msrName.toLowerCase());
      getProperties().put(MetastoreUtil.getCubeMeasureListKey(getName()),
          MetastoreUtil.getNamedStr(measures));
    }
  }

  /**
   * Adds the timed dimension
   *
   * @param timedDimension
   * @throws HiveException
   */
  public void addTimedDimension(String timedDimension) throws HiveException {
    if (timedDimension == null || timedDimension.isEmpty()) {
      throw new HiveException("Invalid timed dimension " + timedDimension);
    }
    timedDimension = timedDimension.toLowerCase();
    Set<String> timeDims = getTimedDimensions();
    if (timeDims == null) {
      timeDims = new LinkedHashSet<String>();
    }
    if (timeDims.contains(timedDimension)) {
      LOG.info("Timed dimension " + timedDimension + " is" +
        " already present in cube "+ getName());
      return;
    }

    timeDims.add(timedDimension);
    getProperties().put(MetastoreUtil.getCubeTimedDimensionListKey(getName()),
        StringUtils.join(timeDims, ","));
  }

  /**
   * Removes the timed dimension
   *
   * @param timedDimension
   * @throws HiveException
   */
  public void removeTimedDimension(String timedDimension) throws HiveException {
    if (timedDimension == null || timedDimension.isEmpty()) {
      throw new HiveException("Invalid timed dimension " + timedDimension);
    }
    timedDimension = timedDimension.toLowerCase();
    Set<String> timeDims = getTimedDimensions();
    if (timeDims != null && timeDims.contains(timedDimension)) {
      timeDims.remove(timedDimension);
      getProperties().put(MetastoreUtil.getCubeTimedDimensionListKey(getName()),
          StringUtils.join(timeDims, ","));
    }
  }
}