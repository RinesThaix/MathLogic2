package ru.luvas.ml2;

import ru.luvas.ml2.utils.Checker;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.luvas.ml2.Expressions.*;
import ru.luvas.ml2.utils.Aksioms;
import ru.luvas.ml2.utils.FileWorker;

/**
 *
 * @author RINES <iam@kostya.sexy>
 */
public class Deductor {

    private final List<Expression> supposing = new ArrayList<>(), memory = new ArrayList<>();
    private final List<String> induction = new ArrayList<>(), heading = new ArrayList<>();

    private String format(String s, String replacer) {
        return s.replace("%a", replacer);
    }

    private void println(List<String> out, String s, Object... args) {
        out.add(String.format(s, args));
    }

    public ArrayList<String> deduct(String header, List<String> proof) throws IOException {
        ArrayList<String> out = new ArrayList<>();
        getSuppositionsMapping(header);
        String alpha = heading.remove(heading.size() - 1);

        FormulaWorker:
        for (String formula : proof) {
            Expression e = ExpressionParser.parse(formula);
            Expression a = ExpressionParser.parse(alpha);
            Expression safe = ExpressionParser.parse(formula);

            if (Checker.check(e, a)) {
                memory.add(safe);
                induction.add(formula);
                String result = format("(%a)->(%a)->(%a)", alpha);
                out.add(result);
                out.add(format("(" + result + ")->((%a)->(((%a)->(%a))->(%a)))->((%a)->(%a))", alpha));
                out.add(format("((%a)->(((%a)->(%a))->(%a)))->((%a)->(%a))", alpha));
                out.add(format("((%a)->(((%a)->(%a))->(%a)))", alpha));
                out.add(format("(%a)->(%a)", alpha));
                continue;
            }
            if (supposing.get(0) != null) {
                for (Expression sup : supposing) {
                    if (Checker.check(sup, e)) {
                        out.add(formula);
                        memory.add(sup);
                        induction.add(formula);
                        println(out, "(%s)->(%s)->(%s)", formula, alpha, formula);
                        println(out, "(%s)->(%s)", alpha, formula);
                        continue FormulaWorker;
                    }
                }
            }

            if (Checker.checkSchemes(e, Aksioms.getParsedSchemes()) > 0
                    || Checker.checkAksioms(e, Aksioms.getParsedAksioms()) > 0
                    || Checker.checkAksioms(e)) {
                memory.add(safe);
                induction.add(formula);
                out.add(formula);
                println(out, "(%s)->(%s)->(%s)", formula, alpha, formula);
                println(out, "(%s)->(%s)", alpha, formula);
                continue;
            }
            if (Checker.checkAny(e, memory) >= 0) {
                BinaryOperation bo = (BinaryOperation) e;
                Quantor q = (Quantor) bo.getSecond();
                List<String> result = new ArrayList<>();
                result.add(alpha);
                result.add(bo.getFirst().toString());
                result.add(q.getExpression().toString());
                result.add(q.getVariable().toString());
                List<String> proofs = FileWorker.read("lib/p1.txt", result);
                memory.add(safe);
                induction.add(formula);
                out.addAll(proofs);
                continue;
            }
            e = Expressions.copy(safe);
            if (Checker.checkExistence(e, memory) >= 0) {
                BinaryOperation bo = (BinaryOperation) e;
                Quantor q = (Quantor) bo.getFirst();
                List<String> result = new ArrayList<>();
                result.add(alpha);
                result.add(q.getExpression().toString());
                result.add(bo.getSecond().toString());
                result.add(q.getVariable().toString());
                List<String> proofs = FileWorker.read("lib/p2.txt", result);
                memory.add(safe);
                induction.add(formula);
                out.addAll(proofs);
                continue;
            }
            e = Expressions.copy(safe);
            int[] mp = checkMP(e);
            if (mp != null) {
                memory.add(safe);
                induction.add(formula);
                String mp0 = induction.get(mp[0]);
                println(out, "((%s)->(%s))->(((%s)->((%s)->(%s)))->((%s)->(%s)))", alpha, mp0, alpha, mp0, formula, alpha, formula);
                println(out, "(((%s)->((%s)->(%s)))->((%s)->(%s)))", alpha, mp0, formula, alpha, formula);
                println(out, "(%s)->(%s)", alpha, formula);
                continue;
            }
            memory.add(safe);
            induction.add(formula);
            out.add(formula);
        }

        return out;
    }

    private int[] checkMP(Expression e) {
        for (int i = memory.size() - 1; i >= 0; --i) {
            Expression es = memory.get(i);
            if (es.getType() == OperationType.IMPLICATION && Checker.check(e, ((BinaryOperation) es).getSecond())) {
                for (int j = memory.size() - 1; j >= 0; --j) {
                    if (Checker.check(((BinaryOperation) es).getFirst(), memory.get(j))) {
                        return new int[]{j, i};
                    }
                }
            }
        }
        return null;
    }

    private Map<String, Integer> getSuppositionsMapping(String s) {
        Map<String, Integer> result = new HashMap<>();
        final int l = s.length();
        int pos = 0, n = 0, bal = 0;
        StringBuilder sb = new StringBuilder();
        while (pos < l) {
            while (Character.isWhitespace(s.charAt(pos))) {
                ++pos;
            }
            while (pos < l) {
                char c = s.charAt(pos);
                if (c == '|' || c == ',' && bal == 0) {
                    break;
                }
                if (c != ' ') {
                    sb.append(c);
                }
                if (c == '(') {
                    ++bal;
                } else if (c == ')') {
                    --bal;
                }
                ++pos;
            }
            ++n;
            String ss = sb.toString();
            result.put(ss, n);
            heading.add(ss);
            supposing.add(ExpressionParser.parse(ss));
            sb = new StringBuilder();
            if (pos < l && s.charAt(pos) == ',') {
                ++pos;
            }
            while (pos < l && Character.isWhitespace(s.charAt(pos))) {
                ++pos;
            }
            if (pos < l && s.charAt(pos) == '|') {
                pos = l;
            }
        }
        return result;
    }

}
