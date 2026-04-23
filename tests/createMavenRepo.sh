#!/bin/bash
# build-maven-repo.sh
# Creates a local Maven repository structure from JARs in the "artifacts" folder

set -e

ARTIFACTS_DIR="${1:-artifacts}"
REPO_DIR="${2:-maven-local-repo}"

if [ ! -d "$ARTIFACTS_DIR" ]; then
    echo "ERROR: Artifacts directory '$ARTIFACTS_DIR' not found"
    exit 1
fi

mkdir -p "$REPO_DIR"

find "$ARTIFACTS_DIR" -name "*.jar" | while read jar; do
    echo "Processing: $jar"

    # Extract pom.properties from inside the JAR
    pom_props=$(unzip -p "$jar" 'META-INF/maven/*/*/pom.properties' 2>/dev/null || true)

    if [ -z "$pom_props" ]; then
        echo "  WARNING: No pom.properties found in $jar, skipping"
        continue
    fi

    groupId=$(echo "$pom_props" | grep '^groupId=' | cut -d'=' -f2 | tr -d '\r')
    artifactId=$(echo "$pom_props" | grep '^artifactId=' | cut -d'=' -f2 | tr -d '\r')
    version=$(echo "$pom_props" | grep '^version=' | cut -d'=' -f2 | tr -d '\r')

    if [ -z "$groupId" ] || [ -z "$artifactId" ] || [ -z "$version" ]; then
        echo "  WARNING: Incomplete Maven coordinates in $jar, skipping"
        continue
    fi

    echo "  Coordinates: $groupId:$artifactId:$version"

    # Build target directory path (groupId dots to slashes)
    group_path=$(echo "$groupId" | tr '.' '/')
    target_dir="$REPO_DIR/$group_path/$artifactId/$version"
    mkdir -p "$target_dir"

    # Copy JAR
    cp "$jar" "$target_dir/$artifactId-$version.jar"

    # Extract embedded POM if available
    pom_content=$(unzip -p "$jar" 'META-INF/maven/*/*/pom.xml' 2>/dev/null || true)
    if [ -n "$pom_content" ]; then
        echo "$pom_content" > "$target_dir/$artifactId-$version.pom"
    else
        # Generate a minimal POM
        cat > "$target_dir/$artifactId-$version.pom" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>$groupId</groupId>
  <artifactId>$artifactId</artifactId>
  <version>$version</version>
  <packaging>jar</packaging>
</project>
EOF
    fi

    # Generate Maven metadata
    cat > "$target_dir/../maven-metadata-local.xml" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>$groupId</groupId>
  <artifactId>$artifactId</artifactId>
  <versioning>
    <release>$version</release>
    <versions>
      <version>$version</version>
    </versions>
    <lastUpdated>$(date +%Y%m%d%H%M%S)</lastUpdated>
  </versioning>
</metadata>
EOF

    echo "  Installed to: $target_dir"
done

echo ""
echo "Maven repository built at: $REPO_DIR"
chmod -R 777 $REPO_DIR
