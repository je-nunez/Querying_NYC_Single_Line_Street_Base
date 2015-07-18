#!/usr/bin/env bash

# We could have done this ETL on the GeoDB send its results
# PostGIS in PostgreSQL, using the option
#
#    ogr2ogr -f "PostgreSQL" ...
#
# in the ETL below. We simplify for now in obtaining a
# 'Shapefile' result db.

# This is the SQL select statement that we use to do the ETL
# (Extract-Transform-Load) on the New York City Single Line
# Street Base Map. The documentation of the fields of this
# input database are here:
#
#    http://www.nyc.gov/html/dcp/pdf/bytes/lion_metadata.pdf?v=15b
#
# One of the task of this ETL on this NYC geodb is to select
# properly this SQL select statement. E.g., as it is now, this
# SQL below for this ETL generates over 100 MB of results.
#
# Note that the geographical coordinates (XFrom, YFrom, XTo, YTo,
# ArcCenterX, ArcCenterY) are included in this SQL query, as well
# as other non-geographical attributes, as the 'street' name.

ETL_SQL_select_transform="select segmentid, segmenttyp, segcount, XFrom, YFrom, XTo, YTo, ArcCenterX, ArcCenterY, street from lion"
#
# This SQL ETL below, which includes the field 'streetwidth', happens to
# fail because the resulting dBase IV file in the ouput 'Shapefile' has
# an invalid value in one of the more than a quater records, so we can't
# use it. (Of couse, the 'streetwidth' field would be very useful for the
# Data Mining because the both the Speed and the Volume Counts of Traffic
# depend on the 'streetwidth'.)
#
# SQL_select_transform="select segmentid, segmenttyp, segcount, XFrom, YFrom, XTo, YTo, ArcCenterX, ArcCenterY, streetwidth, street from lion"

# Abort this script on errors
set -e

# Download the New York City LION Single Line Street Base Map before ETL. Its
# documentation is here:
#
#    http://www.nyc.gov/html/dcp/html/bytes/dwnlion.shtml

echo "Downloading the NYC input database to ./nyc_src_db/in.zip ..."
if [[ ! -d nyc_src_db ]]; then
    mkdir nyc_src_db
fi

wget -O nyc_src_db/in.zip \
     --quiet \
     'http://www.nyc.gov/html/dcp/download/bytes/nyc_lion15b.zip'

echo "Unzipping the NYC input File GeoDataBase ..."
cd nyc_src_db
unzip -qq  in.zip   # -qq: ultra quiet mode
cd ..

echo "Preparing ETL destination dir with the shapefile: ./etl_dest_shp/ ..."

if [[ ! -d etl_dest_shp ]]; then
    mkdir etl_dest_shp
fi
cd etl_dest_shp
if [[ -f nyc_data_exploration.shp ]]; then
    rm nyc_data_exploration*
fi

echo "Running the ETL properly using the SQL: '$ETL_SQL_select_transform'"

# ogr2ogr belongs to the 'gdal' yum package (Fedora) and 'gdal-bin' (Debian)
# Mac users:  brew install gdal
#
ogr2ogr nyc_data_exploration.shp ../nyc_src_db/lion.gdb/ \
        -sql "$ETL_SQL_select_transform"

# The '.shp' contains the geographical attributes
echo "Verifying if the resulting GIS shapefile is valid..."
# ogrinfo is in the same package in Fedora, Debian, and Mac OS/X as above
ogrinfo  nyc_data_exploration.shp

# The dBase DBF contains the non-geographical attributes
echo "Verifying if resulting dBase DBF file in shapefile has valid values... "
../view_dbf.py  nyc_data_exploration.dbf >/dev/null

lat=40.785091
long=-73.968285

echo "ETL finished OK."

