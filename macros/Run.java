import macroutils.*;
import star.common.*;

public class Run extends StarMacro {
    
    public void execute() {
        initMacro();
        if (!mu.check.has.volumeMesh()) {
            mu.update.volumeMesh();
        }
        mu.step(10000);
        mu.io.write.all();
    }
    
    void initMacro(){
        mu = new MacroUtils(getSimulation());
        ud = mu.userDeclarations;
    }
    
    MacroUtils mu;
    UserDeclarations ud;
    boolean vo = true;
}