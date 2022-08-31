#!/bin/bash

export M2_DIR=/Users/adann/projects_trust_updates_paper/.m2

export NUM_THREADS=4

JAR_EXEC=$(ls target/*SNAPSHOT.jar | head -1)

java -cp "target/lib/*:$JAR_EXEC" de.upb.thetis.Main -mp /Users/adann/projects_trust_updates_paper/projects -o /Users/adann/projects_trust_updates_paper/project_results