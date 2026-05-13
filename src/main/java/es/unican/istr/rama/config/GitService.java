package es.unican.istr.rama.config;

import es.unican.istr.rama.comparison.ModelComparisonInput;

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
     * Posts or updates a code review request comment containing SVG report renders.
     *
     * @param pullRequestNumber the pull/merge request number to comment on
     * @param reports the PlantUML reports to publish as rendered SVG images
     * @throws IOException if there is an error communicating with the backing Git provider API
     */
    void postRenderedSvgReport(int pullRequestNumber, List<PlantUmlReport> reports) throws IOException;

    /**
     * A record representing a PlantUML report for a model file.
     *
     * @param filename the name of the model file
     * @param plantuml the PlantUML source to encode as an SVG image URL
     */
    record PlantUmlReport(String filename, String plantuml) {
    }

}
