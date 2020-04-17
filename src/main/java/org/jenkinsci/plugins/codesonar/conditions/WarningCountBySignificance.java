package org.jenkinsci.plugins.codesonar.conditions;

import hudson.AbortException;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.codesonar.CodeSonarBuildAction;
import org.jenkinsci.plugins.codesonar.models.CodeSonarBuildActionDTO;
import org.jenkinsci.plugins.codesonar.models.analysis.Analysis;
import org.jenkinsci.plugins.codesonar.models.analysis.Warning;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Simple condition to check the number of warnings of a given significance.
 * CodeSonar lists five categories for the warnings.
 *
 * author: Mads Jensen <mads.jensen@eficode.com>
 */
public class WarningCountBySignificance extends Condition {

    private static final String NAME = "Warning count significance: specified category";
    private String significance;
    private int warningCountThreshold;
    private String warrantedResult = Result.UNSTABLE.toString();

    @DataBoundConstructor
    public WarningCountBySignificance(@Nonnull String significance, int warningCountThreshold) {
        this.significance = significance;
        this.warningCountThreshold = warningCountThreshold;
    }

    @Override
    public Result validate(Run<?, ?> run, Launcher launcher, TaskListener listener) throws AbortException {
        CodeSonarBuildAction buildAction = run.getAction(CodeSonarBuildAction.class);
        if (buildAction == null) {
            return Result.SUCCESS;
        }

        CodeSonarBuildActionDTO buildActionDTO = buildAction.getBuildActionDTO();
        if (buildActionDTO == null) {
            return Result.SUCCESS;
        }

        Analysis analysis = buildActionDTO.getAnalysisActiveWarnings();

        int noOfWarnings = 0;
        List<Warning> warnings = analysis.getWarnings();
        for (Warning warning : warnings) {
            if (warning.getSignificance().equals(this.significance)) {
                noOfWarnings++;
            }
        }

        if (noOfWarnings> warningCountThreshold) {
            return Result.fromString(warrantedResult);
        }

        return Result.SUCCESS;
    }

    public String getSignificance() {
        return significance;
    }

    public void setSignificance(@Nonnull String significance) {
        this.significance = significance;
    }

    public int getWarningCountThreshold() {
        return warningCountThreshold;
    }

    public void setWarningCountThreshold(int warningCountThreshold) {
        this.warningCountThreshold = warningCountThreshold;
    }

    public String getWarrantedResult() {
        return warrantedResult;
    }

    @DataBoundSetter
    public void setWarrantedResult(String warrantedResult){
        this.warrantedResult = warrantedResult;
    }

    @Symbol("warningCountBySignificance")
    @Extension
    public static final class DescriptorImpl extends ConditionDescriptor<WarningCountBySignificance> {

        public DescriptorImpl() {
            load();
        }

        @Override
        public @Nonnull String getDisplayName() {
            return NAME;
        }

        public FormValidation doCheckWarningCountThreshold(
            @QueryParameter("warningCountThreshold") int warningCountThreshold
        ) {
            if (warningCountThreshold < 0) {
                return FormValidation.error("The provided value must be zero or greater");
            }

            return FormValidation.ok();
        }
    }
}
