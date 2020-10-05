package edu.nju.se.teamnamecannotbeempty.backend.serviceImpl.search.mode;

import edu.nju.se.teamnamecannotbeempty.backend.service.search.SearchMode;
import edu.nju.se.teamnamecannotbeempty.data.domain.Paper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.highlight.Highlighter;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.SimpleQueryStringMatchingContext;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.springframework.stereotype.Component;

import static edu.nju.se.teamnamecannotbeempty.backend.serviceImpl.search.SearchMappingFactory.getFieldName_author;
import static edu.nju.se.teamnamecannotbeempty.backend.serviceImpl.search.SearchMappingFactory.getFieldName_searchYear;

@Component("Author")
public class SearchByAuthor extends SearchMode {
    @Override
    public TermMatchingContext getFieldsBaseOnKeyword(QueryBuilder queryBuilder) {
        return queryBuilder.keyword()
                .onField(getFieldName_searchYear()).boostedTo(2f)
                .andField(getFieldName_author());
    }

    @Override
    public SimpleQueryStringMatchingContext getFieldsBaseOnSQS(QueryBuilder queryBuilder) {
        return queryBuilder.simpleQueryString().onFields(getFieldName_author(), getFieldName_searchYear());
    }

    @Override
    public void highlight(Highlighter highlighter, Analyzer analyzer, Paper paper) {
        highlightAuthor(paper, highlighter, analyzer);
        highlightYear(paper, highlighter, analyzer);
    }
}
