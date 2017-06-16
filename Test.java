
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

        int version = 1;
        double[][] subAreaRatios = {
            {0.8856, 0.7886, 0.6715},
            {0.8740, 0.7787, 0.6650},
            {0.8978, 0.7992, 0.6785},
            {0.8856, 0.7886, 0.6716},
            {0.8856, 0.7886, 0.6715},
            {0.8856, 0.7885, 0.6714},
            {0.8856, 0.7887, 0.6716},
            {0.8856, 0.7886, 0.6715},
            {0.8856, 0.7886, 0.6715},
            {0.8853, 0.7880, 0.6707},
            {0.8859, 0.7892, 0.6724},
            {0.8855, 0.7885, 0.6713},
            {0.8857, 0.7887, 0.6717},
            {0.8856, 0.7886, 0.6715},
            {0.8856, 0.7886, 0.6715}
        };
        double[] subAreaRatio = new double[subAreaRatios[0].length];
        for (int i = 0; i < subAreaRatios[0].length; i++) {
            subAreaRatio[i] = subAreaRatios[version][i];
        }

        mu.io.say.value("Submerged Area Ratio",
                Arrays.toString(subAreaRatio), null, vo);

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
