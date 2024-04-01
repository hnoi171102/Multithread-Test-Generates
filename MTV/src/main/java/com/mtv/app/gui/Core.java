package com.mtv.app.gui;

import com.mtv.encode.cfg.build.ControlFlowGraph;
import com.mtv.encode.constraint.ConstraintManager;
import com.mtv.encode.constraint.OrderConstraintsManager;
import com.mtv.encode.constraint.RWLConstraintsManager;
import com.mtv.encode.constraint.WriteConstraintsManager;
import com.mtv.encode.eog.EventOrderGraph;
import com.mtv.encode.eog.EventOrderGraphBuilder;
import com.mtv.encode.eog.ReadEventNode;
import com.mtv.encode.eog.WriteEventNode;
import com.mtv.output.ExcellReporter;
import com.microsoft.z3.*;
import com.mtv.debug.DebugHelper;
import com.mtv.encode.ast.ASTFactory;
import com.mtv.output.ExportToExcell;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.javatuples.Triplet;

import java.awt.geom.GeneralPath;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class Core {
    private String[] methodSignatures;
    private String filepath;
    public int getNLoops() {
        return nLoops;
    }

    public void setNLoops(int nLoops) {
        this.nLoops = nLoops;
    }

    public Core setLoop(int nLoops) {
        setNLoops(nLoops);
        return this;
    }
    private int nLoops = 1;
    int[] lineNumberOfMethods;
    static String SMTINPUT_DIR = "./src/main/resources/smt/";
    List<ControlFlowGraph> CFGList;
    ASTFactory ast;

    List<String> result;

    public String[] getMethodSignatures() {
        this.methodSignatures = ast.getFunctionSignatures().toArray(new String[0]);
        return methodSignatures;
    }

    public Core() {
    }

    public Core(String filepath) {
        this.filepath = filepath;
        ast = new ASTFactory(filepath);
    }

    static ArrayList<String> paths = new ArrayList<>();
    public List<String> runSolver() throws Exception {

        Integer timeOut = 100; // Timeout value
        String[] filePaths = {"D:\\kiem thu\\Multithread Test Generates\\MTV\\Benchmark\\sv_comp\\triangular-longer-1.c"};
        for (String path : filePaths) {
            paths.add(path);
        }

        GenerateTests(paths, timeOut);
        return null;
    }

    public  static void GenerateTests(ArrayList<String> filePaths, Integer timeOut) throws Exception {
        for (String filePath : paths) {
            Context ctx = new Context();
            Solver solver = ctx.mkSolver();
            Params timeOutParam = ctx.mkParams();
            timeOutParam.add("timeout", timeOut);
            solver.setParameters(timeOutParam);
            GenerateTest(filePath, ctx, solver);
//            ArrayList<ExcellReporter> reporters = new ArrayList<>();
//            reporters.add(VerifyFile(filePath, ctx, solver));
//            ExportToExcell.Export(reporters, timeOut);
//            reporters.clear();
            DebugHelper.print(filePath + " is verified");
            DebugHelper.print("\n=============================================\n");

            solver.reset();
            ctx.close();
        }
    }

    public static void GenerateTest(String filePath, Context ctx, Solver solver) throws Exception {
        DebugHelper.print("Start solve " + filePath.substring(filePath.lastIndexOf("\\") + 1));
        long buildConstraintsTime = BuildConstraints(filePath, ctx, solver);
        int numberConstraints = solver.getAssertions().length;

        long statSolveConstraints = System.currentTimeMillis();
        long numtest = 0;
        while(solver.check() == Status.SATISFIABLE) {
//            System.out.println("SATISFIABLE.Found the test");
            numtest += 1;
            Model model = solver.getModel();
            ArrayList<String> signatures = new ArrayList<String>();
            for (FuncDecl decl : model.getDecls()) {
                String varName = decl.getName().toString();
                Expr varExpr = ctx.mkConst(decl.getName(), decl.getRange());
                Expr valueExpr = model.getConstInterp(varExpr);
//                if(!valueExpr.isBool() || !valueExpr.isFalse())
//                    System.out.println(decl.getName() + " = " + model.getConstInterp(decl));
                if(valueExpr.isBool())
                {
                    if (valueExpr.isFalse()) {
                        signatures.add(varName);
                    }
                }
//                else if (varName.endsWith("_0") && valueExpr != null && valueExpr instanceof IntNum) {
//                    System.out.println(decl.getName() + " = " + model.getConstInterp(decl));
//                }
            }
            Expr[] signals = new Expr[signatures.size()];
            for (int i = 0; i < signatures.size(); i++) {
                Expr expr= ctx.mkBoolConst(signatures.get(i));
                signals[i] = expr;
            }
            solver.add(ctx.mkAtLeast(signals, 1));
        }
        System.out.println(numtest);
        DebugHelper.print("End solve " + filePath.substring(filePath.lastIndexOf("\\") + 1));
        long endSolveConstraints = System.currentTimeMillis();

//        ExcellReporter reporter = new ExcellReporter();
//
//        return reporter;
    }

    public static ASTFactory BuildAST(String filePath) throws Exception {
        ASTFactory ast = new ASTFactory(filePath);
        if (ast.getMain() == null) {
            throw new Exception("Main function 'main' of " + filePath + "is not detected.");
        }
        return ast;
    }
    public static ControlFlowGraph BuildCFG(ASTFactory ast) {
        IASTFunctionDefinition mainFunc = ast.getMain();
        ControlFlowGraph cfg = new ControlFlowGraph(mainFunc, ast, true);
        return cfg;
    }
    public static EventOrderGraph BuildEOG(ControlFlowGraph cfg) {
        EventOrderGraph eog = EventOrderGraphBuilder.Build(cfg, new HashMap<>(), false);
        return eog;
    }

    public static long BuildConstraints(String filePath, Context ctx, Solver solver) throws Exception {
        ASTFactory ast = BuildAST(filePath);

        ControlFlowGraph cfg = BuildCFG(ast);

        EventOrderGraph eog = BuildEOG(cfg);

        return BuildConstraints(ast, eog, ctx, solver);
    }

    public static long BuildConstraints(ASTFactory ast, EventOrderGraph eog, Context ctx, Solver solver) throws Exception {
        long startBuildConstraints = System.currentTimeMillis();
        ConstraintManager constraintManager = new ConstraintManager(ctx, solver);
        constraintManager.BuildConstraints(eog, ast);
        long endBuildConstraints = System.currentTimeMillis();

        return endBuildConstraints - startBuildConstraints;
    }

    public void create()
            throws FileNotFoundException {
    }
}
