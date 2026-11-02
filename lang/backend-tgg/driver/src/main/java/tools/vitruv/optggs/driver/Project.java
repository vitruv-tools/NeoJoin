package tools.vitruv.optggs.driver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class Project {
    private final String name;
    private final List<Metamodel> sourceMetamodels = new ArrayList<>();
    private final List<Metamodel> targetMetamodels = new ArrayList<>();
    private final List<Model> sourceModels = new ArrayList<>();
    private final List<Model> targetModels = new ArrayList<>();
    private final Collection<ConstraintSolver> constraintSolvers = new ArrayList<>();

    public Project(String name) {
        this.name = name;
        this.constraintSolvers.addAll(ConstraintSolver.defaultSolvers());
    }

    public String name() {
        return name;
    }

    public String packageName() {
        return name.toLowerCase();
    }

    public void addSourceMetamodel(Metamodel metamodel) {
        this.sourceMetamodels.add(metamodel);
    }

    public void addTargetMetamodel(Metamodel metamodel) {
        this.targetMetamodels.add(metamodel);
    }

    public void addSourceModel(Model model) {
        this.sourceModels.add(model);
    }

    public void addTargetModel(Model model) {
        this.targetModels.add(model);
    }

    public void addConstraintSolver(ConstraintSolver solver) {
        this.constraintSolvers.add(solver);
    }

    public Collection<Metamodel> sourceMetamodels() {
        return sourceMetamodels;
    }

    public Collection<Metamodel> targetMetamodels() {
        return targetMetamodels;
    }

    public Collection<Model> sourceModels() {
        return sourceModels;
    }

    public Collection<Model> targetModels() {
        return targetModels;
    }

    public Collection<ConstraintSolver> constraintSolvers() {
        return Collections.unmodifiableCollection(constraintSolvers);
    }
}
