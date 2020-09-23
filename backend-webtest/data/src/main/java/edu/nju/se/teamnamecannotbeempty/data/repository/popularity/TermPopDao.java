package edu.nju.se.teamnamecannotbeempty.data.repository.popularity;

import edu.nju.se.teamnamecannotbeempty.data.domain.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TermPopDao extends JpaRepository<Term.Popularity, Long> {
    /**
     * 获得按照热度（活跃度）降序排列的前20名
     *
     * @return 返回至多20条记录，按照热度（活跃度）排序
     * @前置条件 无
     * @后置条件 无
     */
    default List<Term.Popularity> findTop20ByOrderByPopularityDesc() {
        return findTop20ByYearIsNullOrderByPopularityDesc();
    }

    List<Term.Popularity> findTop20ByYearIsNullOrderByPopularityDesc();

    /**
     * 查询一个作者的研究方向及其热度
     * 一个作者的研究方向，是指他发表的论文的研究方向集合之并集
     *
     * @param id 作者id
     * @return 作者的研究方向热度对象列表
     * @前置条件 id不为null
     * @后置条件 无
     */
//    @Query("select distinct tp from term_popularity tp where exists (" +
//            "select p from Paper p inner join p.author_keywords pas inner join p.aa aa " +
//            "where tp.term.id = pas.id and aa.author.id = ?1) and tp.year is null")
    @Query(nativeQuery = true,
            value = "SELECT distinct tp.id, tp.term_id, tp.popularity, tp.year " +
                    "FROM se3.term_popularity tp " +
                    "LEFT JOIN se3.papers_author_keywords pak ON pak.author_keywords_id = tp.term_id " +
                    "LEFT JOIN se3.paper_aa aa ON pak.paper_id = aa.paper_id " +
                    "WHERE tp.year IS NULL AND aa.author_id = ?1")
    List<Term.Popularity> getTermPopByAuthorID(Long id);

    /**
     * 查询一个机构的研究方向及其热度
     * 一个机构的研究方向，是指机构名下的论文的研究方向集合之并集
     *
     * @param id 作者id
     * @return 机构的研究方向热度对象列表
     * @前置条件 id不为null
     * @后置条件 无
     */
//    @Query("select distinct tp from term_popularity tp where exists (" +
//            "select p from Paper p inner join p.author_keywords pas inner join p.aa aa " +
//            "where tp.term.id = pas.id and aa.affiliation.id = ?1) and tp.year is null")
    @Query(nativeQuery = true,
            value = "SELECT distinct tp.id, tp.term_id, tp.popularity, tp.year " +
                    "FROM se3.term_popularity tp " +
                    "LEFT JOIN se3.papers_author_keywords pak ON pak.author_keywords_id = tp.term_id " +
                    "LEFT JOIN se3.paper_aa aa ON pak.paper_id = aa.paper_id " +
                    "WHERE tp.year IS NULL AND aa.affiliation_id = ?1")
    List<Term.Popularity> getTermPopByAffiID(Long id);

    /**
     * 查询一篇论文的研究方向及其热度
     *
     * @param id 论文id
     * @return 论文的研究方向热度对象列表
     * @前置条件 id不为null
     * @后置条件 无
     */
//    @Query("select distinct tp from term_popularity tp where exists (" +
//            "select p from Paper p inner join p.author_keywords pas " +
//            "where tp.term.id = pas.id and p.id = ?1) and tp.year is null")
    @Query(nativeQuery = true,
            value = "SELECT distinct tp.id, tp.term_id, tp.popularity, tp.year " +
                    "FROM se3.term_popularity tp " +
                    "LEFT JOIN se3.papers_author_keywords pak ON pak.author_keywords_id = tp.term_id " +
                    "WHERE tp.year IS NULL AND pak.paper_id = ?1")
    List<Term.Popularity> getTermPopByPaperID(Long id);

    /**
     * 查询会议的研究方向及其热度
     *
     * @param id 会议id
     * @return 会议的研究方向热度对象列表
     * @前置条件 id不为null
     * @后置条件 无
     */
//    @Query("select distinct tp from term_popularity tp where exists (" +
//            "select p from Paper p inner join p.author_keywords pas " +
//            "where tp.term.id = pas.id and p.conference.id = ?1) and tp.year is null")
    @Query(nativeQuery = true,
            value = "SELECT distinct tp.id, tp.term_id, tp.popularity, tp.year " +
                    "FROM se3.term_popularity tp " +
                    "LEFT JOIN se3.papers_author_keywords pak ON pak.author_keywords_id = tp.term_id " +
                    "LEFT JOIN se3.papers p ON pak.paper_id = p.id " +
                    "WHERE tp.year IS NULL AND p.conference_id = ?1")
    List<Term.Popularity> getTermPopByConferenceID(Long id);

    /**
     * 获取一个研究方向的热度
     *
     * @param id 研究方向id
     * @return 通过Optional包装的热度对象
     * @前置条件 id不为null
     * @后置条件 如果有与参数所给的id对应的数据，则Optional.get可获得该对象；否则Optional.isPresent==false
     */
    default Optional<Term.Popularity> getDistinctByTerm_Id(Long id) {
        return getByTerm_IdAndYearIsNull(id);
    }

    Optional<Term.Popularity> getByTerm_IdAndYearIsNull(Long id);
}
