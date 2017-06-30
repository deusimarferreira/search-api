package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.query.OrderOperator;
import com.vivareal.search.api.model.query.Sort;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;

public class SortParser {
    private static final Parser<Sort> SORT_PARSER = Parsers.sequence(FieldParser.getWithoutNot(), OperatorParser.ORDER_OPERATOR_PARSER.optional(OrderOperator.ASC), Sort::new).sepBy(Scanners.isChar(',').next(Scanners.WHITESPACES.skipMany())).label("sort").map(Sort::new);

    public static Parser<Sort> get() {
        return SORT_PARSER;
    }
}