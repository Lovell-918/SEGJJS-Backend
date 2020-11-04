package edu.nju.se.teamnamecannotbeempty.batch.job.worker;

import edu.nju.se.teamnamecannotbeempty.data.domain.*;
import edu.nju.se.teamnamecannotbeempty.data.repository.AA_CooperateDao;
import edu.nju.se.teamnamecannotbeempty.data.repository.AuthorAffiliationYearDao;
import edu.nju.se.teamnamecannotbeempty.data.repository.AuthorDao;
import edu.nju.se.teamnamecannotbeempty.data.repository.PaperDao;
import edu.nju.se.teamnamecannotbeempty.data.repository.duplication.DuplicateAuthorDao;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuthorDupWorker {
    private final DuplicateAuthorDao duplicateAuthorDao;
    private final AuthorDao authorDao;
    private final AuthorPopWorker authorPopWorker;
    private final PaperDao paperDao;
    private final AuthorAffiliationYearDao authorAffiliationYearDao;
    private final AA_CooperateDao aa_cooperateDao;

    @Autowired
    public AuthorDupWorker(DuplicateAuthorDao duplicateAuthorDao, AuthorDao authorDao, AuthorPopWorker authorPopWorker, PaperDao paperDao,AuthorAffiliationYearDao authorAffiliationYearDao,AA_CooperateDao aa_cooperateDao) {
        this.authorAffiliationYearDao=authorAffiliationYearDao;
        this.duplicateAuthorDao = duplicateAuthorDao;
        this.authorDao = authorDao;
        this.authorPopWorker = authorPopWorker;
        this.paperDao=paperDao;
        this.aa_cooperateDao=aa_cooperateDao;
    }

    @Async
    public void generateAuthorDup() {
        ArrayListValuedHashMap<Long, Long> cache = new ArrayListValuedHashMap<>();
        class TempAuthor {
            final Author author;
            final String[] split_LCN;

            public TempAuthor(Author author, String[] split_LCN) {
                this.author = author;
                this.split_LCN = split_LCN;
                for (int i = 0; i < split_LCN.length; i++) {
                    split_LCN[i] = split_LCN[i].intern();
                }
            }
        }
        List<TempAuthor> all = authorDao.findAll().parallelStream()
                .map(author ->
                        new TempAuthor(author, author.getLowerCaseName().split(" "))
                ).collect(Collectors.toList());
        //名字首字母的映射缓存，搭配下面的方法就不用去数据库查一遍了
        ArrayListValuedHashMap<Character, TempAuthor> firstLetterMap = new ArrayListValuedHashMap<>();
        all.parallelStream().collect(Collectors.groupingBy(
                author -> author.split_LCN[0].charAt(0))
        ).forEach(firstLetterMap::putAll);

        all.forEach(author -> {
            String[] split_LCN = author.split_LCN;
            if (split_LCN.length > 1) {
                Long authorId = author.author.getId();
                // 名的首字母相同且姓相同
                firstLetterMap.get(split_LCN[0].charAt(0)).parallelStream().filter(
                        tempAuthor -> {
                            String lastName = split_LCN[split_LCN.length - 1];
                            return tempAuthor.split_LCN[tempAuthor.split_LCN.length - 1].equals(lastName) &&
                                    !tempAuthor.author.getId().equals(authorId);
                        }
                        // 被注释掉的方法性能太差
                        // 一是String.format性能差
                        // 二是Pattern.matches性能也差，而且大材小用
//                        nameIsLikeAndIdIsNot(
//                                tempAuthor.author,
//                                //String.format("^%c.* %s$", firstPrefix, lastName)
//                                "^" + firstPrefix + ".* " + lastName + "$",
//                                authorId
//                        )
                ).collect(Collectors.toList()).forEach(
                        suspect -> {
                            Long suspectId = suspect.author.getId();
                            if (!cache.containsKey(suspectId) ||
                                    (cache.containsKey(suspectId) && !cache.get(suspectId).contains(authorId))) {
                                // 如果已经存在a-b，b-a不会被加入以防止成环
                                if (isSimilar(split_LCN, suspect.split_LCN)) {
                                    cache.put(authorId, suspectId);
                                    duplicateAuthorDao.save(new DuplicateAuthor(author.author, suspect.author));
                                }
                            }
                        }
                );
            }
        });
        List<DuplicateAuthor> dups=duplicateAuthorDao.findAll();
        Map<Long,Long> son_idTofather_id=new HashMap();
        for(int i=0;i<dups.size();i++){
            son_idTofather_id.put(dups.get(i).getSon().getId(),dups.get(i).getFather().getId());
        }
        List<Paper> papers=paperDao.findAll();
        for(Paper paper : papers){
            List<Author_Affiliation> author_affiliations=paper.getAa();
            for(Author_Affiliation author_affiliation : author_affiliations){
                Author author=author_affiliation.getAuthor();
                if(author==null)
                    continue;
                if(son_idTofather_id.containsKey(author.getId())){
                    author_affiliation.setAuthor(authorDao.findById(son_idTofather_id.get(author.getId())).get());
                }
            }
            paper.setAa(author_affiliations);
        }

        List<AuthorAffiliationYear> authorAffiliationYears=authorAffiliationYearDao.findAll();
        for(AuthorAffiliationYear authorAffiliationYear : authorAffiliationYears){
            if(son_idTofather_id.containsKey(authorAffiliationYear.getAuthor().getId())){
                authorAffiliationYear.setAuthor(authorDao.findById(son_idTofather_id.get(authorAffiliationYear.getAuthor().getId())).get());
            }
        }
        authorAffiliationYearDao.saveAll(authorAffiliationYears);

        List<AA_Cooperate> aa_cooperates=aa_cooperateDao.findAll();
        for(AA_Cooperate aa_cooperate:aa_cooperates){
            if(son_idTofather_id.containsKey(aa_cooperate.getAuthor1().getId())){
                aa_cooperate.setAuthor1(authorDao.findById(son_idTofather_id.get(aa_cooperate.getAuthor1().getId())).get());
            }
            if(son_idTofather_id.containsKey(aa_cooperate.getAuthor2().getId())){
                aa_cooperate.setAuthor2(authorDao.findById(son_idTofather_id.get(aa_cooperate.getAuthor2().getId())).get());
            }
        }
        aa_cooperateDao.saveAll(aa_cooperates);
        paperDao.saveAll(papers);
        logger.info("Done generate duplicate authors");
    }

//    private boolean nameIsLikeAndIdIsNot(Author author, String regex, Long id) {
//        return Pattern.matches(regex, author.getLowerCaseName()) && !Objects.equals(author.getId(), id);
//    }

    /**
     * 判断两个作者名字的中间部分的词组是否相似
     * 相似的依据是：较短词组中的每个词（有可能是缩写）在较长词组中都有以它开头的词存在
     *
     * @param parts        一个词组
     * @param suspectParts 另一个词组
     * @return 是否相似
     */
    private boolean isSimilar(String[] parts, String[] suspectParts) {
        boolean partsIsLess = parts.length < suspectParts.length;
        String[] less = partsIsLess ? parts : suspectParts;
        String[] more = partsIsLess ? suspectParts : parts;
        boolean isSimilar = true;
        for (int i = 1; i < less.length - 1; i++) {
            less[i] = less[i].replace(".", "");
            for (int j = i; j < more.length - 1; j++) {
                if (more[j].startsWith(less[i])) break;
                isSimilar = false;
            }
            if (!isSimilar) break;
        }
        return isSimilar;
    }

    @Async
    public void refresh(Date date) {
        duplicateAuthorDao.findByUpdatedAtAfter(date).forEach(dup -> {
            Author son = dup.getSon();
            authorPopWorker.refreshPop(son);
            if (dup.getClear() && !dup.getSon().getId().equals(dup.getSon().getActual().getId())) {
                authorPopWorker.minusPop(son, son);
            } else if (!dup.getClear()) {
                authorPopWorker.refreshPop(dup.getFather());
            }
        });
    }

    private static final Logger logger = LoggerFactory.getLogger(AuthorDupWorker.class);
}
