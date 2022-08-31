#!/bin/bash

echo "Downloading projects.zip file"

curl -o projects.zip https://zenodo.org/record/4479015/files/projects.zip?download=1


echo "Unzipping file"

unzip projects.zip


echo "Remove unused files"

find projects -type f -not -name "COMMIT" -exec rm {} \;