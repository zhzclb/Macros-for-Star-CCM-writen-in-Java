
import star.common.*;
import macroutils.*;

public class Mesh_Run extends StarMacro {

    public void execute() {
        mu = new MacroUtils(getSimulation());
        ud = mu.userDeclarations;
        mu.update.volumeMesh();
        mu.run();
    }
    
    MacroUtils mu;
    UserDeclarations ud;
}
