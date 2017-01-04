package ru.luvas.ml2;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import static ru.luvas.ml2.Expressions.OperationType.*;

/**
 *
 * @author RINES <iam@kostya.sexy>
 */
public class Expressions {

    private final static Map<OperationType, ExpConstructor> CONSTRUCTORS = new EnumMap<>(OperationType.class);

    static {
        loadBinary(ADD, "(%s)+(%s)");
        loadBinary(MUL, "(%s)*(%s)");
        loadBinary(AND, "(%s)&(%s)");
        loadBinary(OR, "(%s)|(%s)");
        loadBinary(IMPLICATION, "(%s)->(%s)");
        loadBinary(EQUAL, "(%s)=(%s)");
        loadUnary(NOT, "!(%s)");
        loadUnary(NEXT, "(%s)'");
        loadQuantor(ANY, "@(%s)(%s)");
        loadQuantor(EXIST, "?(%s)(%s)");
    }

    public static Quantor getQuantor(OperationType type, Variable a, Expression b) {
        return ((QuantorConstructor) CONSTRUCTORS.get(type)).get(a, b);
    }

    public static BinaryOperation get(OperationType type, Expression a, Expression b) {
        return ((BinaryConstructor) CONSTRUCTORS.get(type)).get(a, b);
    }

    public static UnaryOperation get(OperationType type, Expression exp) {
        return ((UnaryConstructor) CONSTRUCTORS.get(type)).get(exp);
    }
    
    private static void loadUnary(OperationType type, String format) {
        UnaryConstructor constructor = a -> new UnaryOperation(type, a, format);
        CONSTRUCTORS.put(type, constructor);
    }
    
    private static void loadBinary(OperationType type, String format) {
        BinaryConstructor constructor = (a, b) -> new BinaryOperation(type, a, b, format);
        CONSTRUCTORS.put(type, constructor);
    }
    
    private static void loadQuantor(OperationType type, String format) {
        QuantorConstructor constructor = (v, e) -> new Quantor(type, v, e, format);
        CONSTRUCTORS.put(type, constructor);
    }

    public static Expression copy(Expression e) {
        if (e instanceof UnaryOperation) {
            return copy((UnaryOperation) e);
        }
        if (e instanceof BinaryOperation) {
            return copy((BinaryOperation) e);
        }
        if (e instanceof Variable) {
            return copy((Variable) e);
        }
        if (e instanceof Const) {
            return copy((Const) e);
        }
        if (e instanceof Quantor) {
            return copy((Quantor) e);
        }
        if (e instanceof Predicate) {
            return copy((Predicate) e);
        }
        if (e instanceof Function) {
            return copy((Function) e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Expression> T copy(T expression, Object... args) {
        try {
            return (T) expression.getClass().getConstructors()[0].newInstance(args);
        } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static UnaryOperation copy(UnaryOperation operation) {
        return copy(operation, operation.getType(), operation.getExpression(), operation.getStyle());
    }

    public static BinaryOperation copy(BinaryOperation operation) {
        return copy(operation, operation.getType(), operation.getFirst(), operation.getSecond(), operation.getStyle());
    }

    public static Const copy(Const var) {
        return copy(var, var.getName());
    }

    public static Variable copy(Variable var) {
        return (Variable) copy((Const) var);
    }

    public static Quantor copy(Quantor q) {
        return copy(q, q.getType(), q.getVariable(), q.getExpression(), q.getStyle());
    }

    public static Function copy(Function f) {
        return copy(f, f.getName(), f.getVariables());
    }

    public static Predicate copy(Predicate p) {
        return (Predicate) copy((Function) p);
    }

    private static void validate(Object o) {
        if (o == null) {
            throw new IllegalArgumentException("Arguments can't be null!");
        }
    }

    public static class Predicate extends Function {

        public Predicate(String name, List<Expression> variables) {
            super(PREDICATE, name, variables);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Predicate)) {
                return false;
            }
            Predicate p = (Predicate) o;
            if (!getName().equals(p.getName())) {
                return false;
            }
            if (getVariables().size() != p.getVariables().size()) {
                return false;
            }
            for (int i = 0; i < getVariables().size(); ++i) {
                if (!getVariables().get(i).toString().equals(p.getVariables().get(i).toString())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public final OperationType getType() {
            return PREDICATE;
        }

    }

    @Data
    public static class Function extends Expression {

        private String name;
        private List<Expression> variables;
        
        private Function(OperationType type, String name, List<Expression> variables) {
            super(type);
            this.name = name;
            this.variables = variables;
        }
        
        public Function(String name, List<Expression> variables) {
            this(FUNCTION, name, variables);
        }

        @Override
        public String toString() {
            if (variables.isEmpty()) {
                return name;
            }
            StringBuilder sb = new StringBuilder();
            sb.append(name).append('(');
            variables.stream().map(v -> v.toString() + ',').forEach(sb::append);
            String ss = sb.toString();
            return ss.substring(0, ss.length() - 1) + ')';
        }
    }

    @Data
    public static class Quantor extends Expression {

        private Variable variable;
        private Expression expression;
        private final String style;
        
        public Quantor(OperationType type, Variable variable, Expression expression, String style) {
            super(type);
            validate(variable);
            validate(expression);
            this.variable = variable;
            this.expression = expression;
            this.style = style;
        }

        @Override
        public String toString() {
            return String.format(style, variable.toString(), expression.toString());
        }

    }

    public static class Variable extends Const {

        public Variable(String name) {
            super(VARIABLE, name);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Variable && ((Variable) o).getName().equals(this.getName());
        }

    }

    public static class Const extends Expression {

        @Setter
        @Getter
        private final String name;
        
        private Const(OperationType type, String name) {
            super(type);
            this.name = name;
        }

        public Const(String name) {
            this(CONST, name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Data
    public static class BinaryOperation extends Expression {

        private Expression first, second;
        private final String style;

        public BinaryOperation(OperationType type, Expression first, Expression second, String style) {
            super(type);
            validate(first);
            validate(second);
            this.first = first;
            this.second = second;
            this.style = style;
        }

        @Override
        public String toString() {
            return String.format(style, first, second);
        }
    }

    @Data
    public static class UnaryOperation extends Expression {

        private Expression expression;
        private final String style;

        public UnaryOperation(OperationType type, Expression expression, String style) {
            super(type);
            validate(expression);
            this.expression = expression;
            this.style = style;
        }

        @Override
        public String toString() {
            return String.format(style, expression);
        }

    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Expression {
        
        @Getter
        private final OperationType type;
        
    }

    private static interface QuantorConstructor extends ExpConstructor {

        Quantor get(Variable var, Expression exp);
    }

    private static interface BinaryConstructor extends ExpConstructor {

        BinaryOperation get(Expression first, Expression second);
    }

    private static interface UnaryConstructor extends ExpConstructor {

        UnaryOperation get(Expression expression);
    }

    private static interface ExpConstructor {
    }

    public enum OperationType {
        END, OPEN, CLOSE, AND, OR, NOT, IMPLICATION, ADD, MUL, VARIABLE, CONST,
        FUNCTION, NEXT, ANY, EQUAL, CHANGE, PREDICATE, EXIST;
    }

}
