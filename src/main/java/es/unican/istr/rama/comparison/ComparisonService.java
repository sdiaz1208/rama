package es.unican.istr.rama.comparison;

import org.eclipse.emf.compare.Comparison;

import java.io.IOException;

public interface ComparisonService {

    /**
     * Compares the source, target, and base contents of a model file.
     *
     * @param input the input containing the filename and contents for source, target, and base
     * @return the comparison result
     * @throws IOException if an I/O error occurs while loading comparison resources
     */
    Comparison compare(ModelComparisonInput input) throws IOException;

}
