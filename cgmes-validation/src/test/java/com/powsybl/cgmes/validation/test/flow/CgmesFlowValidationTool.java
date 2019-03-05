package com.powsybl.cgmes.validation.test.flow;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.google.auto.service.AutoService;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;

@AutoService(Tool.class)
public class CgmesFlowValidationTool implements Tool {

    private static final String INPUT_PATH = "input-path";
    private static final String INPUT_PATTERN = "input-pattern";
    private static final String OUTPUT_PATH = "output-path";

    @Override
    public Command getCommand() {
        return new Command() {

            @Override
            public String getDescription() {
                return "validates all networks in a path";
            }

            @Override
            public String getName() {
                return "flow-validation";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(INPUT_PATH)
                        .desc("the input path")
                        .hasArg()
                        .argName("INPUT_PATH")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(INPUT_PATTERN)
                        .desc("pattern to the input files")
                        .hasArg()
                        .argName("INPUT_PATTERN")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_PATH)
                        .desc("the output path")
                        .hasArg()
                        .argName("OUTPUT_PATH")
                        .required()
                        .build());
                return options;
            }

            @Override
            public String getTheme() {
                return "Flows validation";
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

        CgmesFlowValidation cgmesFlowValidation = new CgmesFlowValidation(inputPath);
        cgmesFlowValidation.setReportPath(outputPath);
        cgmesFlowValidation.reviewAll(inputPattern);
    }

}
