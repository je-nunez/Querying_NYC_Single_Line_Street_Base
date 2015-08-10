#!/usr/bin/env python

"""Plot a Shapefile in Basemap/matplotlib.

Note that this script uses Fiona and Shapely,
and that the original New York City's Shapefile
has more than 250'000 features, so this script,
___as it is in this unoptimized version___,
doesn't scale quite well to so many features.
But for smaller shapefiles, ___as it is in
this unoptimized version right now___, it can
scale.

This script will change to make it faster.
"""

# pylint: disable=no-name-in-module
# pylint: disable=invalid-name

import matplotlib.pyplot as plt
from mpl_toolkits.basemap import Basemap
import fiona
from shapely.geometry import shape
from shapely.ops import cascaded_union
import sys


layers = []
with fiona.drivers():

    with fiona.open(sys.argv[1] + '.shp', 'r') as source:
        for layer in source:
            # layer = source.next()
            # print layer
            layers.append(shape(layer['geometry']))

print "Calculating the bounds of the cascaded_union of the list of layers"
envel_bounds = list(cascaded_union(layers).bounds)

print "Original enveloping bounds: ", envel_bounds

# Expand a little the geometrical bounds given by shapely

delta_long = abs(envel_bounds[0] - envel_bounds[2])
delta_latd = abs(envel_bounds[1] - envel_bounds[3])
envel_bounds[0] -= delta_long / 100.0
envel_bounds[2] += delta_long / 100.0

envel_bounds[1] -= delta_latd / 100.0
envel_bounds[3] += delta_latd / 100.0

print "Expanded enveloping bounds: ", envel_bounds

LOW_LEFT_CORNR_LONGITUDE = envel_bounds[0]
LOW_LEFT_CORNER_LATITUDE = envel_bounds[1]
UP_RIGHT_CORNER_LONGITUDE = envel_bounds[2]
UP_RIGHT_CORNER_LATITUDE = envel_bounds[3]

MIN_NYC_ISLAND_TO_VISUALIZ = 0.0006

# Create the Basemap

m = Basemap(llcrnrlon=LOW_LEFT_CORNR_LONGITUDE,
            llcrnrlat=LOW_LEFT_CORNER_LATITUDE,
            urcrnrlon=UP_RIGHT_CORNER_LONGITUDE,
            urcrnrlat=UP_RIGHT_CORNER_LATITUDE,
            ellps='WGS84',
            resolution='f',
            area_thresh=MIN_NYC_ISLAND_TO_VISUALIZ)

m.drawcoastlines()
m.fillcontinents(color='green')
m.drawcountries(linewidth=3)
m.drawstates()
m.drawrivers()

m.drawmapboundary(fill_color='blue')

# Plot the shapefile in a basemap/matplotlib
s = m.readshapefile(shapefile=sys.argv[1],
                    name='segments', color='blue', linewidth=0.4)

# plt.legend()
mng = plt.get_current_fig_manager()
mng.full_screen_toggle()
plt.title('Plotting the Shapefile ' + sys.argv[1] + ' in basemap/matplotlib')
plt.show()

