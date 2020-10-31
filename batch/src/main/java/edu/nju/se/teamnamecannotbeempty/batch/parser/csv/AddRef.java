package edu.nju.se.teamnamecannotbeempty.batch.parser.csv;

import edu.nju.se.teamnamecannotbeempty.batch.parser.csv.intermediate.PaperImd;
import edu.nju.se.teamnamecannotbeempty.batch.parser.csv.intermediate.RefImd;
import edu.nju.se.teamnamecannotbeempty.data.domain.Paper;
import edu.nju.se.teamnamecannotbeempty.data.domain.Ref;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddRef {

    public static void addRef(List<PaperImd> paperImdList, Map<String, Paper> paperMap){
        for(PaperImd paperImd:paperImdList){
            List<RefImd> refImdList=paperImd.getReferences();
            Paper paper=paperMap.get(paperImd.getTitle());
            for(RefImd refImd:refImdList){
                Ref ref=new Ref(refImd.getTitle());
                ref.setReferee(paper);
                Paper newPaper=paperMap.get(refImd.getTitle());
                if(newPaper==null){
                    newPaper=refImd.toPaper();
                    paperMap.put(refImd.getTitle(),newPaper);
                }
                paper.addRef(ref);
            }
        }
    }
}