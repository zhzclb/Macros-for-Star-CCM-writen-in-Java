
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

    public void execute() {
        initMacro();
        
        MonitorPlot propPlot = (MonitorPlot) mu.get.plots.byREGEX("Prop", vo);
        propPlot.export(ud.simPath + "\\" + "prop.csv", ",");
        

    }

    void initMacro() {
        mu = new MacroUtils(getSimulation(), intrusive);
        ud = mu.userDeclarations;
    }

    private MacroUtils mu;
    private UserDeclarations ud;
    boolean vo = true;
    boolean intrusive = true;

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
