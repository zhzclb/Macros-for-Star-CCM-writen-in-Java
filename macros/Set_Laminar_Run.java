
/**
 * Propeller parametric simulation
 *
 * @author Andrew Gunderson
 *
 * 2017, v11.06
 */
import star.common.*;
import macroutils.*;
import star.flow.*;
import star.kwturb.*;
import star.material.*;
import star.multiphase.*;
import star.segregatedmultiphase.*;
import star.turbulence.*;
import star.walldistance.*;

public class Set_Laminar_Run extends StarMacro {

    public void execute() {
        mu = new MacroUtils(getSimulation());
        ud = mu.userDeclarations;

        // disable models associated with turbulence
        ud.physCont = mu.get.objects.physicsContinua(".*", vo);
        ud.physCont.disableModel(ud.physCont.getModelManager().getModel(
                WallDistanceModel.class));
        ud.physCont.disableModel(ud.physCont.getModelManager().getModel(
                KwAllYplusWallTreatment.class));
        ud.physCont.disableModel(ud.physCont.getModelManager().getModel(
                SstKwTurbModel.class));
        ud.physCont.disableModel(ud.physCont.getModelManager().getModel(
                KOmegaTurbulence.class));
        ud.physCont.disableModel(ud.physCont.getModelManager().getModel(
                RansTurbulenceModel.class));
        ud.physCont.disableModel(ud.physCont.getModelManager().getModel(
                TurbulentModel.class));

        // enable laminar model
        //ud.physCont.enable(EulerianMultiPhaseModel.class);
        ud.physCont.enable(LaminarModel.class);
        
        // set stopping, clear, and run
        mu.get.solver.stoppingCriteria_MaxTime().setMaximumTime(150);
        mu.clear.solution();
        mu.run();
        mu.io.write.plots();
        mu.saveSim(ud.simTitle + "_laminar");

    }

    MacroUtils mu;
    UserDeclarations ud;
    boolean vo = true;
}
