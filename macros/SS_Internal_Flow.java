/**
 * Simple steady state internal flow simulation
 * with streamlines, contours, and total pressure monitors
 * 
 * @author Andrew Gunderson
 * 
 * 2017, v11.06
 */

import com.opencsv.CSVReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import macroutils.*;
import star.base.report.*;
import star.common.*;
import star.vis.*;
import org.apache.poi.xssf.usermodel.*;
import org.apache.commons.math3.stat.descriptive.*;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;

public class SS_Internal_Flow extends StarMacro {

    String[] versions = {"v9"};
    
    String[] flowRates = {"23lpm", "30lpm"};
    
    Double[] mfrs = {0.389, 0.517};
    
    int resx  = 1200;
    int resy = 300;
        
    public void execute() {

        // get pre-created plane sections
        sections = getActiveSimulation().getPartManager().getObjects();
        getActiveSimulation().kill();
        
        for (String version : versions) {
            for (String flowRate : flowRates) {
                
                initMacro(version, flowRate);

                /*
                if (mu.check.is.simFile(version + flowRate)){
                    break;
                }                
                if (mu.getSimulation().isParallel()){
                    
                }
                */
                
                physics();

                mesh(version);
                
                monitors();

                solve();
                
                post();

                output(version, flowRate);
                
                mu.saveSim();
                
            }
        }
    }

    void initMacro(String version, String flowRate) {
        mu = new MacroUtils(new Simulation(version + "_" + flowRate));
        ud = mu.userDeclarations;
        ud.simTitle = version + "_" + flowRate;        
        if (flowRate.contains(flowRates[0])) {
            mfr = mfrs[0];
        } else {
            mfr = mfrs[1];
        }
    }
    
    void physics() {
        if (mu.check.has.volumeMesh()) {
            return;
        }
        ud.physCont = mu.add.physicsContinua.generic(
                StaticDeclarations.Space.THREE_DIMENSIONAL,
                StaticDeclarations.Time.STEADY, 
                StaticDeclarations.Material.LIQUID,
                StaticDeclarations.Solver.SEGREGATED, 
                StaticDeclarations.Density.CONSTANT,
                StaticDeclarations.Energy.ISOTHERMAL, 
                StaticDeclarations.Viscous.RKE_2LAYER);
        mu.enable.cellQualityRemediation(ud.physCont, vo);
        ud.viscWater = 0.000854;
        ud.denWater = 1015;
        mu.set.physics.materialProperty(ud.physCont, "water", 
                StaticDeclarations.Vars.VISC, ud.viscWater, ud.unit_Pa_s);
    }

    void mesh(String version) {
        if (mu.check.has.volumeMesh()) {
            return;
        }
        // add geometry which is placed in working directory
        mu.add.geometry.importPart(version + ".x_b");
        ud.region = mu.add.region.fromAll(true);
        ud.geometryParts = mu.get.geometries.all(true);
        mu.add.scene.geometry();
        // mesh sizing (default units are mm)
        ud.mshBaseSize = 2;
        ud.prismsLayers = 4;
        ud.prismsRelSizeHeight = 33;
        ud.prismsStretching = 1.5;        
        ud.autoMshOp = mu.add.meshOperation.automatedMesh(ud.geometryParts,
                StaticDeclarations.Meshers.SURFACE_REMESHER,
                StaticDeclarations.Meshers.POLY_MESHER,
                StaticDeclarations.Meshers.PRISM_LAYER_MESHER);
        mu.set.boundary.asMassFlowInlet(
                mu.get.boundaries.byREGEX(".*" + ud.bcInlet, vo), 
                mfr, 20.0, 0.05, 10.0);
        mu.set.boundary.asPressureOutlet(
                mu.get.boundaries.byREGEX(".*" + ud.bcOutlet, vo), 
                0.0, 21.0, 0.05, 10.0);
        mu.update.volumeMesh();
        ud.scene = mu.add.scene.mesh();
    }
    
    void monitors() {
        if (mu.check.has.solution()) {
            return;
        }
        // choose region and create report for each plane section
        for (Part ps : sections){
            ps.getInputParts().setObjects(mu.get.regions.byREGEX(".*", vo));
            ud.rep = mu.add.report.massFlowAverage(ps,
                    ps.getPresentationName(), 
                    mu.get.objects.fieldFunction(
                            StaticDeclarations.Vars.TOTAL_P),
                    ud.unit_Pa, vo);
        }                
    }

    void solve() {
        if (mu.check.has.solution()) {
            return;
        }
        //mu.set.solver.aggressiveSettings();
        mu.step(3);
        //mu.saveSim();
    }

    void post() {
        if (mu.getSimulation().isParallel()) {
            return;
        }
        // set scene properties
        ud.defColormap = mu.get.objects.colormap(StaticDeclarations.Colormaps.BLUE_RED);
        ud.legVis = false;
        // create resampled volume scene
        ud.rvp = (ResampledVolumePart) mu.getSimulation().
                getPartManager().createResampledVolumePart();
        ud.rvp.getInputParts().setObjects(mu.get.regions.byREGEX(".*", vo));
        ud.namedObjects.clear();
        ud.namedObjects.add(ud.rvp);
        ud.ff = mu.get.objects.fieldFunction(
                StaticDeclarations.Vars.VEL.getVar(), vo);
        ud.scene = mu.add.scene.scalar(
                ud.namedObjects, ud.ff, ud.unit_mps, vo);
        ud.scene.setPresentationName("Velocity Contours");
        ud.disp = mu.get.scenes.displayerByREGEX(ud.scene, ".*", vo);
        ud.sdq = mu.get.scenes.scalarDisplayQuantity(ud.disp, vo);
        ud.sdq.setRange(new double[]{0,2});
        
        // create streamline scene
        ud.namedObjects.clear();
        ud.namedObjects.add(mu.get.parts.byREGEX(".*", vo));
        ud.namedObjects.add(mu.get.boundaries.byREGEX(ud.bcInlet, vo));
        ud.postStreamlinesTubesWidth = 0.0005;
        ud.scene1 = mu.add.scene.streamline(ud.namedObjects, true, true);
        ud.scene1.setPresentationName("Streamlines");
        ud.disp = mu.get.scenes.displayerByREGEX(
                ud.scene1, "(?i).*stream.*", vo);
        ud.sdq = mu.get.scenes.scalarDisplayQuantity(ud.disp, vo);
        ud.sdq.setRange(new double[]{0,2});
        
        // apply recommended visual settings
        mu.templates.prettify.all();
        
    }
    
    void output(String version, String flowRate) throws FileNotFoundException, IOException {
        if (mu.getSimulation().isParallel()) {
            return;
        }

        // read in camera views
        mu.io.read.cameraViews("myCameras.txt");       
        // output velo scene
        for (VisView vv : mu.get.cameras.allByREGEX(".*(1|2)", vo)) {
            mu.set.scene.cameraView(ud.scene, vv, vo);
            mu.io.sleep(1000);
            mu.io.write.picture(
                    ud.scene, 
                    version + flowRate + " " + vv.getPresentationName(), 
                    resx, resy, vo);
        }        
        // output streamline scene
        for (VisView vv : mu.get.cameras.allByREGEX(".*(3|4)", vo)) {
            mu.set.scene.cameraView(ud.scene1, vv, vo);
            mu.io.sleep(1000);
            mu.io.write.picture(
                    ud.scene1, 
                    ud.simTitle + " " + vv.getPresentationName(), 
                    resx, resy, vo);
        }        
        // update pressure drop results spreadsheet
        
        // if results.xlsx is not found then create with headers
        
        fs = new NPOIFSFileSystem(new File(ud.simPath + "results.xlsx"));
        wb = new XSSFWorkbook(fs.getRoot(), true);
        sheet = wb.getSheet("data");
        int currentRow = sheet.getLastRowNum() + 1;
        row = sheet.createRow(currentRow);
        j = 1;
        for ( Report rep : mu.get.reports.all(vo)) {
            ud.mon = mu.get.monitors.fromReport(rep, vo);
            String fileName = ud.simPath + "\\temp.csv";
            ud.mon.export(fileName);
            reader = new CSVReader(new FileReader(fileName));
            data = reader.readAll();
            for (i=data.size()-1; i>=data.size()-ud.numToAve; i++) {
                String[] rowData = data.get(i);
                stats.addValue(Double.parseDouble(rowData[1]));
            }
            if (j != 0) {
            row.createCell(j).setCellValue(mean - stats.getMean());
            }
            mean = stats.getMean();
            j++;
        }

    }
    
    private MacroUtils mu;
    private UserDeclarations ud;
    boolean vo = true;

    double mfr;
    Collection<Part> sections;
    XSSFWorkbook wb;
    XSSFSheet sheet;
    XSSFRow row;
    NPOIFSFileSystem fs;
    CSVReader reader;
    List<String[]> data;
    int i;
    int j;
    SummaryStatistics stats;
    double mean;
}
    

