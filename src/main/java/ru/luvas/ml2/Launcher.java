package ru.luvas.ml2;

import ru.luvas.ml2.utils.Checker;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ru.luvas.ml2.Expressions.BinaryOperation;
import ru.luvas.ml2.Expressions.Expression;
import ru.luvas.ml2.Expressions.OperationType;
import static ru.luvas.ml2.Expressions.OperationType.ANY;
import static ru.luvas.ml2.Expressions.OperationType.EXIST;
import static ru.luvas.ml2.Expressions.OperationType.IMPLICATION;
import ru.luvas.ml2.Expressions.Quantor;
import ru.luvas.ml2.utils.Aksioms;
import ru.luvas.ml2.utils.FileWorker.FastScanner;

/**
 *
 * @author RINES <iam@kostya.sexy>
 */
public class Launcher {

    private int position = 1;
    private List<Expression> supposing = new ArrayList<>(), memory = new ArrayList<>();
    private List<String> induction = new ArrayList<>();
    private HashMap<String, Expression> dict;
    private String header;

    private FastScanner scan;
    private PrintWriter out;

    private Launcher() {
        try {
            scan = new FastScanner("input.txt");
            out = new PrintWriter(new File("output.txt"));
            solve();
            out.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void solve() throws IOException {
        header = scan.next();
        out.println(header);
        header = header.replaceAll("\\s", "");
        Map<String, Integer> supposition_bank = getSuppositionsMapping(header);
        String formula = scan.next();

        FormulaWorker:
        while (formula != null) {
            formula = formula.replaceAll("\\s", "");
            induction.add(formula);
            if (supposition_bank.containsKey(formula)) {
                int number = supposition_bank.get(formula);
                println("(%d) %s (Предположение %d)", position++, formula, number);
                memory.add(supposing.get(number - 1));
                formula = scan.next();
                continue;
            }
            Expression e = ExpressionParser.parse(formula);
            Checker.checking = false;
            boolean firstNotNull = supposing.get(0) != null;
            for (int i = 0; i < supposing.size(); ++i) {
                if (firstNotNull && Checker.check(supposing.get(i), e)) {
                    println("(%d) %s (Предположение %d)", position++, formula, i + 1);
                    memory.add(supposing.get(i));
                    formula = scan.next();
                    continue FormulaWorker;
                }
            }
            Expression copied = ExpressionParser.parse(formula);
            int i = Checker.checkSchemes(e, Aksioms.getParsedSchemes());
            if (i > 0) {
                println("(%d) %s (По схеме аксиом %d)", position++, formula, i);
                memory.add(copied);
                formula = scan.next();
                continue;
            }
            for (int j = 0; j < Aksioms.getParsedAksioms().size(); ++j) {
                if (Checker.check(Aksioms.getParsedAksioms().get(j), e)) {
                    println("(%d) %s (По %d аксиоме)", position++, formula, j);
                    memory.add(e);
                    formula = scan.next();
                    continue FormulaWorker;
                }
            }
            if (Checker.checkAksioms(e)) {
                println("(%d) %s (По следствию из схем аксиом)", position++, formula);
                memory.add(copied);
                formula = scan.next();
                continue;
            }
            Checker.any = false;
            int ind = Checker.checkAny(e, memory);
            if (ind > -1) {
                println("(%d) %s (Квантор всеобщности)", position++, formula);
                memory.add(e);
                formula = scan.next();
                continue;
            }
            e = Expressions.copy(copied);
            Checker.exs = false;
            ind = Checker.checkExistence(e, memory);
            if (ind > -1) {
                println("(%d) %s (Квантор существования)", position++, formula);
                memory.add(e);
                formula = scan.next();
                continue;
            }
            e = Expressions.copy(copied);
            int[] mp = checkMP(e);
            if (mp != null) {
                println("(%d) %s (Modus Ponens %d, %d)", position++, formula, ++mp[0], ++mp[1]);
                memory.add(e);
                formula = scan.next();
                continue;
            }
            println("(%d) %s вывод некорректен начиная с формулы %d", position, formula, position);
            if (Checker.any || Checker.exs) {
                String s1, s2;
                if (Checker.any) {
                    s1 = ((Quantor) ((BinaryOperation) e).getSecond()).getVariable().getName();
                    s2 = (((BinaryOperation) e).getFirst()).toString();
                } else {
                    s1 = ((Quantor) ((BinaryOperation) e).getFirst()).getVariable().getName();
                    s2 = (((BinaryOperation) e).getSecond()).toString();
                }
                println("переменная %s входит свободно в формулу %s", s1, s2);
            }
            if (Checker.checking) {
                Expression e1 = null, e2 = null;
                if (e.getType() == IMPLICATION) {
                    BinaryOperation bo = (BinaryOperation) e;
                    OperationType type = bo.getFirst().getType();
                    if (type == ANY) {
                        Quantor q = (Quantor) bo.getFirst();
                        Checker.checkEquality(bo.getSecond(), q.getExpression(), dict);
                        e1 = dict.get((String) dict.keySet().toArray()[0]);
                        e2 = q.getVariable();
                    } else if (type == EXIST) {
                        Quantor q = (Quantor) bo.getSecond();
                        Checker.checkEquality(bo.getFirst(), q.getExpression(), dict);
                        e1 = dict.get(q.getVariable().getName());
                        e2 = q.getVariable();
                    }
                }
                println("терм %d не свободен для подстановки в формулу %s вместо переменной %s", e1, formula, e2);
            }
            return;
        }
        deduct();
    }

    private void println(String s, Object... args) {
        out.println(String.format(s, args));
    }

    private void deduct() {
        if (out != null) {
            out.close();
        }
        try {
            scan = new FastScanner("input.txt");
            out = new PrintWriter(new File("deduction.txt"));
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        try {
            new Deductor().deduct(header, induction).forEach(out::println);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private int[] checkMP(Expression e) {
        for (int i = memory.size() - 1; i >= 0; --i) {
            Expression es = memory.get(i);
            if (es.getType() == Expressions.OperationType.IMPLICATION && Checker.check(e, ((BinaryOperation) es).getSecond())) {
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

    public static void main(String[] args) {
        new Launcher();
    }
}
