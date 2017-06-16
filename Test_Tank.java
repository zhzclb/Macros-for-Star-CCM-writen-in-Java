
/**
 * Varies rpm, mfr, and timestep for a propeller in a test tank
 *
 * @author Andrew Gunderson
 *
 * 2017, v12.02
 */

import star.common.*;
import macroutils.*;
import star.motion.*;
import java.math.*;

public class Test_Tank extends StarMacro {
    
    //double[] rpms = {343, 714, 1000, 1714, 2286, 2857};
    double[] rpms = {1000, 2286};
    double mfr_wot = 0.0; // kgps
    double deg = 5;
    double stop = 10; // stopping criteria (s)

    public void execute() {
        varyRPM();
    }

    void varyRPM() {
        mu = new MacroUtils(getSimulation());
        ud = mu.userDeclarations;
        rm = (RotatingMotion) mu.getSimulation().get(
                MotionManager.class).getObject("Rotation");
        ud.bdry = mu.get.boundaries.byREGEX("exh_inlet", true);
        for (double rpm : rpms) {
            rm.getRotationRate().setValue(rpm);
            tStep = 60. / 360. / rpm * deg;
            mu.set.solver.timestep(tStep);
            mfr = Math.pow(rpm / 3543, 3) * mfr_wot;
            mu.set.boundary.values(ud.bdry,
                    StaticDeclarations.Vars.MFR, mfr, ud.unit_kgps);
            mu.get.solver.stoppingCriteria_MaxTime().setMaximumTime(stop);
            mu.run();
            ud.simTitle = rpm + "rpm";
            mu.saveSim();
            stop += 5;
        }
    }

    MacroUtils mu;
    UserDeclarations ud;
    RotatingMotion rm;
    double mfr;
    double tStep;

}
