# Querying NYC Single Line Street Base

Querying the New York City Department of City Planning's Single Line Street
Base

# Description

This database is very helpful in an explorarion of the speed of the traffic,
the volume counts, etc. It is downloaded from the `BYTES of the Big APPLE`
web-site:

    http://www.nyc.gov/html/dcp/html/bytes/dwnlion.shtml

This is the explanation of the fields of the main table, and their
value-domains:

    http://www.nyc.gov/html/dcp/pdf/bytes/lion_metadata.pdf?v=15b

# Files

We need to do an `ETL` (Extraction-Transform-Load) phase on the geographical
database provided by the `BYTES of the Big APPLE` web-site, because it is in
the propietary `ESRI File Geodatabase` format, which need to be transformed
into the GIS `Shapefile` format. The GIS 'Shapefile' is composed by several
files, and one of the mandatory files is in dBase IV DBF format: here there
is an example of a 'Shapefile':

     http://mapserver.org/input/vector/shapefiles.html

Sometimes it is necessary to dump this dBase IV file to see what is inside.
This project includes the script `view_dbf.py`:

     view_dbf.py    a-dbf-filename.dbf

to verify the DBF file resulting from the ETL of the NYC LION FGDB file.

More details on the GIS 'Shapefile' format and its included DBF file is here:

     https://en.wikipedia.org/wiki/Shapefile

The ESRI File Geodatabase:

     https://en.wikipedia.org/wiki/ArcGIS#Geodatabase

This is a driver of GDAL (Geospatial Data Abstraction Library) to open ESRI
FGDB files:

     http://www.gdal.org/drv_filegdb.html

