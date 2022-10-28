/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.cdc.oracle.table;

import io.debezium.connector.AbstractSourceInfo;
import io.debezium.data.Envelope;
import io.debezium.data.Envelope.FieldName;
import io.debezium.relational.Table;
import io.debezium.relational.history.TableChanges;
import io.debezium.relational.history.TableChanges.TableChange;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.GenericArrayData;
import org.apache.flink.table.data.GenericMapData;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.types.DataType;
import org.apache.inlong.sort.cdc.oracle.debezium.table.MetadataConverter;
import org.apache.inlong.sort.formats.json.canal.CanalJson;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;

/** Defines the supported metadata columns for {@link OracleTableSource}. */
public enum OracleReadableMetaData {

    /** Name of the table that contain the row. */
    TABLE_NAME(
            "table_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY));
                }
            }),
    /** Name of the schema that contain the row. */
    SCHEMA_NAME(
            "schema_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.SCHEMA_NAME_KEY));
                }
            }),

    /** Name of the database that contain the row. */
    DATABASE_NAME(
            "database_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY));
                }
            }),

    /**
     * It indicates the time that the change was made in the database. If the record is read from
     * snapshot of the table instead of the change stream, the value is always 0.
     */
    OP_TS(
            "op_ts",
            DataTypes.TIMESTAMP_LTZ(3).notNull(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    Struct messageStruct = (Struct) record.value();
                    Struct sourceStruct = messageStruct.getStruct(Envelope.FieldName.SOURCE);
                    return TimestampData.fromEpochMillis(
                            (Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY));
                }
            }),

    DATA(
            "meta.data",
            DataTypes.STRING(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(SourceRecord record,
                        @Nullable TableChanges.TableChange tableSchema, RowData rowData) {
                    return getCanalData(record, tableSchema, (GenericRowData) rowData);
                }
            }),

    DATA_CANAL(
            "meta.data_canal",
            DataTypes.STRING(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(SourceRecord record,
                        @Nullable TableChanges.TableChange tableSchema, RowData rowData) {
                    return getCanalData(record, tableSchema, (GenericRowData) rowData);
                }
            }),

    /**
     * Name of the table that contain the row. .
     */
    META_TABLE_NAME(
            "meta.table_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY));
                }
            }),

    /**
     * Name of the database that contain the row.
     */
    META_DATABASE_NAME(
            "meta.database_name",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY));
                }
            }),

    /**
     * It indicates the time that the change was made in the database. If the record is read from
     * snapshot of the table instead of the binlog, the value is always 0.
     */
    META_OP_TS(
            "meta.op_ts",
            DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).notNull(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    Struct messageStruct = (Struct) record.value();
                    Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
                    return TimestampData.fromEpochMillis(
                            (Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY));
                }
            }),

    /**
     * Operation type, INSERT/UPDATE/DELETE.
     */
    OP_TYPE(
            "meta.op_type",
            DataTypes.STRING().notNull(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString(getOpType(record));
                }
            }),

    /**
     * Not important, a simple increment counter.
     */
    BATCH_ID(
            "meta.batch_id",
            DataTypes.BIGINT().nullable(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                private long id = 0;

                @Override
                public Object read(SourceRecord record) {
                    return id++;
                }
            }),

    /**
     * Source does not emit ddl data.
     */
    IS_DDL(
            "meta.is_ddl",
            DataTypes.BOOLEAN().notNull(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return false;
                }
            }),

    /**
     * The update-before data for UPDATE record.
     */
    OLD(
            "meta.update_before",
            DataTypes.ARRAY(
                            DataTypes.MAP(
                                            DataTypes.STRING().nullable(),
                                            DataTypes.STRING().nullable())
                                    .nullable())
                    .nullable(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    final Envelope.Operation op = Envelope.operationFor(record);
                    if (op != Envelope.Operation.UPDATE) {
                        return null;
                    }
                    return record;
                }
            }),

    MYSQL_TYPE(
            "meta.mysql_type",
            DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.STRING().nullable()).nullable(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(
                        SourceRecord record, @Nullable TableChanges.TableChange tableSchema) {
                    if (tableSchema == null) {
                        return null;
                    }
                    Map<StringData, StringData> mysqlType = new HashMap<>();
                    final Table table = tableSchema.getTable();
                    table.columns()
                            .forEach(
                                    column -> {
                                        mysqlType.put(
                                                StringData.fromString(column.name()),
                                                StringData.fromString(
                                                        String.format(
                                                                "%s(%d)",
                                                                column.typeName(),
                                                                column.length())));
                                    });

                    return new GenericMapData(mysqlType);
                }
            }),

    PK_NAMES(
            "meta.pk_names",
            DataTypes.ARRAY(DataTypes.STRING().nullable()).nullable(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(
                        SourceRecord record, @Nullable TableChanges.TableChange tableSchema) {
                    if (tableSchema == null) {
                        return null;
                    }
                    return new GenericArrayData(
                            tableSchema.getTable().primaryKeyColumnNames().stream()
                                    .map(StringData::fromString)
                                    .toArray());
                }
            }),

    SQL(
            "meta.sql",
            DataTypes.STRING().nullable(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return StringData.fromString("");
                }
            }),

    SQL_TYPE(
            "meta.sql_type",
            DataTypes.MAP(DataTypes.STRING().nullable(), DataTypes.INT().nullable()).nullable(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    return null;
                }

                @Override
                public Object read(
                        SourceRecord record, @Nullable TableChanges.TableChange tableSchema) {
                    if (tableSchema == null) {
                        return null;
                    }
                    Map<StringData, Integer> mysqlType = new HashMap<>();
                    final Table table = tableSchema.getTable();
                    table.columns()
                            .forEach(
                                    column -> {
                                        mysqlType.put(
                                                StringData.fromString(column.name()),
                                                column.jdbcType());
                                    });

                    return new GenericMapData(mysqlType);
                }
            }),

    TS(
            "meta.ts",
            DataTypes.TIMESTAMP_WITH_LOCAL_TIME_ZONE(3).notNull(),
            new MetadataConverter() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object read(SourceRecord record) {
                    Struct messageStruct = (Struct) record.value();
                    return TimestampData.fromEpochMillis(
                            (Long) messageStruct.get(FieldName.TIMESTAMP));
                }
            });

    private static StringData getCanalData(SourceRecord record, TableChange tableSchema, GenericRowData rowData) {
        // construct canal json
        Struct messageStruct = (Struct) record.value();
        Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
        // tableName
        String tableName = getMetaData(record, AbstractSourceInfo.TABLE_NAME_KEY);
        // databaseName
        String databaseName = getMetaData(record, AbstractSourceInfo.DATABASE_NAME_KEY);
        // schemaName
        String schemaName = getMetaData(record, AbstractSourceInfo.SCHEMA_NAME_KEY);
        // opTs
        long opTs = (Long) sourceStruct.get(AbstractSourceInfo.TIMESTAMP_KEY);
        // ts
        long ts = (Long) messageStruct.get(FieldName.TIMESTAMP);
        // actual data
        GenericRowData data = rowData;
        Map<String, Object> field = (Map<String, Object>) data.getField(0);
        List<Map<String, Object>> dataList = new ArrayList<>();
        dataList.add(field);

        CanalJson canalJson = CanalJson.builder()
                .data(dataList).database(databaseName).schema(schemaName)
                .sql("").es(opTs).isDdl(false).pkNames(getPkNames(tableSchema))
                .oracleType(getOracleType(tableSchema))
                .table(tableName).ts(ts)
                .type(getOpType(record)).sqlType(getSqlType(tableSchema)).build();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return StringData.fromString(objectMapper.writeValueAsString(canalJson));
        } catch (Exception e) {
            throw new IllegalStateException("exception occurs when get meta data", e);
        }
    }

    private final String key;

    private final DataType dataType;

    private final MetadataConverter converter;

    OracleReadableMetaData(String key, DataType dataType, MetadataConverter converter) {
        this.key = key;
        this.dataType = dataType;
        this.converter = converter;
    }

    private static String getOpType(SourceRecord record) {
        String opType;
        final Envelope.Operation op = Envelope.operationFor(record);
        if (op == Envelope.Operation.CREATE || op == Envelope.Operation.READ) {
            opType = "INSERT";
        } else if (op == Envelope.Operation.DELETE) {
            opType = "DELETE";
        } else {
            opType = "UPDATE";
        }
        return opType;
    }

    private static List<String> getPkNames(@Nullable TableChanges.TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        return tableSchema.getTable().primaryKeyColumnNames();
    }

    public static Map<String, String> getOracleType(@Nullable TableChanges.TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        Map<String, String> oracleType = new LinkedHashMap<>();
        final Table table = tableSchema.getTable();
        table.columns()
                .forEach(
                        column -> {
                            oracleType.put(
                                    column.name(),
                                    String.format(
                                            "%s(%d)",
                                            column.typeName(),
                                            column.length()));
                        });
        return oracleType;
    }

    public static Map<String, Integer> getSqlType(@Nullable TableChanges.TableChange tableSchema) {
        if (tableSchema == null) {
            return null;
        }
        Map<String, Integer> mysqlType = new LinkedHashMap<>();
        final Table table = tableSchema.getTable();
        table.columns()
                .forEach(
                        column -> {
                            mysqlType.put(
                                    column.name(),
                                    column.jdbcType());
                        });
        return mysqlType;
    }

    private static String getMetaData(SourceRecord record, String tableNameKey) {
        Struct messageStruct = (Struct) record.value();
        Struct sourceStruct = messageStruct.getStruct(FieldName.SOURCE);
        return sourceStruct.getString(tableNameKey);
    }

    public String getKey() {
        return key;
    }

    public DataType getDataType() {
        return dataType;
    }

    public MetadataConverter getConverter() {
        return converter;
    }

}