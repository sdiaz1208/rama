package es.unican.istr.rama.render;

/**
 * Renderable data for a model file where EMF Compare detected conflict groups.
 *
 * @param conflictCount number of EMF Compare conflict groups
 * @param realConflictCount number of real conflict groups
 * @param pseudoConflictCount number of pseudo conflict groups
 * @param leftPlantuml PlantUML for source/head changes against the merge base
 * @param rightPlantuml PlantUML for target/base-branch changes against the merge base
 */
public record ConflictReport(
        int conflictCount,
        int realConflictCount,
        int pseudoConflictCount,
        String leftPlantuml,
        String rightPlantuml
) {

    public boolean hasLeftPlantuml() {
        return leftPlantuml != null && !leftPlantuml.isBlank();
    }

    public boolean hasRightPlantuml() {
        return rightPlantuml != null && !rightPlantuml.isBlank();
    }
}
