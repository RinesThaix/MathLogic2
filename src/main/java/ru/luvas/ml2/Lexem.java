package ru.luvas.ml2;

import lombok.Data;
import ru.luvas.ml2.Expressions.*;
import static ru.luvas.ml2.Expressions.OperationType.*;

/**
 *
 * @author RINES <iam@kostya.sexy>
 */
@Data
public class Lexem {

    private final String expression;
    private OperationType type;
    private int position;
    private String lastVariable, lastPredicate, lastConst;

    public Lexem(String expression) {
        this.expression = expression;
        this.lastVariable = this.lastPredicate = this.lastConst = "";
        this.position = 0;
        this.next();
    }

    public void next() {
        if (position >= expression.length()) {
            type = END;
            return;
        }
        char cc = expression.charAt(position);
        while (position < expression.length() && Character.isWhitespace(cc)) {
            if (++position < expression.length()) {
                cc = expression.charAt(position);
            }
        }
        switch (cc) {
            case '|':
                type = OR;
                ++position;
                return;
            case '!':
                type = NOT;
                ++position;
                return;
            case '-':
                type = IMPLICATION;
                position += 2;
                return;
            case '&':
                type = AND;
                ++position;
                return;
            case '+':
                type = ADD;
                ++position;
                return;
            case '*':
                type = MUL;
                ++position;
                return;
            case '\'':
                type = NEXT;
                ++position;
                return;
            case '@':
                type = ANY;
                ++position;
                return;
            case '=':
                type = EQUAL;
                ++position;
                return;
            case '?':
                type = EXIST;
                ++position;
                return;
            case '(':
                type = OPEN;
                ++position;
                return;
            case ')':
                type = CLOSE;
                ++position;
                return;
            case ',':
                type = CHANGE;
                ++position;
                return;
        }
        StringBuilder sb = new StringBuilder();
        if (cc >= 'a' && cc <= 'z' || Character.isDigit(cc) && type == VARIABLE) {
            while (position < expression.length() && cc >= 'a' && cc <= 'z' || Character.isDigit(cc)) {
                sb.append(cc);
                if (++position == expression.length()) {
                    break;
                }
                cc = expression.charAt(position);
            }
            if (cc == '(' && type != ANY && type != EXIST) {
                if (expression.charAt(position - 1) == '(' && (expression.charAt(position - 2) == '@' || expression.charAt(position - 2) == '?')) {
                    lastVariable = sb.toString();
                }
                type = FUNCTION;
                lastPredicate = sb.toString();
            } else {
                type = VARIABLE;
                lastVariable = sb.toString();
            }
            return;
        }
        if (cc >= 'A' && cc <= 'Z' || Character.isDigit(cc) && type == PREDICATE) {
            while (position < expression.length() && cc >= 'A' && cc <= 'Z' || Character.isDigit(cc)) {
                lastPredicate = "" + cc;
                if (++position == expression.length()) {
                    break;
                }
                cc = expression.charAt(position);
            }
            type = PREDICATE;
            return;
        }
        if (Character.isDigit(cc)) {
            while (position < expression.length() && Character.isDigit(cc)) {
                lastConst = "" + cc;
                if (++position == expression.length()) {
                    break;
                }
                cc = expression.charAt(position);
            }
            type = CONST;
            return;
        }
        if (!Character.isWhitespace(cc)) {
            type = null;
            ++position;
        }
    }

    public String getExpression() {
        return expression.substring(position);
    }

}
