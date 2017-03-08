
import macroutils.*;
import star.common.*;
import star.vis.*;

public class Run extends StarMacro {

    public void execute() {
        initMacro();
        if (!mu.check.has.volumeMesh()) {
            mu.update.volumeMesh();
        }
        for (Displayer d : mu.get.scenes.allDisplayers(vo)) {
            d.setRepresentation(mu.get.mesh.fvr());
        }
        mu.step(30);
        mu.io.write.all();
        mu.saveSim();
    }

    void initMacro() {
        mu = new MacroUtils(getSimulation(), intrusive);
        ud = mu.userDeclarations;
    }

    MacroUtils mu;
    UserDeclarations ud;
    boolean vo = true;
    boolean intrusive = true;
}
