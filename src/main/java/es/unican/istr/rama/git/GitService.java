package es.unican.istr.rama.git;

import es.unican.istr.rama.comparison.ModelComparisonInput;
import es.unican.istr.rama.render.ReportComment;

import java.io.IOException;
import java.util.List;

public interface GitService {

    /**
     * Fetches the model/metamodel files affected in the specified code review request and returns the
     * contents of the source, target, and merge-base versions.
     *
     * @param pullRequestNumber the number of the pull/merge request to analyze
     * @return a list of model comparison inputs representing the model files in a pull/merge request
     * @throws IOException if there is an error communicating with the backing Git provider API
     */
    List<ModelComparisonInput> getModelFiles(int pullRequestNumber) throws IOException;

    /**
     * Creates or updates the provider comment used for a RAMA report.
     *
     * @param pullRequestNumber the pull/merge request number to comment on
     * @param comment the rendered report comment to publish
     * @throws IOException if there is an error communicating with the backing Git provider API
     */
    void publishComment(int pullRequestNumber, ReportComment comment) throws IOException;

}
