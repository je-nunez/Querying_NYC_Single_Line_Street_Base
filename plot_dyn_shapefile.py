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

import sys
import os
import matplotlib.pyplot as plt
from mpl_toolkits.basemap import Basemap
import fiona
from shapely.geometry import shape


def find_bounds_of_the_shapefile(shp_filename):
    """Find the bounds -or envelope- of a shapefile

    The 'shp_filename' parameter should NOT have a '.shp' extension: it is
    appended inside.

    The return value is a tuple (minx, miny, maxx, maxy) with the bounds
    -or envelope- of the shapefile
    """

    # initial values
    abs_minx = 100000000.0
    abs_miny = 100000000.0
    abs_maxx = -100000000.0
    abs_maxy = -100000000.0

    with fiona.drivers():

        with fiona.open(shp_filename + '.shp', 'r') as source:
            for layer in source:
                (minx, miny, maxx, maxy) = shape(layer['geometry']).bounds
                if minx < abs_minx:
                    abs_minx = minx
                if miny < abs_miny:
                    abs_miny = miny
                if maxx > abs_maxx:
                    abs_maxx = maxx
                if maxy > abs_maxy:
                    abs_maxy = maxy

    # expand a little the bounds (eg., by the delta/100)
    delta_long = abs(abs_maxx - abs_minx)
    delta_latd = abs(abs_maxy - abs_miny)
    abs_minx -= delta_long / 100.0
    abs_maxx += delta_long / 100.0

    abs_miny -= delta_latd / 100.0
    abs_maxy += delta_latd / 100.0

    return (abs_minx, abs_miny, abs_maxx, abs_maxy)


def draw_shapefile(shp_filename):
    """Draw a shapefile.

    The 'shp_filename' parameter should NOT have a '.shp' extension: it is
    appended inside.
    """

    (minx, miny, maxx, maxy) = find_bounds_of_the_shapefile(shp_filename)

    min_coastal_line_to_draw = 0.0006

    # Create the Basemap

    m = Basemap(llcrnrlon=minx, llcrnrlat=miny,
                urcrnrlon=maxx, urcrnrlat=maxy,
                ellps='WGS84',
                resolution='f',
                area_thresh=min_coastal_line_to_draw)

    m.drawcoastlines()
    m.fillcontinents(color='green')
    m.drawcountries(linewidth=3)
    m.drawstates()
    m.drawrivers()

    m.drawmapboundary(fill_color='blue')

    # Plot the shapefile in a basemap/matplotlib
    _ = m.readshapefile(shapefile=shp_filename,
                        name='segments', color='blue', linewidth=0.4)

    # plt.legend()
    mng = plt.get_current_fig_manager()
    mng.full_screen_toggle()
    plt.title('Plotting the Shapefile ' + shp_filename)
    plt.show()


def main(shp_filename):
    """Main function."""

    if not os.path.isfile(shp_filename):
        sys.stderr.write("File 'shp_filename' not found.\n")
        sys.exit(1)

    if shp_filename.endswith('.shp'):
        # remove the '.shp' extension
        shp_filename = shp_filename[:-4]

    # draw the shapefile
    draw_shapefile(shp_filename)


if __name__ == '__main__':
    main(sys.argv[1])
