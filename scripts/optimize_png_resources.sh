#!/bin/bash
find ../app/src/main/res/ -type d -exec sh -c 'cd "{}" ; optipng -o7 *.png ;' \;
