package ru.luvas.ml2.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import ru.luvas.ml2.Expressions.Expression;
import ru.luvas.ml2.ExpressionParser;

/**
 *
 * @author RINES <iam@kostya.sexy>
 */
public class Aksioms {

    private final static List<String> schemes = Arrays.asList(
            "a->b->a",
            "(a->b)->(a->(b->c))->(a->c)",
            "a->b->a&b",
            "a&b->a",
            "a&b->b",
            "a->a|b",
            "b->a|b",
            "(a->c)->(b->c)->(a|b->c)",
            "(a->b)->(a->!b)->!a",
            "!!a->a"
    ), aksioms = Arrays.asList(
            "a=b->a'=b'",
            "(a=b)->(a=c)->(b=c)",
            "a'=b'->a=b",
            "!a'=0",
            "a+b'=(a+b)'",
            "a+0=a",
            "a*0=0",
            "a*b'=a*b+a"
    );

    @Getter
    private final static List<Expression> parsedSchemes = new ArrayList<>(), parsedAksioms = new ArrayList<>();

    static {
        schemes.forEach(s -> parsedSchemes.add(ExpressionParser.parse(s)));
        aksioms.forEach(a -> parsedAksioms.add(ExpressionParser.parse(a)));
    }

}
