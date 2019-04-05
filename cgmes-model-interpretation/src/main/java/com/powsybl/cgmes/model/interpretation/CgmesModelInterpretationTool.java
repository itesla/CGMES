/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cgmes.model.interpretation;

import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.google.auto.service.AutoService;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;

/**
 * @author José Antonio Marqués <marquesja at aia.es>
 * @author Marcos de Miguel <demiguelm at aia.es>
 */
@AutoService(Tool.class)
public class CgmesModelInterpretationTool implements Tool {

    private static final String INPUT_PATH    = "input-path";
    private static final String INPUT_PATTERN = "input-pattern";
    private static final String OUTPUT_PATH   = "output-path";

    @Override
    public Command getCommand() {
        return new Command() {

            @Override
            public String getDescription() {
                return "Inferes interpretations of CGMES models found in a catalog";
            }

            @Override
            public String getName() {
                return "cgmes-model-interpretation";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(INPUT_PATH).desc("the input path").hasArg()
                        .argName("INPUT_PATH").required().build());
                options.addOption(Option.builder().longOpt(INPUT_PATTERN).desc("pattern to the input files").hasArg()
                        .argName("INPUT_PATTERN").required().build());
                options.addOption(Option.builder().longOpt(OUTPUT_PATH).desc("the output path").hasArg()
                        .argName("OUTPUT_PATH").required().build());
                return options;
            }

            @Override
            public String getTheme() {
                return "Data conversion";
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        String inputPath = line.getOptionValue(INPUT_PATH);
        String inputPattern = line.getOptionValue(INPUT_PATTERN);
        String outputPath = line.getOptionValue(OUTPUT_PATH);

        CgmesModelsInterpretation cgmesFlowValidation = new CgmesModelsInterpretation(inputPath);
        new InterpretationsReport(Paths.get(outputPath)).report(cgmesFlowValidation.reviewAll(inputPattern));
    }

}
