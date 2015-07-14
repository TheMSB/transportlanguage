package otld.otld.parsing;

import javafx.collections.transformation.SortedList;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import otld.otld.grammar.otldBaseListener;
import otld.otld.grammar.otldParser;
import otld.otld.intermediate.*;
import otld.otld.intermediate.exceptions.FunctionAlreadyDeclared;
import otld.otld.intermediate.exceptions.TypeMismatch;
import otld.otld.intermediate.exceptions.VariableAlreadyDeclared;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Base visitor for the OTLD intermediate representation.
 */
public class otldRailroad extends otldBaseListener {

    /**
     * The program that is being parsed by this visitor.
     */
    private Program city;
    /**
     * All of the functions encountered during parsing.
     */
    ParseTreeProperty<Function> functions;
    /**
     * All of the conditionals encountered during parsing.
     */
    ParseTreeProperty<Conditional> conditionals;
    /**
     * The stack of operation sequences generated by parsing.
     */
    Stack<OperationSequence> stack;
    /**
     * A sorted list of errors encountered during parsing.
     */
    SortedList<Error> errors;

    /**
     * Returns the system type of that is identified by the passed string.
     */
    public Type getType(String ctxType) {
        switch (ctxType) {
            case "boolean":
                return Type.BOOL;
            case "int":
                return Type.INT;
            case "char":
                return Type.CHAR;
            default:
                return null;
        }
    }

    /**
     * Returns the system array type of that is identified by the passed string.
     */
    public Type getArrType(String ctxType) {
        switch (ctxType) {
            case "boolean":
                return Type.BOOLARR;
            case "int":
                return Type.INTARR;
            case "char":
                return Type.CHARARR;
            default:
                return null;
        }
    }

    @Override
    public void enterCity(otldParser.CityContext ctx) {
        city = new Program(ctx.ID().getText());
        stack.push(city.getBody());
    }

    @Override
    public void exitCity(otldParser.CityContext ctx) {
        stack.pop();
    }

    @Override
    public void enterDeftrain(otldParser.DeftrainContext ctx) {
        /*Train represents an array, arrays are as of yet still unsupported in our intermediate representation so this
        code isn't used.*/
        try {
            city.addVariable(Variable.create(getArrType(ctx.CARGO().getText()), ctx.ID().getText(), null));
        } catch (VariableAlreadyDeclared variableAlreadyDeclared) {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARALREADYDEFINED.getMessage()));
        }

    }

    @Override
    public void enterDefwagon(otldParser.DefwagonContext ctx) {
        try {
            Type type = getType(ctx.CARGO().getText());

            if (type != null) {
                city.addVariable(Variable.create(type, ctx.ID().getText(), null));
            } else {
                errors.add(new Error(ctx.CARGO().getSymbol().getLine(),
                        ctx.CARGO().getSymbol().getCharPositionInLine(),
                        ErrorMsg.TYPENOTDEFINED.getMessage()));
            }
        } catch (VariableAlreadyDeclared variableAlreadyDeclared) {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARALREADYDEFINED.getMessage()));
        }
    }

    @Override
    public void enterFactory(otldParser.FactoryContext ctx) {
        ArrayList<Type> types = new ArrayList<>(ctx.CARGO().size());
        for (TerminalNode n : ctx.CARGO()) {
            Type type = getType(n.getText());

            if (type != null) {
                types.add(type);
            } else {
                errors.add(new Error(ctx.CARGO().get(ctx.CARGO().indexOf(n)).getSymbol().getLine(),
                        ctx.CARGO().get(ctx.CARGO().indexOf(n)).getSymbol().getCharPositionInLine(),
                        ErrorMsg.TYPENOTDEFINED.getMessage()));
            }
        }

        try {
            Function function = new Function(ctx.ID().getText(), (Type[]) types.toArray());
            city.addFunction(function);
            functions.put(ctx.deffactory(), function);
        } catch (FunctionAlreadyDeclared functionAlreadyDeclared) {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.FACTALREADYDEFINED.getMessage()));
        }
    }

    @Override
    public void enterDeffactory(otldParser.DeffactoryContext ctx) {
        Function function = functions.get(ctx);
        stack.push(function.getBody());
    }

    @Override
    public void exitDeffactory(otldParser.DeffactoryContext ctx) {
        Return ret;
        if (city.getVariable(ctx.ID().getText()) != null) {
            ret = new Return(city.getVariable(ctx.ID().getText()));
            if (functions.get(ctx).getType().equals(ret.getSource().getType())) {
                stack.peek().add(ret);
            } else {
                errors.add(new Error(ctx.ID().getSymbol().getLine(),
                        ctx.ID().getSymbol().getCharPositionInLine(),
                        ErrorMsg.TYPEMISMATCH.getMessage()));
            }
        } else {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARNOTDEFINED.getMessage()));
        }

        stack.pop();
    }

    @Override
    public void enterDefsignal(otldParser.DefsignalContext ctx) {
        try {
            city.addVariable(Variable.create(Type.BOOL, ctx.ID().getText(), "false"));
        } catch (VariableAlreadyDeclared variableAlreadyDeclared) {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARALREADYDEFINED.getMessage()));
        }
    }

    @Override
    public void enterDefwaypoint(otldParser.DefwaypointContext ctx) {
        try {
            city.addFunction(new Function(ctx.ID().getText(), Type.BOOL));
            city.addVariable(Variable.create(Type.BOOL, ctx.ID().getText(), "false"));
            functions.put(ctx.ID(), null);
        } catch (FunctionAlreadyDeclared functionAlreadyDeclared) {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.FACTALREADYDEFINED.getMessage()));
        } catch (VariableAlreadyDeclared variableAlreadyDeclared) {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARALREADYDEFINED.getMessage()));
        }
    }

    @Override
    public void exitDefwaypoint(otldParser.DefwaypointContext ctx) {
        Return ret;
        if (city.getVariable(ctx.ID().getText()) != null) {
            ret = new Return(city.getVariable(ctx.ID().getText()));
            if (functions.get(ctx).getType().equals(ret.getSource().getType())) {
                stack.peek().add(ret);
            } else {
                errors.add(new Error(ctx.ID().getSymbol().getLine(),
                        ctx.ID().getSymbol().getCharPositionInLine(),
                        ErrorMsg.TYPEMISMATCH.getMessage()));
            }
        } else {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARNOTDEFINED.getMessage()));
        }
        stack.pop();
    }

    @Override
    public void enterDefcircle(otldParser.DefcircleContext ctx) {
        Variable variable = city.getVariable(ctx.ID().getText());

        if (variable != null) {
            try {
                stack.peek().add(new Call(city.getFunction(ctx.ID().getText())));
                Loop loop = new Loop(variable);
                stack.peek().add(loop);
                stack.push(loop.getBody());
                //Evaluate the condition again to determine if we should continue or not
                stack.push(loop.getConditionBody());
            } catch (TypeMismatch typeMismatch) {
                errors.add(new Error(ctx.ID().getSymbol().getLine(),
                        ctx.ID().getSymbol().getCharPositionInLine(),
                        ErrorMsg.TYPEMISMATCH.getMessage()));
            }
        }
    }

    @Override
    public void exitDefcircle(otldParser.DefcircleContext ctx) {
        stack.pop();
    }

    @Override
    public void enterIfcond(otldParser.IfcondContext ctx) {
        Variable variable = city.getVariable(ctx.ID().getText());
        if (variable != null) {
            Conditional cond = new Conditional(variable);
            stack.peek().add(cond);
            for (otldParser.IfcondcaseContext c : ctx.ifcondcase()) {
                conditionals.put(c, cond);
            }
        }
    }

    @Override
    public void enterIfcondcase(otldParser.IfcondcaseContext ctx) {
        Conditional conditional = conditionals.get(ctx);
        switch (ctx.BOOLEAN().getText()) {
            case "red":
                stack.push(conditional.getBodyFalse());
                break;
            case "green":
                stack.push(conditional.getBodyFalse());
                break;
            default:
                errors.add(new Error(ctx.BOOLEAN().getSymbol().getLine(),
                        ctx.BOOLEAN().getSymbol().getCharPositionInLine(),
                        ErrorMsg.UNKNOWNVALUE.getMessage()));
        }
    }

    @Override
    public void exitIfcondcase(otldParser.IfcondcaseContext ctx) {
        stack.pop();
    }

    @Override
    public void enterStop(otldParser.StopContext ctx) {
        stack.peek().add(new Break());
    }

    @Override
    public void enterLoad(otldParser.LoadContext ctx) {
        //TODO Array support is missing
        //Get the previously defined variable
        Variable var = city.getVariable(ctx.ID().getText());
        //Check if it has been previously defined.
        if (var != null) {
            if (ctx.INTEGER() != null) {
                if (var.getType().equals(Type.INT)) {
                    stack.peek().add(var.createValueAssignment(Integer.valueOf(ctx.INTEGER().getText())));
                }
            } else if (ctx.BOOLEAN() != null) {
                if (var.getType().equals(Type.BOOL)) {
                    //Translate our green/red to true/false
                    Boolean boolval = true;
                    if (ctx.BOOLEAN().getText().equals("red")) {
                        boolval = false;
                    }
                    stack.peek().add(var.createValueAssignment(boolval));
                }
            } else if (ctx.CHARACTER() != null) {
                if (var.getType().equals(Type.CHAR)) {
                    stack.peek().add(var.createValueAssignment(ctx.CHARACTER().getText().charAt(0)));
                }
            }
        } else {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARNOTDEFINED.getMessage()));
        }

    }

    @Override
    public void enterTransfer(otldParser.TransferContext ctx) {
        //TODO implement for arrays
        Variable var0 = city.getVariable(ctx.ID().get(0).getText());
        Variable var1 = city.getVariable(ctx.ID().get(1).getText());

        if (var0 != null) {
            if (var1 != null) {
                try {
                    stack.peek().add(var1.createVariableAssignment(var0));
                } catch (TypeMismatch typeMismatch) {
                    errors.add(new Error(ctx.ID().get(0).getSymbol().getLine(),
                            ctx.ID().get(0).getSymbol().getCharPositionInLine(),
                            ErrorMsg.TYPEMISMATCH.getMessage()));
                }
            } else {
                errors.add(new Error(ctx.ID().get(1).getSymbol().getLine(),
                        ctx.ID().get(1).getSymbol().getCharPositionInLine(),
                        ErrorMsg.VARNOTDEFINED.getMessage()));
            }
        } else {
            errors.add(new Error(ctx.ID().get(0).getSymbol().getLine(),
                    ctx.ID().get(0).getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARNOTDEFINED.getMessage()));
        }
    }

    @Override
    public void enterTransport(otldParser.TransportContext ctx) {
        ArrayList<Type> vars = new ArrayList<>(ctx.ID().size());

        //This means we have a predefined function
        if (ctx.OP() != null) {
            //Check if all of the provided arguments exist
            for (TerminalNode node : ctx.ID()) {
                if (city.getVariable(node.getText()) != null) {
                    vars.add(getType(node.getText()));
                } else {
                    errors.add(new Error(ctx.ID().get(ctx.ID().indexOf(node)).getSymbol().getLine(),
                            ctx.ID().get(ctx.ID().indexOf(node)).getSymbol().getCharPositionInLine(),
                            ErrorMsg.VARNOTDEFINED.getMessage()));
                }
            }
            try {
                Application appl = new Application(Operator.valueOf(ctx.OP().getText()), (Variable[]) vars.toArray());
                stack.peek().add(appl);
            } catch (TypeMismatch typeMismatch) {
                errors.add(new Error(ctx.ID().get(0).getSymbol().getLine(),
                        ctx.ID().get(0).getSymbol().getCharPositionInLine(),
                        ErrorMsg.TYPEMISMATCH.getMessage()));
            }
        }

        //This means we have a custom function
        else {
            //Remove the second to last argument since this is actually the function name
            vars.remove(ctx.ID().size() - 2);

            //Check if all of the provided arguments exist
            for (TerminalNode node : ctx.ID()) {
                if (city.getVariable(node.getText()) == null) {
                    errors.add(new Error(ctx.ID().get(ctx.ID().indexOf(node)).getSymbol().getLine(),
                            ctx.ID().get(ctx.ID().indexOf(node)).getSymbol().getCharPositionInLine(),
                            ErrorMsg.VARNOTDEFINED.getMessage()));
                }
                vars.add(getType(node.getText()));
            }

            Function func = city.getFunction(ctx.ID().get(ctx.ID().size() - 2).getText());
            //If the custom function exists then call it
            if (func != null) {
                try {
                    Call call = new Call(func, (Variable[]) vars.toArray());
                    stack.peek().add(call);
                } catch (TypeMismatch typeMismatch) {
                    errors.add(new Error(ctx.ID().get(0).getSymbol().getLine(),
                            ctx.ID().get(0).getSymbol().getCharPositionInLine(),
                            ErrorMsg.TYPEMISMATCH.getMessage()));
                }
            } else {
                errors.add(new Error(ctx.ID().get(ctx.ID().size() - 2).getSymbol().getLine(),
                        ctx.ID().get(ctx.ID().size() - 2).getSymbol().getCharPositionInLine(),
                        ErrorMsg.FACTUNDEFINED.getMessage()));
            }
        }
    }

    @Override
    public void enterInvert(otldParser.InvertContext ctx) {
        Variable var = city.getVariable(ctx.ID().getText());

        if (var != null) {
            if (var.getType().equals(Type.BOOL)) {
                try {
                    Application apl = new Application(Operator.NOT, var, var);
                    stack.peek().add(apl);
                } catch (TypeMismatch typeMismatch) {
                    errors.add(new Error(ctx.ID().getSymbol().getLine(),
                            ctx.ID().getSymbol().getCharPositionInLine(),
                            ErrorMsg.TYPEMISMATCH.getMessage()));
                }
            } else {
                errors.add(new Error(ctx.ID().getSymbol().getLine(),
                        ctx.ID().getSymbol().getCharPositionInLine(),
                        ErrorMsg.TYPEMISMATCH.getMessage()));
            }
        } else {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARNOTDEFINED.getMessage()));
        }
    }

    @Override
    public void enterUnarymin(otldParser.UnaryminContext ctx) {
        Variable var = city.getVariable(ctx.ID().getText());

        if (var != null) {
            try {
                Application appl = new Application(Operator.UMINUS, var, var);
                stack.peek().add(appl);
            } catch (TypeMismatch typeMismatch) {
                errors.add(new Error(ctx.ID().getSymbol().getLine(),
                        ctx.ID().getSymbol().getCharPositionInLine(),
                        ErrorMsg.TYPEMISMATCH.getMessage()));
            }
        }
    }

    @Override
    public void enterWrite(otldParser.WriteContext ctx) {
        Variable src = city.getVariable(ctx.ID().getText());
        if (src != null) {
            Output output = new Output(ctx.STRING().getText(), src);
            stack.peek().add(output);
        } else {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARNOTDEFINED.getMessage()));
        }
    }

    @Override
    public void enterInput(otldParser.InputContext ctx) {
        Variable dest = city.getVariable(ctx.ID().getText());
        if (dest != null) {
            Input input = new Input(ctx.STRING().getText(), dest);
            stack.peek().add(input);
        } else {
            errors.add(new Error(ctx.ID().getSymbol().getLine(),
                    ctx.ID().getSymbol().getCharPositionInLine(),
                    ErrorMsg.VARNOTDEFINED.getMessage()));
        }
    }
}
