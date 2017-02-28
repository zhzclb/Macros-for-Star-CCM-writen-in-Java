
/**
 * Propeller parametric simulation
 *
 * @author Andrew Gunderson
 *
 * 2017, v11.06
 */

import star.common.*;
import macroutils.*;
import java.util.*;

public class Props extends StarMacro {
    //----------------------------------------------------------------------
    // User inputs
    //----------------------------------------------------------------------
    // Titles, filenames, and headers

    String simTitle = "Fury4";
    String TR = "2016-1027-014";
    String PPTFileName = "TR" + TR + ".ppt";
    String propExcelFileName = "prop_data.xls";
    String gcExcelFileName = "gc_data.xls";
    String[] propHeaders = {"Model",
        "Speed (mph)",
        "Trim (deg)",
        "Height (in.)",
        "RPM",
        "Prop Lift (lbf)",
        "Prop Sideforce (lbf)",
        "Prop Thrust Net (lbf)",
        "Prop Thrust Normal (lbf)",
        "Prop Pitch Moment (lbf-ft)",
        "Prop Yaw Moment (lbf-ft)",
        "Prop Thrust",
        "Blade 1 Thrust (lbf)",
        "Prop Torque (lbf-ft)",
        "Blade 1 Torque (lbf-ft)",};
    String[] gcHeaders = {"Model",
        "Speed (mph)",
        "Trim (deg)",
        "Height (in.)",
        "RPM",
        "Gearcase Drag (lbf",
        "Gearcase Lift (lbf)",
        "Gearcase Sideforce (lbf)",
        "Gearcase Pitch Moment (lbf-ft)",
        "Gearcase Roll Moment (lbf-ft)",
        "Gearcase Yaw Moment (lbf-ft)"
    };
    // Simulation parameters
    double[] set_speeds = {62.7, 58.6}; // mph
    double[] set_trim = {5, 7.5, 10}; // deg, positive is trim out
    double[] set_height = {7.19}; // // level trim propshaft depth below water (in.)
    double[] set_rpm = {3135, 3265.5, 3396, 3526.5, 3657};
    double mdot = 0.4; // exhaust mass flow rate (kg/s)
    double stepsize = 1.0; // degrees
    double revs_init = 4; // number of prop revolutions for initial rpm setting
    double revs = 2; // number of prop revolutions for subsequent rpms
    double x_prop = 12.8799; // prop center from GC center (in)
    double trimPoint_z = 1.097; // z-coord of trim rotation point (m)
    double trimPoint_x = .282; // x-coord of trim rotation point (m)
    int numToAve = 300; // number of iterations or timesteps to average for monitor data
    int numPropReports = 10; // number of reports being exported to csv file
    int numGcReports = 6;

    public void execute() {
        initMacro();
    }

    void initMacro() {
        mu = new MacroUtils(getActiveSimulation());
        ud = mu.userDeclarations;

        //mu.set.region.motion(rm, mu.get.regions.byREGEX(".*rotating.*", true));
    }

    MacroUtils mu;
    UserDeclarations ud;
}
