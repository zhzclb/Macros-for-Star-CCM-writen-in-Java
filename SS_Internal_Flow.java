/**
 * Simple steady state internal flow sim with scenes and total pressure monitors
 * 
 * @author Andrew Gunderson
 * 
 * 2017, v11.06
 */
package macroutils;

import macroutils.*;
import macroutils.templates.*;
import star.common.*;
import star.vis.*;

public class SS_Internal_Flow extends StarMacro {

    String[] versions = {"v0", "v3", "v4", "v5"};
    
    String[] flowRates = {"_low", "_high"};
    
    Double[] mfrs = {0.389, 0.517};
    
    public void execute() {

        for (String version : versions) {
            for (String flowRate : flowRates) {
                if (mu.check.is.simFile(version + flowRate)){
                    break;
                }

                initMacro(version, flowRate);

                pre();

                solve();

                post();

                mu.saveSim();
                
            }
        }
    }

    void initMacro(String version, String flowRate) {
        sim = getActiveSimulation();
        mu = new MacroUtils(sim);
        ud = mu.userDeclarations;
        if (flowRate.contains("low")) {
            mfr = mfrs[0];
        } else {
            mfr = mfrs[1];
        }
        ud.simTitle = version + flowRate;
        mu.add.geometry.importPart(ud.simTitle + ".x_b");
    }

    void pre() {
        if (mu.check.has.volumeMesh()) {
            return;
        }
        ud.region = mu.add.region.fromAll(true);
        ud.geometryParts = mu.get.geometries.all(true);
        //--
        ud.mshBaseSize = .002;
        ud.prismsLayers = 4;
        ud.prismsRelSizeHeight = 33;
        ud.prismsStretching = 1.5;        
        ud.autoMshOp = mu.add.meshOperation.automatedMesh(ud.geometryParts,
                StaticDeclarations.Meshers.SURFACE_REMESHER,
                StaticDeclarations.Meshers.POLY_MESHER,
                StaticDeclarations.Meshers.PRISM_LAYER_MESHER);
        //--
        ud.physCont = mu.add.physicsContinua.generic(StaticDeclarations.Space.THREE_DIMENSIONAL,
                StaticDeclarations.Time.STEADY, StaticDeclarations.Material.LIQUID,
                StaticDeclarations.Solver.SEGREGATED, StaticDeclarations.Density.CONSTANT,
                StaticDeclarations.Energy.ISOTHERMAL, StaticDeclarations.Viscous.RKE_2LAYER);
        mu.enable.cellQualityRemediation(ud.physCont, true);
        ud.viscWater = 0.000854;
        ud.denWater = 1015;
        mu.set.physics.materialProperty(ud.physCont, "water", StaticDeclarations.Vars.VISC, ud.viscWater, ud.unit_Pa_s);
        //--
        mu.set.boundary.asMassFlowInlet(mu.get.boundaries.byREGEX(".*" + ud.bcInlet, true), mfr, 20.0, 0.05, 10.0);
        mu.set.boundary.asPressureOutlet(mu.get.boundaries.byREGEX(".*" + ud.bcOutlet, true), 0.0, 21.0, 0.05, 10.0);
        //--
        mu.update.volumeMesh();
        ud.scene = mu.add.scene.mesh();
    }

    void solve() {
        if (mu.check.has.solution()) {
            return;
        }
        
        ud.maxIter = 2000;
        //mu.set.solver.aggressiveSettings();
        mu.run();
        mu.saveSim();
    }

    void post() {
        ud.namedObjects.addAll(mu.get.boundaries.all(true));
        //--
        //-- One can make it all in a single Scenes.
        ud.ff = mu.get.objects.fieldFunction(StaticDeclarations.Vars.P.getVar(), true);
        ud.scene = mu.add.scene.scalar(ud.namedObjects, ud.ff, ud.unit_Pa, true);
        ud.scene.getAxes().setAxesVisible(false);

        scd = (ScalarDisplayer) mu.get.scenes.displayerByREGEX(ud.scene, "Scalar", true);
        scd.setOpacity(0);
        scd.getLegend().setVisible(false);

        pd1 = mu.add.scene.displayer_Geometry(ud.scene);
        pd1.setOpacity(1);
        pd1.setColorMode(PartColorMode.DEFAULT);

        pd2 = (PartDisplayer) mu.add.scene.displayer_Geometry(ud.scene);
        pd2.copyProperties(pd1);
        pd2.setMesh(true);
        pd2.setPresentationName("Mesh");
        pd2.setOpacity(0);

        ud.namedObjects2.add(mu.get.boundaries.byREGEX(".*" + ud.bcInlet, true));
        ud.namedObjects2.addAll(mu.get.regions.all(true));
        ud.postStreamlinesTubesWidth = 0.0005;
        std = mu.add.scene.displayer_Streamline(ud.scene, ud.namedObjects2, true);
        mu.templates.prettify.all();
        std.getScalarDisplayQuantity().setRange(new double[]{0, 3.0});
        std.getAnimationManager().setMode(StreamDisplayerAnimationMode.TRACER);
        std.setLegendPosition(scd.getLegend().getPositionCoordinate());
        std.setVisibilityOverrideMode(DisplayerVisibilityOverride.HIDE_ALL_PARTS);
        //--
    }
    
    private MacroUtils mu;
    private UserDeclarations ud;

    Double mfr;
    PartDisplayer pd1, pd2;
    Simulation sim;
    ScalarDisplayer scd;
    StreamDisplayer std;

}
    

