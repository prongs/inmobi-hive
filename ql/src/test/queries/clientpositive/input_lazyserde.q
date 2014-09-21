CREATE TABLE dest1(a array<int>, b array<string>, c map<string,string>, d int, e string)
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '1'
COLLECTION ITEMS TERMINATED BY '2'
MAP KEYS TERMINATED BY '3'
LINES TERMINATED BY '10'
STORED AS TEXTFILE;

EXPLAIN
FROM src_thrift
INSERT OVERWRITE TABLE dest1 SELECT src_thrift.lint, src_thrift.lstring, src_thrift.mstringstring, src_thrift.aint, src_thrift.astring DISTRIBUTE BY 1;

FROM src_thrift
INSERT OVERWRITE TABLE dest1 SELECT src_thrift.lint, src_thrift.lstring, src_thrift.mstringstring, src_thrift.aint, src_thrift.astring DISTRIBUTE BY 1;

SELECT dest1.* FROM dest1 CLUSTER BY 1;

SELECT dest1.a[0], dest1.b[0], dest1.c['key2'], dest1.d, dest1.e FROM dest1 CLUSTER BY 1;

DROP TABLE dest1;

CREATE TABLE dest1(a array<int>) ROW FORMAT DELIMITED FIELDS TERMINATED BY '1' ESCAPED BY '\\';
INSERT OVERWRITE TABLE dest1 SELECT src_thrift.lint FROM src_thrift DISTRIBUTE BY 1;
SELECT * from dest1 ORDER BY 1 ASC;
DROP TABLE dest1;

CREATE TABLE dest1(a map<string,string>) ROW FORMAT DELIMITED FIELDS TERMINATED BY '1' ESCAPED BY '\\';
INSERT OVERWRITE TABLE dest1 SELECT src_thrift.mstringstring FROM src_thrift DISTRIBUTE BY 1;
SELECT * from dest1 ORDER BY 1 ASC;

CREATE TABLE destBin(a UNIONTYPE<int, double, array<string>, struct<col1:int,col2:string>>) ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.lazybinary.LazyBinarySerDe' STORED AS SEQUENCEFILE;
INSERT OVERWRITE TABLE destBin SELECT create_union( CASE WHEN key < 100 THEN 0 WHEN key < 200 THEN 1 WHEN key < 300 THEN 2 WHEN key < 400 THEN 3 ELSE 0 END, key, 2.0, array("one","two"), struct(5,"five")) FROM srcbucket2;
SELECT * from destBin ORDER BY a;
DROP TABLE destBin;
