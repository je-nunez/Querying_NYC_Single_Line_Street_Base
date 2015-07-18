#!/usr/bin/env python

"""Dump a dBase IV DBF file, given as first argument in the command line.

The Geographical Information Systems (GIS) 'Shapefile' format includes a
file in dBase IV DBF format, and sometimes it is necessary to dump this
DBF file to see what is inside. More details on the GIS 'Shapefile' format
and its included DBF file is here:

     https://en.wikipedia.org/wiki/Shapefile
"""

import sys
from dbfpy import dbf

dbf_filename = sys.argv[1]

# open the DBF file read-only
dbf = dbf.Dbf(dbf_filename, readOnly=True, new=False)

# dbf.ignoreErrors = True

# print the definitions of the fields of the DBF
print dbf.fieldDefs

# print the names of the fields of the DDF
print dbf.fieldNames

# print the records of the DBF
i = 1
for record in dbf:
    print "%d %s\n", i, record
    i += 1
