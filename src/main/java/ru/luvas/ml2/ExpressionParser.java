package ru.luvas.ml2;

import java.util.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.luvas.ml2.Expressions.*;
import static ru.luvas.ml2.Expressions.OperationType.*;

/**
 *
 * @author RINES <iam@kostya.sexy>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExpressionParser {

    private final static ExpressionParser PARSER = new ExpressionParser();

    public static ExpressionParser get() {
        return PARSER;
    }

    public static Expression parse(String s) {
        return get().parse0(s);
    }

    private Lexem lexem;

    public Expression parse0(String expression) {
        lexem = new Lexem(expression);
        return parse();
    }

    private Expression parse() {
        return parse1();
    }

    private Expression parse10() {
        Expression e = null;
        if (lexem.getType() == OPEN) {
            lexem.next();
            e = parse();
            lexem.next();
        }
        if (lexem.getType() == VARIABLE) {
            e = new Variable(lexem.getLastVariable());
            lexem.next();
        }
        if (lexem.getType() == CONST) {
            e = new Const(lexem.getLastConst());
            lexem.next();
        }
        return e;
    }

    private Expression parse9() {
        Expression e = parse10();
        if (e == null) {
            while (lexem.getType() == FUNCTION) {
                lexem.next();
                String p = lexem.getLastPredicate();
                List<Expression> variables = new ArrayList<>();
                if (lexem.getType() == OPEN) {
                    lexem.next();
                    while (lexem.getType() != CLOSE && lexem.getType() != END) {
                        if (lexem.getType() == VARIABLE) {
                            variables.add(new Variable(lexem.getLastVariable()));
                        }
                        lexem.next();
                    }
                    lexem.next();
                }
                e = new Function(p, variables);
            }
        }
        return e;
    }

    private Expression parse8() {
        Expression e = parse9();
        while (lexem.getType() == NEXT) {
            lexem.next();
            e = Expressions.get(NEXT, e);
        }
        return e;
    }

    private Expression parse7() {
        Expression e = parse8();
        while (lexem.getType() == MUL || lexem.getType() == ADD) {
            if (lexem.getType() == MUL) {
                lexem.next();
                e = Expressions.get(MUL, e, parse8());
            } else {
                lexem.next();
                e = Expressions.get(ADD, e, parse8());
            }
        }
        return e;
    }

    private Expression parse6() {
        Expression e = parse7();
        while (lexem.getType() == EQUAL) {
            lexem.next();
            e = Expressions.get(EQUAL, e, parse7());
        }
        return e;
    }

    private Expression parse5() {
        Expression e = parse6();
        if (e == null) {
            while (lexem.getType() == PREDICATE) {
                lexem.next();
                String p = lexem.getLastPredicate();
                List<Expression> variables = new ArrayList<>();
                if (lexem.getType() == OPEN) {
                    lexem.next();
                    while (lexem.getType() == VARIABLE || lexem.getType() == FUNCTION || lexem.getType() == CHANGE) {
                        if (lexem.getType() == VARIABLE) {
                            variables.add(new Variable(lexem.getLastVariable()));
                        } else if (lexem.getType() == FUNCTION) {
                            variables.add(parse9());
                            continue;
                        }
                        lexem.next();
                    }
                    if (lexem.getType() == CLOSE) {
                        lexem.next();
                    }
                }
                e = new Predicate(p, variables);
            }
        }
        return e;
    }

    private Expression parse4() {
        Expression e;
        List<OperationType> unaries = new ArrayList<>();
        Stack<Variable> names = new Stack<>();
        while (lexem.getType() == NOT || lexem.getType() == ANY || lexem.getType() == EXIST) {
            OperationType type = lexem.getType();
            lexem.next();
            unaries.add(type);
            if (type != NOT) {
                while (lexem.getType() == OPEN) {
                    lexem.next();
                }
                names.push(new Variable(lexem.getLastVariable()));
                lexem.next();
                while (lexem.getType() == CLOSE) {
                    lexem.next();
                }
            }
        }
        e = parse5();
        Collections.reverse(unaries);
        for (OperationType ot : unaries) {
            switch (ot) {
                case ANY:
                    e = Expressions.getQuantor(ANY, names.pop(), e);
                    break;
                case EXIST:
                    e = Expressions.getQuantor(EXIST, names.pop(), e);
                    break;
                default:
                    e = Expressions.get(NOT, e);
                    break;
            }
        }
        return e;
    }

    private Expression parse3() {
        Expression e = parse4();
        while (lexem.getType() == AND) {
            lexem.next();
            e = Expressions.get(AND, e, parse4());
        }
        return e;
    }

    private Expression parse2() {
        Expression e = parse3();
        while (lexem.getType() == OR) {
            lexem.next();
            e = Expressions.get(OR, e, parse3());
        }
        return e;
    }

    private Expression parse1() {
        Expression e = parse2();
        while (lexem.getType() == IMPLICATION) {
            lexem = new Lexem(lexem.getExpression());
            e = Expressions.get(IMPLICATION, e, parse1());
        }
        return e;
    }

}
