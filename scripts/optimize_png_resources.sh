#!/bin/bash
find ../app/src/main/res/ -type d -exec sh -c 'cd "{}" ; optipng --strip all -o7 *.png ;' \;
