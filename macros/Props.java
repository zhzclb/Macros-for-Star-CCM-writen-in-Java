
/**
 * Propeller parametric simulation
 *
 * @author Andrew Gunderson
 *
 * 2017, v11.06
 */
import com.opencsv.CSVReader;
import java.io.*;
import star.common.*;
import macroutils.*;
import java.util.*;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import star.base.neo.DoubleVector;
import star.flow.*;
import star.meshing.*;
import star.vof.*;
import star.motion.*;

public class Props extends StarMacro {
    //--------------------------------------------------------------------------
    // -- USER INPUTS --
    //--------------------------------------------------------------------------
    boolean linux = false;
    String rev = "6036_v0";
    double[] subAreaRatio = {
        0.99948,
        0.97559,
        0.88561,
        0.82882,
        0.67153,
        0.60981};
    double dProp = 14.5027; // prop diameter (in)
    double x_prop = 13.19; // distance from prop center to GC center (in)
    //--------------------------------------------------------------------------
    
    String[] headers = {"Revision",
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
        "Blade 1 Torque (lbf-ft)",
        "Gearcase Drag (lbf",
        "Gearcase Lift (lbf)",
        "Gearcase Sideforce (lbf)",
        "Gearcase Pitch Moment (lbf-ft)",
        "Gearcase Roll Moment (lbf-ft)",
        "Gearcase Yaw Moment (lbf-ft)"
    };

    // Simulation parameters
    double[] speeds = {62.7, 58.6}; // mph
    double[] trims = {5, 7.5, 10}; // deg, positive is trim out
    double[] heights = {7.19}; // // level trim propshaft depth below water (in.)
    double[] rpms = {3135, 3265.5, 3396, 3526.5, 3657};
    double mfr = .4; // exhaust mass flow rate (kg/s)
    double stepSize = 1.; // degrees per timestep 
    double revs_init = .03; // number of prop revolutions for initial rpm setting
    double revs = .03; // number of prop revolutions for subsequent rpms
    double trimPoint_z = 43.19; // z distance from trim point to GC center (in)
    double trimPoint_x = 11.1; // x distance from trim point to GC center (in)
    int numPropReports = 10; // number of reports being exported to csv file
    int numTitleCol = 5; // number of columns containing run condition info (speed, trim, etc)
    int numPropCol = numPropReports + numTitleCol + 4;
    int numGcReports = 6; // number of gc reports being exported to csv
    
    public void execute() {
        try {
            initMacro();
            for (double speed : speeds) {
                setSpeed(speed);

                for (double height : heights) {
                    setHeight(height);

                    for (double trim : trims) {
                        setTrim(trim);
                        setCsys(height, trim);

                        for (double rpm : rpms) {
                            setRpm(rpm);
                            run(speed, height, trim, rpm);
                            exportScene();
                            exportData(speed, height, trim, rpm);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            mu.getSimulation().println(ex);
        }
    }

    void initMacro() {
        mu = new MacroUtils(getActiveSimulation(), intrusive);
        ud = mu.userDeclarations;
        ud.defColormap = mu.get.objects.colormap(
                StaticDeclarations.Colormaps.BLUE_RED);
        if (linux) {
            slash = "/";
        } else {
            slash = "\\";
        }
    }

    void setSpeed(double speed) {
        // set wave speed
        ud.physCont = mu.get.objects.physicsContinua(".*", vo);
        vwm = ud.physCont.getModelManager().getModel(VofWaveModel.class);
        fvw = (FlatVofWave) vwm.getVofWaveManager().getObject("FlatVofWave 1");
        fvw.getCurrent().setComponents(speed, 0, 0);
        fvw.getWind().setComponents(speed, 0, 0);

        // set pressure coeff ref velocity
        ud.ff = mu.get.objects.fieldFunction(
                StaticDeclarations.Vars.PC.getVar(), vo);
        pcf = (PressureCoefficientFunction) ud.ff;
        pcf.getReferenceVelocity().setValue(speed);

        // initialize number of meshes generated
        meshCount = -1;
    }

    void setHeight(double height) {
        // set heave value in parts translate operation
        tpo = (TransformPartsOperation) mu.getSimulation()
                .get(MeshOperationManager.class).getObject("Translate");
        tc = (TranslationControl) tpo.getTransforms().getObject("Heave");
        tc.getTranslationVector().setCoordinate(
                ud.unit_in, ud.unit_in, ud.unit_in,
                new DoubleVector(new double[]{0.0, 0.0, -height}));
    }

    void setTrim(double trim) {
        // set trim angle in parts rotate operation
        tpo = (TransformPartsOperation) mu.getSimulation()
                .get(MeshOperationManager.class).getObject("Rotate");
        rc = (RotationControl) tpo.getTransforms().getObject("Pitch");
        rc.getAngle().setValue(trim);

        // Move outer refinement zone to follow motion due to trim
        tpo = (TransformPartsOperation) mu.getSimulation()
                .get(MeshOperationManager.class)
                .getObject("Translate_Refine_Outer");
        tc = (TranslationControl) tpo.getTransforms().getObject("Translate");
        tc.getTranslationVector().setCoordinate(
                ud.unit_in, ud.unit_in, ud.unit_in,
                new DoubleVector(new double[]{
            trimPoint_z * Math.sin(trim * Math.PI / 180), 0.0,
            (trimPoint_x + x_prop) * Math.sin(trim * Math.PI / 180)}));

        // Clear solution history and fields
        mu.clear.solution();
        // Execute all mesh operations and update mesh count
        mu.update.volumeMesh();
        meshCount++;
    }

    void setCsys(double height, double trim) {

        // -- Trim Center --
        trimCenter = (CartesianCoordinateSystem) ud.lab0
                .getLocalCoordinateSystemManager().getObject("Trim_Center");
        trimCenter.getOrigin().setCoordinate(ud.unit_in, ud.unit_in, ud.unit_in,
                new DoubleVector(new double[]{
            -trimPoint_x, 0.0, trimPoint_z + -height}));
        // set orientation
        trimCenter.setBasis0(new DoubleVector(new double[]{
            Math.cos(trim * Math.PI / 180), 0.0,
            Math.sin(trim * Math.PI / 180)}));

        // -- GC Center --
        gcCenter = (CartesianCoordinateSystem) trimCenter
                .getLocalCoordinateSystemManager().getObject("GC_Center");
        gcCenter.getOrigin().setCoordinate(ud.unit_in, ud.unit_in, ud.unit_in,
                new DoubleVector(new double[]{
            trimPoint_x, 0.0, -trimPoint_z}));

        // -- Prop Center --
        propCenter = (CartesianCoordinateSystem) gcCenter
                .getLocalCoordinateSystemManager().getObject("Prop_Center");
        propCenter.getOrigin().setCoordinate(ud.unit_in, ud.unit_in, ud.unit_in,
                new DoubleVector(new double[]{x_prop, 0.0, 0.0}));
    }

    void setRpm(double rpm) {
        // set time step
        tStep = 1 / (rpm / 60 * 360 / stepSize);
        mu.set.solver.timestep(tStep);

        // set exhaust flow
        ud.bdry = mu.get.boundaries.byREGEX("Inlet_Exhaust", true);
        mu.set.boundary.values(ud.bdry,
                StaticDeclarations.Vars.MFR, mfr, ud.unit_kgps);

        // set prop rotation speed
        rm = (RotatingMotion) mu.getSimulation().get(
                MotionManager.class).getObject("Rotation");
        rm.getRotationRate().setValue(rpm);

        // set number of timesteps
        if (rpm == rpms[0]) {
            numSteps = (int) Math.round(revs_init * 360 / stepSize);
        } else {
            numSteps = (int) Math.round(revs * 360 / stepSize);
        }
    }

    void run(double speed, double height, double trim, double rpm) {
        mu.step(numSteps);
        ud.simTitle = rev + "_" + speed + "mph_" + trim + "deg_"
                + height + "in_" + rpm + "rpm";
        fileName = ud.simPath + slash + ud.simTitle;
        mu.saveSim();
    }

    void exportScene() {
        // export pressure coeff 3d scene
        ud.scene = mu.get.scenes.byREGEX("Scalar Scene", vo);
        ud.scene.export3DSceneFileAndWait(
                fileName + ".sce", ud.simTitle,
                "Pressure Coefficient", true, false);

        // clear solution history
        mu.clear.solutionHistory();

    }

    void exportData(double speed, double height, double trim, double rpm)
            throws Exception {
        // 
        // export data to csv
        MonitorPlot propPlot = (MonitorPlot) mu.get.plots.byREGEX("Prop", vo);
        propPlot.export(fileName + "_prop.csv", ",");
        MonitorPlot gcPlot = (MonitorPlot) mu.get.plots.byREGEX("Gearcase", vo);
        gcPlot.export(fileName + "_gc.csv", ",");

        // create results spreadsheet if not already created
        ssTitle = ud.simPath + slash + rev + "_results.xls";
        if (!new File(ssTitle).exists()) {
            initSpreadsheet();
        }

        // open existing results spreadsheet and create new row
        //ud.numToAve = (int) (360 / stepSize);
        ud.numToAve = 5;
        wb = WorkbookFactory.create(new File(ssTitle));
        sheet = wb.getSheet("Data");
        int currentRow = sheet.getLastRowNum() + 1;
        row = sheet.createRow(currentRow);
        row.createCell(0).setCellValue(rev);
        row.createCell(1).setCellValue(speed);
        row.createCell(2).setCellValue(trim);
        row.createCell(3).setCellValue(height);
        row.createCell(4).setCellValue(rpm);

        // read in prop data
        reader = new CSVReader(new FileReader(fileName + "_prop.csv"));
        data = reader.readAll();

        // compute mean and blade max/min of prop data
        stats = new SummaryStatistics();
        int reportIterator = 1;
        for (columnIterator = 5;
                columnIterator < numPropCol; columnIterator++) {
            for (rowIterator = data.size() - 1;
                    rowIterator >= data.size() - ud.numToAve; rowIterator--) {
                String[] array = data.get(rowIterator);
                stats.addValue(Double.parseDouble(array[reportIterator]));
            }
            // write data to row
            if (columnIterator == 12 || columnIterator == 16) {
                row.createCell(columnIterator).setCellValue(stats.getMean());
                row.createCell(columnIterator + 1).setCellValue(stats.getMax());
                row.createCell(columnIterator + 2).setCellValue(stats.getMin());
                columnIterator += 2;
            } else {
                row.createCell(columnIterator).setCellValue(stats.getMean());
            }
            stats = new SummaryStatistics();
            reportIterator++;
        }
        // Compute prop parameters of interest
        double SHP = rpm * 2 * Math.PI / 60
                * row.getCell(15).getNumericCellValue() / 550;
        double J = speed * 1.467 / (rpm / 60 * dProp / 12);
        double KT = row.getCell(7).getNumericCellValue()
                / (Math.pow(rpm / 60, 2) * Math.pow(dProp / 12, 4) * 1.94);
        double KT_norm = KT / subAreaRatio[meshCount];
        double KQ = row.getCell(15).getNumericCellValue()
                / (Math.pow(rpm / 60, 2) * Math.pow(dProp / 12, 5) * 1.94);
        double KQ_norm = KQ / subAreaRatio[meshCount];
        double eta = J / 2 / Math.PI * KT_norm / KQ_norm;

        // Write prop parameters to excel ss
        row.createCell(columnIterator).setCellValue(SHP); // colIt = 19
        row.createCell(columnIterator + 1).setCellValue(J);
        row.createCell(columnIterator + 2).setCellValue(KT_norm);
        row.createCell(columnIterator + 3).setCellValue(KQ_norm);
        row.createCell(columnIterator + 4).setCellValue(eta);
        int gcColStart = columnIterator + 5;

        // read in gearcase data
        reader = new CSVReader(new FileReader(fileName + "_gc.csv"));
        data = reader.readAll();

        // Compute mean and standard deviation of gc data
        stats = new SummaryStatistics();
        for (columnIterator = gcColStart;
                columnIterator <= gcColStart + numGcReports; columnIterator++) {
            for (rowIterator = data.size() - 1;
                    rowIterator >= data.size() - ud.numToAve; rowIterator--) {
                String[] array = data.get(rowIterator);
                stats.addValue(Double.parseDouble(array[columnIterator]));
            }
            row.createCell(columnIterator).setCellValue(stats.getMean());
            stats = new SummaryStatistics(); // clear report data
        }

        // save spreadsheet
        fileOut = new FileOutputStream(ssTitle);
        wb.write(fileOut);
        fileOut.close();

    }

    void initSpreadsheet() throws Exception {
        // Create prop excel workbook with headers
        wb = new HSSFWorkbook();
        row = wb.createSheet("Data").createRow(0);
        for (int i = 0; i < headers.length; i++) {
            row.createCell(i).setCellValue(headers[i]);
        }
        fileOut = new FileOutputStream(ssTitle);
        wb.write(fileOut);
        fileOut.close();
    }

    MacroUtils mu;
    UserDeclarations ud;
    boolean vo = true;
    boolean intrusive = true;

    int numSteps;
    int columnIterator;
    int rowIterator;
    int meshCount;

    FileOutputStream fileOut;
    Workbook wb;
    Sheet sheet;
    Row row;
    CSVReader reader;
    List<String[]> data;
    SummaryStatistics stats;
    VofWaveModel vwm;
    FlatVofWave fvw;
    TransformPartsOperation tpo;
    PressureCoefficientFunction pcf;
    RotationControl rc;
    TranslationControl tc;
    CartesianCoordinateSystem trimCenter;
    CartesianCoordinateSystem gcCenter;
    CartesianCoordinateSystem propCenter;
    RotatingMotion rm;
    String fileName;
    String slash;
    String ssTitle;
    double tStep;

}
