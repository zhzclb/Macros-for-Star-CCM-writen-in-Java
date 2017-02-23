
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
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import star.base.neo.DoubleVector;

public class Test extends StarMacro {

    String version = "v10";
    String flowRate = "23Lpm";
    String[] headers = {"Revision", "A", "B", "C", "D", "E"};
    Double mfr = 0.389;

    int resx = 1200;
    int resy = 300;

    public void execute() {

        initMacro(version, flowRate);

        PartManager pm = mu.getSimulation().getPartManager();
        pm.removeObjects(mu.get.parts.allByREGEX("(?i)^((?!(plane|flow)).)*$", vo));
    }

    void initMacro(String version, String flowRate) {
        mu = new MacroUtils(getSimulation());
        ud = mu.userDeclarations;
        ud.simTitle = version + "_" + flowRate;
        as = mu.getSimulation().getSimulationIterator().getAutoSave();
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
    int i;
    int j;

}
