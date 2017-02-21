/**
 * Simple steady state internal flow simulation
 * with streamlines, contours, and total pressure monitors
 * 
 * @author Andrew Gunderson
 * 
 * 2017, v11.06
 */

import java.io.*;
import java.util.*;
import macroutils.*;
import star.base.report.*;
import star.common.*;
import star.vis.*;
import org.apache.commons.math3.stat.descriptive.*;
import org.apache.poi.ss.usermodel.*;
import com.opencsv.CSVReader;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class SS_Internal_Para extends StarMacro {

    String[] versions = {"v10"};
    String[] flowRates = {"23Lpm", "30Lpm"};
    String[] headers = {"Revision", "A", "B", "C", "D" , "E", "F"};
    Double[] mfrs = {0.389, 0.517};
    
    int resx  = 1200;
    int resy = 300;
        
    public void execute() {
        
        String workingDir = getSimulation().getSessionDir();
        
        for (String version : versions) {
            for (String flowRate : flowRates) {
                try {
                    
                    initMacro(version, flowRate, workingDir);

                    if (!mu.check.has.volumeMesh()) {
                        mesh(version);
                    }
                    
                    if (!mu.check.has.solution()) {
                        monitors();
                        solve();
                    }

                    if (!mu.getSimulation().isParallel()) {
                        post();
                        output();
                    }
                    
                    mu.saveSim();

                } catch (Exception ex) {
                    mu.getSimulation().println(ex);
                }
            }
        }
    }

    void initMacro(String version, String flowRate, String workingDir) {
        mu = new MacroUtils(new Simulation(workingDir + "\\star.sim"));
        ud = mu.userDeclarations;
        ud.simTitle = version + "_" + flowRate;
        as = mu.getSimulation().getSimulationIterator().getAutoSave();        
        if (flowRate.contains(flowRates[0])) {
            mfr = mfrs[0];
        } else {
            mfr = mfrs[1];
        }
    }
    
    void mesh(String version) {
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
        as.setAutoSaveMesh(false);
        mu.update.volumeMesh();
        ud.scene = mu.add.scene.mesh();
        mu.saveSim();
    }
    
    void monitors() {
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
        //mu.set.solver.aggressiveSettings();
        as.setAutoSaveBatch(false);
        mu.step(10);
        ud.numToAve = 5;
        
        //mu.saveSim();
    }

    void post() {
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
    
    void output() throws Exception {
        // read in camera views
        mu.io.read.cameraViews("myCameras.txt");       
        // output velo scene
        for (VisView vv : mu.get.cameras.allByREGEX(".*(1|2)", vo)) {
            mu.set.scene.cameraView(ud.scene, vv, vo);
            mu.io.sleep(1000);
            mu.io.write.picture(
                    ud.scene, 
                    ud.simTitle + " " + vv.getPresentationName(), 
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
        // create or update pressure drop results spreadsheet
        String ssTitle = ud.simPath + "results.xlsx";
        if (!new File(ssTitle).exists()) {
            wb = new XSSFWorkbook();
            sheet = wb.createSheet("data");
            row = sheet.createRow(0);
            for (i = 0; i < headers.length; i++) {
                row.createCell(i).setCellValue(headers[i]);
            out = new FileOutputStream(ssTitle);
            wb.write(out);
            out.close();
            }
        }
        wb = WorkbookFactory.create(new File(ssTitle));
        sheet = wb.getSheet("data");
        int currentRow = sheet.getLastRowNum() + 1;
        row = sheet.createRow(currentRow);
        j = 1;
        for (Report rep : mu.get.reports.all(vo)) {
            ud.mon = mu.get.monitors.fromReport(rep, vo);
            String fileName = ud.simPath + "\\temp.csv";
            ud.mon.export(fileName);
            reader = new CSVReader(new FileReader(fileName));
            data = reader.readAll();
            for (i = data.size() - 1; i >= data.size() - ud.numToAve; i++) {
                String[] rowData = data.get(i);
                stats.addValue(Double.parseDouble(rowData[1]));
            }
            if (j != 0) {
                row.createCell(j).setCellValue(mean - stats.getMean());
            } else{
                row.createCell(0).setCellValue(ud.simTitle);
            }
            mean = stats.getMean();
            out = new FileOutputStream(ssTitle);
            wb.write(out);
            out.close();
            j++;
        }
    }
    
    private MacroUtils mu;
    private UserDeclarations ud;
    boolean vo = true;

    Collection<Part> sections;
    List<String[]> data;
    Workbook wb;
    Sheet sheet;
    Row row;
    CSVReader reader;
    SummaryStatistics stats;
    FileOutputStream out;
    AutoSave as;
    
    double mean;
    double mfr;    
    int i;
    int j;

}
    

