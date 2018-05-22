#
# CGMES scripts
# Copyright (C) 2017 - 2018 RTE (http://rte-france.com)
# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.
#
# @author Luma Zamarreño <zamarrenolm at aia.es>
#

import sys
import os
import glob
import zipfile

def ensure_dir(directory):
    if not os.path.exists(directory):
        os.makedirs(directory)

CATALOG = "CasesCatalog"
TEST_CONVERSION = "ConversionTest"
TEST_LOADFLOW = "LoadFlowTest"
OUTPUT_FOLDER = 'unzipped'

def build_code_for_conversion_test_setup(java_test, tso_class):
    java_test.write("    @BeforeClass\n");
    java_test.write("    public static void setUp() {\n");
    java_test.write("        actuals = new %s%s();\n" % (tso_class, CATALOG));
    java_test.write("        tester = new ConversionTester(\n");
    java_test.write("                TripleStoreFactory.onlyDefaultImplementation(),\n");
    java_test.write("                new ComparisonConfig()\n");
    java_test.write("                        .checkNetworkId(false)\n");
    java_test.write("                        .checkVoltageLevelLimits(false)\n");
    java_test.write("                        .checkGeneratorReactiveCapabilityCurve(false)\n");
    java_test.write("                        .checkGeneratorRegulatingTerminal(false));\n");
    java_test.write("    }\n");

def build_code_for_conversion_test_imports(java_test):
    java_test.write("\n");
    java_test.write("import org.junit.BeforeClass;\n");
    java_test.write("import org.junit.Test;\n");
    java_test.write("\n");
    java_test.write("import com.powsybl.cgmes.CgmesModelException;\n");
    java_test.write("import com.powsybl.cgmes.conversion.test.ConversionTester;\n");
    java_test.write("import com.powsybl.cgmes.conversion.test.network.compare.ComparisonConfig;\n");
    java_test.write("import com.powsybl.triplestore.TripleStoreFactory;\n");
    java_test.write("\n");

def build_code_for_loadflow_test_setup(java_test, tso_class):
    java_test.write("    @BeforeClass\n");
    java_test.write("    public static void setUp() {\n");
    java_test.write("        catalog = new %s%s();\n" % (tso_class, CATALOG));
    java_test.write("        tester = new LoadFlowTester(\n");
    java_test.write("                TripleStoreFactory.onlyDefaultImplementation(),\n");
    java_test.write("                new LoadFlowValidation.Builder()\n");
    java_test.write("                        .writeNetworksInputsResults(true)\n");
    java_test.write("                        .validateInitialState(true)\n");
    java_test.write("                        .compareWithInitialState(true)\n");
    java_test.write("                        .build());\n");
    java_test.write("    }\n");


def build_code_for_loadflow_test_imports(java_test):
    java_test.write("\n");
    java_test.write("import org.junit.BeforeClass;\n");
    java_test.write("import org.junit.Test;\n");
    java_test.write("\n");
    java_test.write("import com.powsybl.cgmes.CgmesModelException;\n");
    java_test.write("import com.powsybl.cgmes.conversion.test.LoadFlowTester;\n");
    java_test.write("import com.powsybl.cgmes.conversion.test.LoadFlowValidation;\n");
    java_test.write("import com.powsybl.triplestore.TripleStoreFactory;\n");
    java_test.write("\n");

def build_code_for_catalog_imports(java_catalog):                
    java_catalog.write("\n");
    java_catalog.write("import java.nio.file.Path;\n");
    java_catalog.write("import java.nio.file.Paths;\n");
    java_catalog.write("\n");
    java_catalog.write("import com.powsybl.cgmes.test.TestGridModel;\n");
    java_catalog.write("import com.powsybl.cgmes.triplestore.CgmesModelTripleStore;\n");
    java_catalog.write("\n");
                
def build_code_for_catalog(p, package, base, tso, ts):
    # Do not allow too many upper case letters in tso name used for building class names
    tso_class = tso.title()
    tso_method = tso.lower()
    java_catalog_fname = os.path.join(p, '%s%s.java' % (tso_class, CATALOG))
    with open(java_catalog_fname, 'w') as java_catalog:
        var_path_tso = tso.upper()
        java_catalog.write('package %s;\n' % package)
        build_code_for_catalog_imports(java_catalog)
        java_catalog.write('public class %s%s {\n' % (tso_class, CATALOG))
        for t in ts:
            java_catalog.write('    public TestGridModel %s%s() {\n' % (tso_method, t))
            java_catalog.write('        return new TestGridModel(%s.resolve("%s"), null, CgmesModelTripleStore.CIM_NAMESPACE_16, null, false, false);\n' % (var_path_tso, t))
            java_catalog.write('    }\n')
        java_catalog.write('    private static final Path %s = Paths.get("%s/%s");\n' % (var_path_tso, base, tso))
        java_catalog.write('}\n')

def build_code_for_conversion_test(p, package, tso, ts):
    tso_class = tso.title()
    tso_method = tso.lower()
    java_test_fname = os.path.join(p, '%s%s.java' % (tso_class, TEST_CONVERSION))
    with open(java_test_fname, 'w') as java_test:
        java_test.write('package %s;\n' % package)
        build_code_for_conversion_test_imports(java_test)
        java_test.write('public class %s%s {\n' % (tso_class, TEST_CONVERSION))
        build_code_for_conversion_test_setup(java_test, tso_class)
        for t in ts:
            java_test.write('    @Test\n')
            java_test.write('    public void %s%s()  {\n' % (tso_method, t))
            java_test.write('        tester.testConversion(null, actuals.%s%s());\n' % (tso_method, t))
            java_test.write('    }\n')
        java_test.write('    private static %s%s  actuals;\n' % (tso_class, CATALOG))
        java_test.write('    private static ConversionTester tester;\n')
        java_test.write('}\n')
                        
def build_code_for_loadflow_test(p, package, tso, ts):
    tso_class = tso.title()
    tso_method = tso.lower()
    java_test_fname = os.path.join(p, '%s%s.java' % (tso_class, TEST_LOADFLOW))
    with open(java_test_fname, 'w') as java_test:
        java_test.write('package %s;\n' % package)
        build_code_for_loadflow_test_imports(java_test)
        java_test.write('public class %s%s {\n' % (tso_class, TEST_LOADFLOW))
        build_code_for_loadflow_test_setup(java_test, tso_class)
        for t in ts:
            java_test.write('    @Test\n')
            java_test.write('    public void %s%s()  {\n' % (tso_method, t))
            java_test.write('        tester.testLoadFlow(catalog.%s%s());\n' % (tso_method, t))
            java_test.write('    }\n')
        java_test.write('    private static %s%s catalog;\n' % (tso_class, CATALOG))
        java_test.write('    private static LoadFlowTester tester;\n')
        java_test.write('}\n')
                        
def build_code_for_tests_cases(p, package, base, tso, ts):
    p = os.path.join(p, 'temp-code')
    ensure_dir(p)
    build_code_for_catalog(p, package, base, tso, ts)
    build_code_for_conversion_test(p, package, tso, ts)
    build_code_for_loadflow_test(p, package, tso, ts)

def tso_from_main_zip_filename(filename):
    return filename.replace('.zip', '').split('_')[3]

def datehour_from_filename(filename):
    return filename.split('_')[0]

def is_boundary_filename(filename):
    return 'ENTSO-E_BD' in filename
    
def date_hour_valid_filename(filename):
    return filename.lower().endswith('.zip') and not is_boundary_filename(filename)

def process_tso_files(ptso):
    for f in glob.glob('%s/*' % ptso):
        fname = os.path.basename(f)
        print '        file %s' % fname
        if date_hour_valid_filename(fname):
            # Extract all files in tso/dh folder
            fzip = zipfile.ZipFile(f, 'r')
            pdh = os.path.join(ptso, datehour_from_filename(fname))
            fzip.extractall(pdh)
            fzip.close()
            os.remove(f)
        elif is_boundary_filename(fname):
            # Extract boundary in tso folder
            fzip = zipfile.ZipFile(f, 'r')
            fzip.extractall(ptso)
            fzip.close()
            os.remove(f)
        
def process_tso_main_zip_file(p, main_zip_file, tso, extract):
    ptso = os.path.join(p, OUTPUT_FOLDER, tso)
    print '    mkdir [%s]' % ptso
    ensure_dir(ptso)
    print '    main zip file [%s]' % os.path.basename(main_zip_file)
    mzip = zipfile.ZipFile(main_zip_file, 'r')
    tso_files = mzip.namelist()
    # Prepare a list of test cases
    test_cases = [] 
    # Ensure all folders for date/hour exist
    dhs = set([datehour_from_filename(f) for f in tso_files if date_hour_valid_filename(f)])
    print '    DH count %s : %d' % (tso, len(dhs))
    for dh in dhs:
        print '        DH mkdir %s' % dh
        ensure_dir(os.path.join(ptso, dh))
        test_cases.append(dh)
    if extract:
        # Extract all files in main zip file to tso folder and process them
        mzip.extractall(ptso)
        process_tso_files(ptso)
    mzip.close()
    return test_cases

def process_main_tso_zip_files(p, package, base_location_for_data_in_code, extract):
    for f in glob.glob('%s/*.zip' % p):
        fname = os.path.basename(f)
        tso = tso_from_main_zip_filename(fname)
        print 'TSO [%-12s]: %s' % (tso, fname)
        test_cases = process_tso_main_zip_file(p, f, tso, extract)
        build_code_for_tests_cases(p, package, base_location_for_data_in_code, tso, test_cases)
        
if __name__ == '__main__':
    dataset_location = sys.argv[1]
    package = sys.argv[2]
    base_location_for_data_in_code = os.path.join(sys.argv[3], OUTPUT_FOLDER)
    extract = sys.argv[4].lower() in ("yes", "true", "t", "1")
    print 'dataset_location               : %s' % dataset_location
    print 'package                        : %s' % package
    print 'base_location_for_data_in_code : %s' % base_location_for_data_in_code
    print 'extract                        : %s' % extract
    process_main_tso_zip_files(dataset_location, package, base_location_for_data_in_code, extract)
