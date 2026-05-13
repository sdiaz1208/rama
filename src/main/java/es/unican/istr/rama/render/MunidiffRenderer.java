package es.unican.istr.rama.render;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.emf.compare.Comparison;
import org.eclipse.epsilon.modiff.emfcompare.munidiff.transformations.EmfCompare2Munidiff;
import org.eclipse.epsilon.modiff.matcher.EcoreMatcher;
import org.eclipse.epsilon.modiff.matcher.IdOrNameMatcher;
import org.eclipse.epsilon.modiff.matcher.Matcher;
import org.eclipse.epsilon.modiff.munidiff.Munidiff;
import org.eclipse.epsilon.modiff.output.LabelProvider;
import org.eclipse.epsilon.modiff.output.MatcherBasedLabelProvider;
import org.eclipse.epsilon.modiff.output.graphical.PlantumlEcoreFormatter;
import org.eclipse.epsilon.modiff.output.graphical.PlantumlFormatter;
import org.eclipse.epsilon.modiff.output.textual.UnifiedDiffFormatter;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

public class MunidiffRenderer {

    /**
     * Renders a Comparison into a RenderedMunidiff, which includes unified diff, PlantUML, and SVG representations.
     *
     * @param comparison the EMF Comparison to be rendered
     * @param ecoreDiff whether the comparison involves Ecore or other metamodels
     * @return a RenderedMunidiff containing the unified diff, PlantUML, and SVG representations
     */
    public RenderedMunidiff render(Comparison comparison, boolean ecoreDiff) {
        Matcher matcher = ecoreDiff ? new EcoreMatcher() : new IdOrNameMatcher();
        LabelProvider labels = new MatcherBasedLabelProvider(matcher);

        Munidiff munidiff = new EmfCompare2Munidiff(matcher).transform(comparison);
        String unifiedDiff = new UnifiedDiffFormatter(munidiff, labels).format();
        String plantuml = ecoreDiff
                ? new PlantumlEcoreFormatter(munidiff, labels).format()
                : new PlantumlFormatter(munidiff, labels).format();

        return new RenderedMunidiff(
                munidiff,
                unifiedDiff,
                plantuml,
                renderPlantumlToSvg(plantuml)
        );
    }

    /**
     * Renders PlantUML content to SVG format using the PlantUML library.
     *
     * @param plantuml the PlantUML content to be rendered
     * @return the rendered SVG content as a string
     */
    private String renderPlantumlToSvg(String plantuml) {
        try {
            SourceStringReader reader = new SourceStringReader(plantuml);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            reader.outputImage(output, new FileFormatOption(FileFormat.SVG));
            return output.toString(StandardCharsets.UTF_8);
        }
        catch (Exception ex) {
            throw new IllegalStateException("Could not render Munidiff PlantUML to SVG", ex);
        }
    }
}
