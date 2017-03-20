
import macroutils.*;
import star.common.*;
import star.meshing.AutoMeshOperation;
import star.meshing.*;
import star.prismmesher.*;
import star.vis.*;

public class Run extends StarMacro {

    public void execute() {
        initMacro();
        if (!mu.check.has.volumeMesh()) {
            mu.update.volumeMesh();
        }
        Simulation simulation_0
                = getActiveSimulation();

        AutoMeshOperation autoMeshOperation_1
                = ((AutoMeshOperation) simulation_0.get(MeshOperationManager.class).getObject("Mesh_Stationary"));

        PartsMinimumSurfaceSize partsMinimumSurfaceSize_1
                = autoMeshOperation_1.getDefaultValues().get(PartsMinimumSurfaceSize.class);

        GenericRelativeSize genericRelativeSize_1
                = ((GenericRelativeSize) partsMinimumSurfaceSize_1.getRelativeSize());

        genericRelativeSize_1.setPercentage(50.0);

        SurfaceCustomMeshControl surfaceCustomMeshControl
                = ((SurfaceCustomMeshControl) autoMeshOperation_1.getCustomMeshControls().getObject("le_refine"));

        surfaceCustomMeshControl.setEnableControl(false);

        AutoMeshOperation autoMeshOperation_0
                = ((AutoMeshOperation) simulation_0.get(MeshOperationManager.class).getObject("Mesh_Rotating"));

        SurfaceCustomMeshControl surfaceCustomMeshControl_0
                = ((SurfaceCustomMeshControl) autoMeshOperation_0.getCustomMeshControls().getObject("Blade_Edges"));

        PartsTargetSurfaceSize partsTargetSurfaceSize_0
                = surfaceCustomMeshControl_0.getCustomValues().get(PartsTargetSurfaceSize.class);

        GenericRelativeSize genericRelativeSize_3
                = ((GenericRelativeSize) partsTargetSurfaceSize_0.getRelativeSize());

        genericRelativeSize_3.setPercentage(25.0);

        PartsMinimumSurfaceSize partsMinimumSurfaceSize_0
                = surfaceCustomMeshControl_0.getCustomValues().get(PartsMinimumSurfaceSize.class);

        GenericRelativeSize genericRelativeSize_0
                = ((GenericRelativeSize) partsMinimumSurfaceSize_0.getRelativeSize());

        genericRelativeSize_0.setPercentage(25.0);

        SurfaceCustomMeshControl surfaceCustomMeshControl_2
                = ((SurfaceCustomMeshControl) autoMeshOperation_0.getCustomMeshControls().getObject("Blades"));

        PartsTargetSurfaceSize partsTargetSurfaceSize_1
                = surfaceCustomMeshControl_2.getCustomValues().get(PartsTargetSurfaceSize.class);

        GenericRelativeSize genericRelativeSize_4
                = ((GenericRelativeSize) partsTargetSurfaceSize_1.getRelativeSize());

        genericRelativeSize_4.setPercentage(50.0);

        PartsMinimumSurfaceSize partsMinimumSurfaceSize_2
                = surfaceCustomMeshControl_2.getCustomValues().get(PartsMinimumSurfaceSize.class);

        GenericRelativeSize genericRelativeSize_2
                = ((GenericRelativeSize) partsMinimumSurfaceSize_2.getRelativeSize());

        genericRelativeSize_2.setPercentage(25.0);

        mu.update.volumeMesh();
        for (Displayer d : mu.get.scenes.allDisplayers(vo)) {
            d.setRepresentation(mu.get.mesh.fvr());
        }
        mu.clear.solutionHistory();
        mu.step(360);

        ud.simTitle = ud.simTitle + "_coarseGcLE";

        mu.io.write.picture(mu.get.plots.byREGEX("Prop", vo),
                ud.simTitle, ud.picResX, ud.picResY, vo);

        String fileName = ud.simPath + "/" + ud.simTitle;
        MonitorPlot propPlot = (MonitorPlot) mu.get.plots.byREGEX("Prop", vo);
        propPlot.export(fileName + "_prop.csv", ",");
        MonitorPlot gcPlot = (MonitorPlot) mu.get.plots.byREGEX("Gearcase", vo);
        gcPlot.export(fileName + "_gc.csv", ",");

        mu.saveSim();
    }

    void initMacro() {
        mu = new MacroUtils(getSimulation(), intrusive);
        ud = mu.userDeclarations;
    }

    MacroUtils mu;
    UserDeclarations ud;
    boolean vo = true;
    boolean intrusive = true;
}
