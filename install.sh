#!/bin/bash

# Copyright (c) 2018, RTE (http://www.rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

sourceDir=$(dirname $(readlink -f $0))


## install default settings
###############################################################################
cgmes_prefix=$HOME/itesla_cgmes
cgmes_package_version=` mvn -f "$sourceDir/pom.xml" org.apache.maven.plugins:maven-help-plugin:evaluate -Dexpression=project.version | grep -v "Download" | grep -v "\["`
cgmes_package_name=cgmes-$cgmes_package_version
cgmes_package_type=zip

# Targets
cgmes_clean=false
cgmes_compile=false
cgmes_docs=false
cgmes_package=false
cgmes_install=false

# compile options
cgmes_skip_tests=false



## read settings from configuration file
###############################################################################
settings="$sourceDir/install.cfg"
if [ -f "${settings}" ]; then
     source "${settings}"
fi


## Usage/Help
###############################################################################
cmd=$0
usage() {
    echo "usage: $cmd [options] [target...]"
    echo ""
    echo "Available targets:"
    echo "  clean                    Clean CGMES modules"
    echo "  compile                  Compile CGMES modules"
    echo "  package                  Compile CGMES modules and create a distributable package"
    echo "  install                  Compile CGMES modules and install it (default target)"
    echo "  help                     Display this help"
    echo "  docs                     Generate the documentation (Doxygen/Javadoc)"
    echo ""
    echo "CGMES options:"
    echo "  --help                   Display this help"
    echo "  --prefix                 Set the installation directory (default is $HOME/itesla)"
    echo "  --package-type           Set the package format. The supported formats are zip, tar, tar.gz and tar.bz2 (default is zip)"
    echo "  --skip-tests             compile modules without testing"
    echo "  --with-tests             compile modules with testing (default)"
    echo ""
    echo ""
}


## Write Settings functions
###############################################################################
writeSetting() {
    if [[ $# -lt 2 || $# -gt 3 ]]; then
        echo "WARNING: writeSetting <setting> <value> [comment (true|false)]"
        exit 1
    fi

    SETTING=$1
    VALUE=$2
    if [[ $# -eq 3 ]]; then
        echo -ne "# "
    fi
    echo "${SETTING}=${VALUE}"

    return 0
}

writeComment() {
    echo "# $*"
    return 0
}

writeEmptyLine() {
    echo ""
    return 0
}

writeSettings() {
    writeComment " -- CGMES options --"
    writeSetting "cgmes_prefix" ${cgmes_prefix}
    writeSetting "cgmes_package_type" ${cgmes_package_type}

    writeEmptyLine

    writeComment " -- CGMES compile options --"
    writeSetting "cgmes_skip_tests" ${cgmes_skip_tests}

    return 0
}


## Build Java Modules
###############################################################################
cgmes_java()
{
    if [[ $cgmes_clean = true || $cgmes_compile = true || $cgmes_docs = true ]]; then
        echo "** Building CGMES modules"

        mvn_options=""
        [ $cgmes_clean = true ] && mvn_options="$mvn_options clean"
        [ $cgmes_compile = true ] && mvn_options="$mvn_options install"
        [ $cgmes_skip_tests = true ] && mvn_options="$mvn_options -DskipTests"
        if [ ! -z "$mvn_options" ]; then
            mvn -f "$sourceDir/pom.xml" $mvn_options || exit $?
        fi

        if [ $cgmes_docs = true ]; then
            echo "**** Generating Javadoc documentation"
            mvn -f "$sourceDir/pom.xml" javadoc:javadoc || exit $?
            mvn -f "$sourceDir/distribution-cgmes/pom.xml" install || exit $?
        fi
    fi
}

## Package CGMES
###############################################################################
cgmes_package()
{
    if [ $cgmes_package = true ]; then
        echo "** Packaging CGMES"

        case "$cgmes_package_type" in
            zip)
                [ -f "${cgmes_package_name}.zip" ] && rm -f "${cgmes_package_name}.zip"
                $(cd "$sourceDir/distribution-cgmes/target/powsybl-distribution-cgmes-${cgmes_package_version}-full" && zip -rq "$sourceDir/${cgmes_package_name}.zip" "powsybl")
                zip -qT "${cgmes_package_name}.zip" > /dev/null 2>&1 || exit $?
                ;;

            tar)
                [ -f "${cgmes_package_name}.tar" ] && rm -f "${cgmes_package_name}.tar"
                tar -cf "${cgmes_package_name}.tar" -C "$sourceDir/distribution-cgmes/target/powsybl-distribution-cgmes-${cgmes_package_version}-full" . || exit $?
                ;;

            tar.gz | tgz)
                [ -f "${cgmes_package_name}.tar.gz" ] && rm -f "${cgmes_package_name}.tar.gz"
                [ -f "${cgmes_package_name}.tgz" ] && rm -f "${cgmes_package_name}.tgz"
                tar -czf "${cgmes_package_name}.tar.gz" -C "$sourceDir/distribution-cgmes/target/powsybl-distribution-cgmes-${cgmes_package_version}-full" . || exit $?
                ;;

            tar.bz2 | tbz)
                [ -f "${cgmes_package_name}.tar.bz2" ] && rm -f "${cgmes_package_name}.tar.bz2"
                [ -f "${cgmes_package_name}.tbz" ] && rm -f "${cgmes_package_name}.tbz"
                tar -cjf "${cgmes_package_name}.tar.bz2" -C "$sourceDir/distribution-cgmes/target/powsybl-distribution-cgmes-${cgmes_package_version}-full" . || exit $?
                ;;

            *)
                echo "Invalid package format: zip, tar, tar.gz, tar.bz2 are supported."
                exit 1;
                ;;
        esac
    fi
}

## Install CGMES
###############################################################################
cgmes_install()
{
    if [ $cgmes_install = true ]; then
        echo "** Installing CGMES modules"

        echo "**** Copying files"
        mkdir -p "$cgmes_prefix" || exit $?
        cp -Rp "$sourceDir/distribution-cgmes/target/powsybl-distribution-cgmes-${cgmes_package_version}-full/powsybl"/* "$cgmes_prefix" || exit $?

    fi
}

## Parse command line
###############################################################################
cgmes_options="prefix:,package-type:,skip-tests,with-tests"

opts=`getopt -o '' --long "help,$cgmes_options" -n 'install.sh' -- "$@"`
eval set -- "$opts"
while true; do
    case "$1" in
        # CGMES options
        --prefix) cgmes_prefix=$2 ; shift 2 ;;
        --package-type) cgmes_package_type=$2 ; shift 2 ;;

        # compile options
        --skip-tests) cgmes_skip_tests=true ; shift ;;
        --with-tests) cgmes_skip_tests=false ; shift ;;

        # Help
        --help) usage ; exit 0 ;;

        --) shift ; break ;;
        *) usage ; exit 1 ;;
    esac
done

if [ $# -ne 0 ]; then
    for command in $*; do
        case "$command" in
            clean) cgmes_clean=true ;;
            compile) cgmes_compile=true ;;
            docs) cgmes_docs=true ;;
            package) cgmes_package=true ; cgmes_compile=true ;;
            install) cgmes_install=true ; cgmes_compile=true ;;
            help) usage; exit 0 ;;
            *) usage ; exit 1 ;;
        esac
    done
else
    cgmes_compile=true
    cgmes_install=true
fi

## Build CGMES
###############################################################################

# Build Java modules
cgmes_java

# Package CGMES modules
cgmes_package

# Install CGMES
cgmes_install

# Save settings
writeSettings > "${settings}"
