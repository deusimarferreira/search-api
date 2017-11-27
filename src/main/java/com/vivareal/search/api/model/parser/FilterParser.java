package com.vivareal.search.api.model.parser;

import com.vivareal.search.api.model.parser.ValueParser.GeoPoint.Type;
import com.vivareal.search.api.model.query.Filter;
import org.jparsec.Parser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.vivareal.search.api.model.query.RelationalOperator.*;
import static org.jparsec.Parsers.or;
import static org.jparsec.Parsers.sequence;

@Component
public class FilterParser {

    private final Parser<Filter> normalParser;
    private final Parser<Filter> likeParser;
    private final Parser<Filter> rangeParser;
    private final Parser<Filter> viewportParser;
    private final Parser<Filter> polygonParser;
    private final Parser<Filter> filterParser;

    @Autowired
    public FilterParser(FieldParser fieldParser, OperatorParser operatorParser, ValueParser valueParser) {
        normalParser = sequence(fieldParser.get(), operatorParser.exact(DIFFERENT, EQUAL, GREATER_EQUAL, GREATER, IN, LESS_EQUAL, LESS), valueParser.get(), Filter::new).label("filter");
        likeParser = sequence(fieldParser.get(), operatorParser.exact(LIKE), valueParser.getLikeValue(), Filter::new).label("LIKE filter");
        rangeParser = sequence(fieldParser.get(), operatorParser.exact(RANGE), valueParser.getRangeValue(), Filter::new).label("RANGE filter");
        viewportParser = sequence(fieldParser.get(), operatorParser.exact(VIEWPORT), valueParser.getGeoPointValue(Type.VIEWPORT), Filter::new).label("VIEWPORT filter");
        polygonParser = sequence(fieldParser.get(), operatorParser.exact(POLYGON), valueParser.getGeoPointValue(Type.POLYGON), Filter::new).label("POLYGON filter");

        filterParser = or(normalParser, rangeParser, likeParser, viewportParser, polygonParser);
    }


    Parser<Filter> get() {
        return filterParser;
    }
}
