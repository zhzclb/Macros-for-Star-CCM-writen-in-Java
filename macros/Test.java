import star.common.*;
import macroutils.*;
import java.util.*;

public class Test extends StarMacro {

    public void execute() {
        MacroUtils mu = new MacroUtils(getActiveSimulation());
        UserDeclarations ud = mu.userDeclarations;
        ud.mon = mu.get.monitors.fromReport(mu.get.reports.byREGEX("Drag", true), true);
        ud.mon.export(ud.simPath+"\\fromMonitor.csv");
        // the above has been tested and works
    }
}
