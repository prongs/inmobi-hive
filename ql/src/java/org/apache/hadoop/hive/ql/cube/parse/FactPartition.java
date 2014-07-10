package org.apache.hadoop.hive.ql.cube.parse;
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


import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.hadoop.hive.ql.cube.metadata.UpdatePeriod;

class FactPartition implements Comparable<FactPartition> {
  final String partCol;
  final String partSpec;
  final Set<String> storageTables = new LinkedHashSet<String>();
  final UpdatePeriod period;
  final FactPartition containingPart;

  FactPartition(String partCol, String partSpec, UpdatePeriod period,
      FactPartition containingPart) {
    this.partCol = partCol;
    this.partSpec = partSpec;
    this.period = period;
    this.containingPart = containingPart;
  }

  FactPartition(String partCol, String partSpec, UpdatePeriod period,
      FactPartition containingPart, Set<String> storageTables) {
    this(partCol, partSpec, period, containingPart);
    this.storageTables.addAll(storageTables);
  }

  String getFilter(String tableName) {
    StringBuilder builder = new StringBuilder();
    if (containingPart != null) {
        builder.append(containingPart.getFilter(tableName));
        builder.append(" AND ");
    }
    if (tableName != null) {
      builder.append(tableName);
      builder.append(".");
    }
    builder.append(partCol);
    builder.append("='").append(partSpec).append("'");
    return builder.toString();
  }

  @Override
  public String toString() {
    return getFilter(null);
  }

  public int compareTo(FactPartition o) {
    int colComp = this.partCol.compareTo(o.partCol);
    if (colComp == 0) {
      int partComp = this.partSpec.compareTo(o.partSpec);
      if (partComp == 0) {
        int upComp = 0;
        if (this.period != null && o.period != null) {
          upComp = this.period.compareTo(o.period);
        } else if (this.period == null && o.period == null) {
          upComp = 0;
        } else if (this.period == null) {
          upComp = -1;
        } else {
          upComp = 1;
        }
        if (upComp == 0) {
          if (this.containingPart != null) {
            if (o.containingPart == null) {
              return 1;
            }
            return this.containingPart.compareTo(
                    o.containingPart);
          } else {
            if (o.containingPart != null) {
              return -1;
            } else {
              return 0;
            }
          }
        }
        return upComp;
      }
      return partComp;
    }
    return colComp;
  }
}