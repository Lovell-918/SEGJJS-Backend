package edu.nju.se.teamnamecannotbeempty.backend.serviceImpl.visualization;

import edu.nju.se.teamnamecannotbeempty.backend.config.parameter.EntityMsg;
import edu.nju.se.teamnamecannotbeempty.backend.vo.*;
import edu.nju.se.teamnamecannotbeempty.data.domain.*;
import edu.nju.se.teamnamecannotbeempty.data.repository.*;
import edu.nju.se.teamnamecannotbeempty.data.repository.popularity.PaperPopDao;
import edu.nju.se.teamnamecannotbeempty.data.repository.popularity.TermPopDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AcademicEntityFetch {

    private final AffiliationDao affiliationDao;
    private final AuthorDao authorDao;
    private final ConferenceDao conferenceDao;
    private final PaperDao paperDao;
    private final TermPopDao termPopDao;
    private final EntityMsg entityMsg;
    private final PaperPopDao paperPopDao;
    private final TermDao termDao;
    private final FetchForCache fetchForCache;

    @Autowired
    public AcademicEntityFetch(AffiliationDao affiliationDao, AuthorDao authorDao, ConferenceDao conferenceDao,
                               PaperDao paperDao, EntityMsg entityMsg,
                               TermPopDao termPopDao, PaperPopDao paperPopDao, TermDao termDao,
                               FetchForCache fetchForCache) {
        this.affiliationDao = affiliationDao;
        this.authorDao = authorDao;
        this.conferenceDao = conferenceDao;
        this.paperDao = paperDao;
        this.entityMsg = entityMsg;
        this.termPopDao = termPopDao;
        this.paperPopDao = paperPopDao;
        this.termDao = termDao;
        this.fetchForCache = fetchForCache;
    }

    @Cacheable(value = "getAcademicEntity", key = "#p0+'_'+#p1")
    public AcademicEntityVO getAcademicEntity(long id, int type) {
        AcademicEntityVO academicEntityVO = null;
        if (type == entityMsg.getAuthorType()) academicEntityVO = authorsEntity(id);
        else if (type == entityMsg.getAffiliationType()) academicEntityVO = affiliationEntity(id);
        else if (type == entityMsg.getConferenceType()) academicEntityVO = conferenceEntity(id);
        return academicEntityVO;
    }

    private AcademicEntityVO authorsEntity(long id) {

        //获取作者的别名列表
        List<Long> aliasIdList = getAllAliasIdsOfAuthor(id, new ArrayList<>()).stream().distinct()
                .collect(Collectors.toList());
        List<Affiliation> affiliationList = aliasIdList.stream().flatMap(aliasId ->
                affiliationDao.getAffiliationsByAuthor(aliasId).stream())
                .distinct().collect(Collectors.toList());
        List<Conference> conferenceList = aliasIdList.stream().flatMap(aliasId ->
                conferenceDao.getConferencesByAuthor(aliasId).stream())
                .distinct().collect(Collectors.toList());

        List<AcademicEntityItem> affiEntityItems = generateAffiEntityItems(affiliationList);
        List<AcademicEntityItem> conferenceEntityItems = generateConferenceEntityItems(conferenceList);

        //生成研究方向云图
        HashMap<Long, Double> termHashMap = new HashMap<>();
        aliasIdList.forEach(aliasId -> termPopDao.getTermPopByAuthorID(aliasId).forEach(
                termPop -> {
                    Long termId = termPop.getTerm().getId();
                    if (termHashMap.containsKey(termId)) {
                        Double d = termHashMap.get(termId);
                        termHashMap.put(termId, d + paperPopDao.getWeightByAuthorOnKeyword(
                                aliasId, termPop.getTerm().getId()));
                    } else {
                        termHashMap.put(termId, paperPopDao.getWeightByAuthorOnKeyword(
                                aliasId, termPop.getTerm().getId()));
                    }
                }));

        List<TermItem> termItems = termHashMap.entrySet().stream().map(
                en -> new TermItem(en.getKey(), termDao.findById(
                        en.getKey()).orElseGet(Term::new).getContent(), en.getValue())
        ).collect(Collectors.toList());

        //生成按年份的研究方向列表
        List<Paper> allPapers = new ArrayList<>();
        aliasIdList.forEach(aliasId -> allPapers.addAll(fetchForCache.getAllPapersByAuthor(aliasId)));
        List<YearlyTerm> yearlyTerms = getYearlyTermList(allPapers);

        //生成代表作
        List<Paper> paperList = aliasIdList.stream().flatMap(aliasId ->
                fetchForCache.getAllPapersByAuthor(aliasId).stream())
                .distinct().collect(Collectors.toList());
        List<SimplePaperVO> simplePaperVOS = paperList.stream().map(
                SimplePaperVO::new).collect(Collectors.toList());
        if(simplePaperVOS.size()>12) simplePaperVOS=simplePaperVOS.subList(0,12);

        //生成总引用数
        int sumCitation = aliasIdList.stream().mapToInt(aliasId ->
                (int) paperDao.getCitationByAuthorId(aliasId)).sum();

        //生成热度变化字符串
        List<PopByYear> popByYearList = aliasIdList.stream().flatMap(authorId ->
                authorDao.findById(authorId).orElseGet(Author::new).getPops().stream().filter(
                        pop->pop.getYear()!=null)).collect(
                Collectors.groupingBy(Author.Popularity::getYear)
        ).entrySet().stream().map(en -> new PopByYear(en.getKey(), en.getValue().stream().mapToDouble(
                Author.Popularity::getPopularity
        ).sum())).sorted(Comparator.comparing(PopByYear::getYear)).collect(Collectors.toList());
        String popTrend = generatePopTrend(popByYearList);

        return new AcademicEntityVO(entityMsg.getAuthorType(), id,
                authorDao.findById(id).orElseGet(Author::new).getActual().getName(),
                sumCitation, null, affiEntityItems, conferenceEntityItems, termItems,
                simplePaperVOS, yearlyTerms, popTrend);
    }

    private AcademicEntityVO affiliationEntity(long id) {
        Affiliation affiliation=affiliationDao.getOne(id);
        if(affiliation.getName().equals("NA")) return null;

        //生成机构的别名列表
        List<Long> aliasIdList = getAllAliasIdsOfAffi(id, new ArrayList<>()).stream().distinct()
                .collect(Collectors.toList());
        List<Author> authorList = aliasIdList.stream().flatMap(aliasId ->
                authorDao.getAuthorsByAffiliation(aliasId).stream())
                .distinct().collect(Collectors.toList());
        List<Conference> conferenceList = aliasIdList.stream().flatMap(aliasId ->
                conferenceDao.getConferencesByAffiliation(aliasId).stream())
                .distinct().collect(Collectors.toList());

        List<AcademicEntityItem> authorEntityItems = generateAuthorEntityItems(authorList);
        List<AcademicEntityItem> conferenceEntityItems = generateConferenceEntityItems(conferenceList);

        //生成研究方向云图
        HashMap<Long, Double> termHashMap = new HashMap<>();
        aliasIdList.forEach(aliasId -> termPopDao.getTermPopByAffiID(aliasId).forEach(
                termPop -> {
                    Long termId = termPop.getTerm().getId();
                    if (termHashMap.containsKey(termId)) {
                        Double d = termHashMap.get(termId);
                        termHashMap.put(termId, d + paperPopDao.getWeightByAffiOnKeyword(aliasId,
                                termPop.getTerm().getId()));
                    } else {
                        termHashMap.put(termId, paperPopDao.getWeightByAffiOnKeyword(aliasId,
                                termPop.getTerm().getId()));
                    }
                }));
        List<TermItem> termItems = termHashMap.entrySet().stream().map(
                en -> new TermItem(en.getKey(), termDao.findById(
                        en.getKey()).orElseGet(Term::new).getContent(), en.getValue())
        ).collect(Collectors.toList());

        //生成按年份的研究方向列表
        List<Paper> allPapers = new ArrayList<>();
        aliasIdList.forEach(aliasId -> allPapers.addAll(fetchForCache.getAllPapersByAffi(aliasId)));
        List<YearlyTerm> yearlyTerms = getYearlyTermList(allPapers);

        //生成代表作
        List<Paper> paperList = aliasIdList.stream().flatMap(aliasId ->
                fetchForCache.getAllPapersByAffi(aliasId).stream())
                .distinct().collect(Collectors.toList());
        List<SimplePaperVO> simplePaperVOS = paperList.stream().map(
                SimplePaperVO::new).collect(Collectors.toList());
        if(simplePaperVOS.size()>12) simplePaperVOS=simplePaperVOS.subList(0,12);

        //生成总引用数
        int sumCitation = aliasIdList.stream().mapToInt(aliasId ->
                (int) paperDao.getCitationByAffiId(aliasId)).sum();

        //生成热度变化字符串
        List<PopByYear> popByYearList = aliasIdList.stream().flatMap(affiId ->
                affiliationDao.findById(affiId).orElseGet(Affiliation::new).getPops().stream()
        .filter(pop->pop.getYear()!=null)).collect(
                Collectors.groupingBy(Affiliation.Popularity::getYear)
        ).entrySet().stream().map(en -> new PopByYear(en.getKey(), en.getValue().stream().mapToDouble(
                Affiliation.Popularity::getPopularity
        ).sum())).sorted(Comparator.comparing(PopByYear::getYear)).collect(Collectors.toList());
        String popTrend = generatePopTrend(popByYearList);

        return new AcademicEntityVO(entityMsg.getAffiliationType(), id, affiliationDao.findById(id).
                orElseGet(Affiliation::new).getActual().getName(),
                sumCitation, authorEntityItems, null, conferenceEntityItems, termItems,
                simplePaperVOS, yearlyTerms, popTrend);
    }

    private AcademicEntityVO conferenceEntity(long id) {
        Conference conference=conferenceDao.getOne(id);
        if(conference.getName().equals("NA")) return null;

        List<AcademicEntityItem> authorEntityItems = generateAuthorEntityItems(
                authorDao.getAuthorsByConference(id));
        List<AcademicEntityItem> affiEntityItems = generateAffiEntityItems(
                affiliationDao.getAffiliationsByConference(id));
        List<Term.Popularity> termPopularityList = termPopDao.getTermPopByConferenceID(id);

        List<TermItem> termItems = termPopularityList.stream().map(
                termPopularity -> new TermItem(termPopularity.getTerm().getId(),
                        termPopularity.getTerm().getContent(),
                        paperPopDao.getWeightByConferenceOnKeyword(id, termPopularity.getTerm().getId()))
        ).collect(Collectors.toList());

        List<Paper> allPapers = fetchForCache.getAllPapersByConference(id);
        List<YearlyTerm> yearlyTerms = getYearlyTermList(allPapers);

        List<SimplePaperVO> simplePaperVOS = fetchForCache.getAllPapersByConference(id)
                .stream().map(SimplePaperVO::new
        ).collect(Collectors.toList());
        if(simplePaperVOS.size()>12) simplePaperVOS=simplePaperVOS.subList(0,12);

        return new AcademicEntityVO(entityMsg.getConferenceType(), id, conferenceDao.findById(id).
                orElseGet(Conference::new).getName(), -1,
                authorEntityItems, affiEntityItems, null, termItems, simplePaperVOS,
                yearlyTerms, null);
    }

    private List<AcademicEntityItem> generateAuthorEntityItems(List<Author> authors) {
        List<AcademicEntityItem> academicEntityItems = authors.stream().map(
                author -> new AcademicEntityItem(entityMsg.getAuthorType(), author.getActual().getId(),
                        author.getActual().getName(), generatePopTrend(
                        author.getPops().stream()
                                .filter(pop->pop.getYear()!=null)
                                .map(pop -> new PopByYear(pop.getYear(), pop.getPopularity())).sorted(
                                Comparator.comparing(PopByYear::getYear)
                        ).collect(Collectors.toList())
                )))
                .collect(Collectors.toList());
        return academicEntityItems.size() > 15 ? academicEntityItems.subList(0, 15) : academicEntityItems;
    }

    private List<AcademicEntityItem> generateAffiEntityItems(List<Affiliation> affiliations) {
        List<AcademicEntityItem> academicEntityItems = affiliations.stream()
                .filter(affiliation -> !affiliation.getActual().getName().equals("NA"))
                .map(
                        affiliation -> new AcademicEntityItem(entityMsg.getAffiliationType(),
                                affiliation.getActual().getId(),
                                affiliation.getActual().getName(), generatePopTrend(
                                affiliation.getPops().stream()
                                        .filter(pop->pop.getYear()!=null)
                                        .map(pop -> new PopByYear(pop.getYear(),
                                                pop.getPopularity())).sorted(
                                        Comparator.comparing(PopByYear::getYear)
                                ).collect(Collectors.toList())
                        )))
                .collect(Collectors.toList());
        return academicEntityItems.size() > 15 ? academicEntityItems.subList(0, 15) : academicEntityItems;
    }

    private List<AcademicEntityItem> generateConferenceEntityItems(List<Conference> conferences) {
        List<AcademicEntityItem> academicEntityItems = conferences.stream()
                .filter(conference -> !conference.getName().equals("NA"))
                .map(
                conference -> new AcademicEntityItem(entityMsg.getConferenceType(), conference.getId(),
                        conference.getName(), null)) //会议没有热度
                .collect(Collectors.toList());
        return academicEntityItems.size() > 15 ? academicEntityItems.subList(0, 15) : academicEntityItems;
    }

    private List<SimplePaperVO> generateTopPapers(List<Paper.Popularity> paperPopularityList) {
        List<SimplePaperVO> simplePaperVOS = paperPopularityList.stream()
                .map(paprePopularity -> new SimplePaperVO(paprePopularity.getPaper()))
                .collect(Collectors.toList());
        return simplePaperVOS.size() > 5 ? simplePaperVOS.subList(0, 5) : simplePaperVOS;
    }

    public List<Long> getAllAliasIdsOfAuthor(long id, List<Long> results) {
        results.add(id);
        List<Author> aliasList = authorDao.getByAlias_Id(id);
        if (aliasList == null || aliasList.isEmpty()) {
            return results;
        }
        for (Author author : aliasList) {
            results.addAll(getAllAliasIdsOfAffi(author.getId(), new ArrayList<>()));
        }
        return results;
    }

    public List<Long> getAllAliasIdsOfAffi(long id, List<Long> results) {
        results.add(id);
        List<Affiliation> aliasList = affiliationDao.getByAlias_Id(id);
        if (aliasList == null || aliasList.isEmpty()) {
            return results;
        }
        for (Affiliation affiliation : aliasList) {
            results.addAll(getAllAliasIdsOfAffi(affiliation.getId(), new ArrayList<>()));
        }
        return results;
    }

    private List<YearlyTerm> getYearlyTermList(List<Paper> allPapers) {
        Map<Integer, List<Paper>> paperByYear = allPapers.stream().distinct().collect(
                Collectors.groupingBy(Paper::getYear));
        return paperByYear.entrySet().stream().map(
                en -> new YearlyTerm(en.getKey(), en.getValue().stream().flatMap(
                        paper -> fetchForCache.getTermPopByPaperID(paper.getId()).stream().map(
                                termPop -> new TermItem(termPop.getTerm().getId(),
                                        termPop.getTerm().getContent(), -1)
                        )
                ).distinct().collect(Collectors.toList()))
        ).collect(Collectors.toList());
    }

    private String generatePopTrend(List<PopByYear> popByYearList) {
        if (popByYearList.size() == 0) return null;
        StringBuilder sb = new StringBuilder();
        int beginYear = popByYearList.get(0).getYear();
        sb.append(beginYear);
        sb.append(" ");
        sb.append(popByYearList.get(0).getPop());
        beginYear++;
        for (int i = 1; i < popByYearList.size(); beginYear++) {
            sb.append(" ");
            if (popByYearList.get(i).getYear() == beginYear) {
                sb.append(popByYearList.get(i).getPop());
                i++;
            } else {
                sb.append(0);
            }
        }
        return sb.toString();
    }

    static class PopByYear {
        int year;
        double pop;

        public PopByYear(int year, double pop) {
            this.year = year;
            this.pop = pop;
        }

        int getYear() {
            return year;
        }

        double getPop() {
            return pop;
        }
    }
}
