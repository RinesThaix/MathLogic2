package ru.luvas.ml2.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ru.luvas.ml2.Expressions;
import static ru.luvas.ml2.Expressions.OperationType.*;
import ru.luvas.ml2.Expressions.*;

/**
 *
 * @author RINES <iam@kostya.sexy>
 */
public class Checker {

    public static boolean checking = false;
    public static boolean any = false;
    public static boolean exs = false;

    public static boolean check(Expression a, Expression b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getType() == b.getType()) {
            if (a instanceof BinaryOperation) {
                BinaryOperation ba = (BinaryOperation) a, bb = (BinaryOperation) b;
                return check(ba.getFirst(), bb.getFirst()) && check(ba.getSecond(), bb.getSecond());
            }
            if (a instanceof UnaryOperation) {
                return check(((UnaryOperation) a).getExpression(), ((UnaryOperation) b).getExpression());
            }
            if (a instanceof Quantor) {
                Quantor qa = (Quantor) a, qb = (Quantor) b;
                return check(qa.getVariable(), qb.getVariable()) && check(qa.getExpression(), qa.getExpression());
            }
            if (a instanceof Function) {
                Function fa = (Function) a, fb = (Function) b;
                return fa.getName().equals(fb.getName()) && checkTwoLists(fa.getVariables(), fb.getVariables());
            }
            if (a instanceof Const) {
                return ((Const) a).getName().equals(((Const) b).getName());
            }
        }
        return false;
    }

    public static boolean checkTwoLists(List<Expression> a, List<Expression> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); ++i) {
            if (!a.get(i).toString().equals(b.get(i).toString())) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkEquality(Expression a, Expression b, Map<String, Expression> dict) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getType() == b.getType()) {
            if (a instanceof BinaryOperation) {
                BinaryOperation ba = (BinaryOperation) a, bb = (BinaryOperation) b;
                return checkEquality(ba.getFirst(), bb.getFirst(), dict)
                        && checkEquality(ba.getSecond(), bb.getSecond(), dict);
            }

            if (a instanceof Quantor) {
                Quantor qa = (Quantor) a, qb = (Quantor) b;
                return checkEquality(qa.getVariable(), qb.getVariable(), dict) && checkEquality(qa.getExpression(), qb.getExpression(), dict);
            }

            if (a instanceof Function) {
                Function fa = (Function) a, fb = (Function) b;
                if (fa.getVariables().size() != fb.getVariables().size() || !(fa.getName().equals(fb.getName()))) {
                    return false;
                }
                for (int i = 0; i < fa.getVariables().size(); ++i) {
                    if (!checkEquality(fa.getVariables().get(i), fb.getVariables().get(i), dict)) {
                        return false;
                    }
                }
                return true;
            }

            if (a instanceof UnaryOperation) {
                return checkEquality(((UnaryOperation) a).getExpression(), ((UnaryOperation) b).getExpression(), dict);
            }

            if (a.getType() == CONST) {
                return ((Const) a).getName().equals(((Const) b).getName());
            }

            if (a.getType() == VARIABLE) {
                Variable va = (Variable) a, vb = (Variable) b;
                if (va.getName().equals(vb.getName())) {
                    if (!dict.containsKey(((Variable) b).getName())) {
                        dict.put(va.getName(), b);
                    } else {
                        return dict.get(vb.getName()).toString().equals(va.getName());
                    }
                    return true;
                }
                String n = vb.getName();
                if (dict.containsKey(n)) {
                    return check(a, dict.get(n));
                }
                dict.put(n, a);
                return true;
            }

        }
        if (b.getType() == VARIABLE) {
            String n = ((Variable) b).getName();
            if (dict.containsKey(n)) {
                return check(a, dict.get(n));
            }
            dict.put(n, a);
            return true;
        }
        return false;
    }

    public static boolean checkAksioms(Expression e) {
        if (e.getType() != IMPLICATION) {
            return false;
        }
        BinaryOperation impl = (BinaryOperation) e;
        if (impl.getFirst().getType() != AND) {
            return false;
        }
        BinaryOperation and = (BinaryOperation) impl.getFirst();
        if (and.getSecond().getType() != ANY) {
            return false;
        }
        Quantor any = (Quantor) and.getSecond();
        Map<String, Expression> dictionary = new HashMap<>();
        if (!checkEquality(and.getFirst(), impl.getSecond(), dictionary)) {
            return false;
        }
        Variable v = any.getVariable();
        if (dictionary.get(v.getName()).getType() != CONST) {
            return false;
        }
        Expression copied = Expressions.copy(e);
        BinaryOperation impl2 = (BinaryOperation) copied;
        Expression sub = substitute(impl2.getSecond(), v, dictionary.get(v.getName()));
        if (!check(sub, ((BinaryOperation) impl2.getFirst()).getFirst())) {
            return false;
        }
        dictionary.clear();
        Expression temp = ((Quantor) ((BinaryOperation) impl2.getFirst()).getSecond()).getExpression();
        return temp.getType() == IMPLICATION && checkEquality(((BinaryOperation) temp).getSecond(), ((BinaryOperation) temp).getFirst(), dictionary);
    }

    public static int checkAny(Expression e, List<Expression> memory) {
        if (e.getType() != IMPLICATION) {
            return -1;
        }
        BinaryOperation bo = (BinaryOperation) e;
        for (int i = memory.size() - 1; i >= 0; --i) {
            Expression es = memory.get(i);
            if (es.getType() != IMPLICATION) {
                continue;
            }
            BinaryOperation bos = (BinaryOperation) es;
            if (check(bo.getFirst(), bos.getFirst())) {
                if (!(bo.getSecond() instanceof Quantor)) {
                    continue;
                }
                Quantor q = (Quantor) bo.getSecond();
                if (check(bos.getSecond(), q.getExpression())) {
                    any = true;
                    if (checkGracefully0(q.getVariable(), bo.getFirst())) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public static int checkExistence(Expression e, List<Expression> memory) {
        if (e.getType() != IMPLICATION) {
            return -1;
        }
        BinaryOperation bo = (BinaryOperation) e;
        for (int i = memory.size() - 1; i >= 0; --i) {
            Expression es = memory.get(i);
            if (es.getType() != IMPLICATION) {
                continue;
            }
            BinaryOperation bos = (BinaryOperation) es;
            if (check(bo.getSecond(), bos.getSecond())) {
                if (bo.getFirst().getType() != EXIST) {
                    continue;
                }
                Quantor q = (Quantor) bo.getFirst();
                if (check(bos.getFirst(), q.getExpression())) {
                    exs = true;
                    if (checkGracefully0(q.getVariable(), bo.getSecond())) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    public static boolean checkGracefully0(Variable v, Expression exp) {
        return checkGracefully(v, exp, false);
    }

    public static boolean checkGracefully(Variable v, Expression exp, boolean q) {
        if (exp instanceof Variable && check(exp, v)) {
            return q;
        }
        if (exp instanceof BinaryOperation) {
            return checkGracefully(v, ((BinaryOperation) exp).getFirst(), q) && checkGracefully(v, ((BinaryOperation) exp).getSecond(), q);
        }
        if (exp instanceof UnaryOperation) {
            return checkGracefully(v, ((UnaryOperation) exp).getExpression(), q);
        }
        if (exp instanceof Quantor) {
            return check(((Quantor) exp).getVariable(), v) || checkGracefully(v, ((Quantor) exp).getExpression(), q);
        }
        if (exp instanceof Function) {
            return q || ((Function) exp).getVariables().stream().allMatch(var -> checkGracefully(v, var, q));
        }
        return true;
    }

    public static Expression substitute(Expression e, Variable v, Expression sub) {
        if (e instanceof BinaryOperation) {
            BinaryOperation bo = (BinaryOperation) e;
            bo.setFirst(substitute(bo.getFirst(), v, sub));
            bo.setSecond(substitute(bo.getSecond(), v, sub));
            return e;
        }
        if (e instanceof UnaryOperation) {
            UnaryOperation uo = (UnaryOperation) e;
            uo.setExpression(substitute(uo.getExpression(), v, sub));
            return e;
        }
        if (e instanceof Quantor) {
            Quantor q = (Quantor) e;
            check(q.getVariable(), sub);
            q.setExpression(substitute(q.getExpression(), v, sub));
            return e;
        }
        if (e instanceof Function) {
            Function f = (Function) e;
            f.setVariables(f.getVariables().stream().map(exp -> substitute(exp, v, sub)).collect(Collectors.toList()));
        }
        if (e.getType() == VARIABLE) {
            if (check(e, v)) {
                return sub;
            }
        }
        return e;
    }

    public static int checkSchemes(Expression e, List<Expression> exps) {
        Map<String, Expression> dict;
        for (int i = 0; i < exps.size(); ++i) {
            if (checkEquality(e, exps.get(i), new HashMap<>())) {
                return i + 1;
            }
        }
        dict = new HashMap<>();

        ANY:
        {
            if (e.getType() != IMPLICATION) {
                break ANY;
            }
            BinaryOperation impl = (BinaryOperation) e;
            if (impl.getFirst().getType() != ANY) {
                break ANY;
            }
            Quantor any = (Quantor) impl.getFirst();
            if (!checkEquality(impl.getSecond(), any.getExpression(), dict)) {
                break ANY;
            }
            Expression copied = Expressions.copy(e);
            Expression sub = substitute(impl.getFirst(), any.getVariable(), dict.get(any.getVariable().getName()));
            e = copied;
            impl = (BinaryOperation) e;
            if (!check(impl.getSecond(), ((Quantor) sub).getExpression())) {
                break ANY;
            }
            any = (Quantor) impl.getFirst();
            Expression exp = dict.get(any.getVariable().getName());
            if (check(exp, any.getVariable()) || checkQuant(any.getExpression(), any.getVariable(), false, false, exp)) {
                return 11;
            }
            checking = true;
            checkEquality(impl.getSecond(), any.getExpression(), dict);
        }

        dict = new HashMap<>();

        EXIST:
        {
            if (e.getType() != IMPLICATION) {
                break EXIST;
            }
            BinaryOperation impl = (BinaryOperation) e;
            if (impl.getSecond().getType() != EXIST) {
                break EXIST;
            }
            Quantor exist = (Quantor) impl.getSecond();
            if (!checkEquality(impl.getFirst(), exist.getExpression(), dict)) {
                break EXIST;
            }
            Expression copied = Expressions.copy(e);
            Expression sub = substitute(impl.getSecond(), exist.getVariable(), dict.get(exist.getVariable().getName()));
            e = copied;
            impl = (BinaryOperation) e;
            if (!check(impl.getFirst(), ((Quantor) sub).getExpression())) {
                break EXIST;
            }
            exist = (Quantor) impl.getSecond();
            Expression exp = dict.get(exist.getVariable().getName());
            if (check(exp, exist.getVariable()) || checkQuant(exist.getExpression(), exist.getVariable(), false, false, exp)) {
                return 12;
            }
            checking = true;
        }

        return -1;
    }

    public static boolean checkQuant(Expression e, Variable v, boolean qx, boolean qa, Expression exp) {
        if (e.getType() == VARIABLE && check(e, v)) {
            return qx || !qa;
        }
        if (e instanceof Quantor && check(((Quantor) e).getVariable(), exp)) {
            return checkQuant(((Quantor) e).getExpression(), v, qx, true, exp);
        }
        if (e instanceof Quantor && check(((Quantor) e).getVariable(), v)) {
            return true;
        }
        if (e instanceof BinaryOperation) {
            BinaryOperation bo = (BinaryOperation) e;
            return checkQuant(bo.getFirst(), v, qx, qa, exp) && checkQuant(bo.getSecond(), v, qx, qa, exp);
        }
        if (e instanceof UnaryOperation) {
            return checkQuant(((UnaryOperation) e).getExpression(), v, qx, qa, exp);
        }
        if (e.getType() == PREDICATE) {
            return !((Predicate) e).getVariables().contains(v) || qx || !qa;
        }
        return e.getType() != FUNCTION || ((Function) e).getVariables().contains(v) && !qx && qa;
    }

    public static int checkAksioms(Expression e, List<Expression> aksioms) {
        for (int i = 0; i < aksioms.size(); ++i) {
            if (check(aksioms.get(i), e)) {
                return i + 1;
            }
        }
        return -1;
    }

}
