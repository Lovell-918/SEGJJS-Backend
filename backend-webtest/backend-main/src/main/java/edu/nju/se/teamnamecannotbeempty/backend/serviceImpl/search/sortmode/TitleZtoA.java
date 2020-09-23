package edu.nju.se.teamnamecannotbeempty.backend.serviceImpl.search.sortmode;

import edu.nju.se.teamnamecannotbeempty.backend.service.search.SortMode;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.springframework.stereotype.Component;

@Component("Title Z-A")
public class TitleZtoA implements SortMode {
    private static Sort sort;

    public TitleZtoA() {
        sort = new Sort(
                new SortField("sortTitle", SortField.Type.STRING, true)
        );
    }

    @Override
    public Sort getSort() {
        return sort;
    }
}
