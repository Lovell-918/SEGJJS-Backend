package edu.nju.se.teamnamecannotbeempty.data.repository.popularity;

import edu.nju.se.teamnamecannotbeempty.data.domain.Paper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface PaperPopDao extends CrudRepository<Paper.Popularity, Long> {

    /**
     * 通过论文id获得论文的热度
     *
     * @param id 论文id
     * @return 论文热度对象
     * @前置条件 id不为null
     * @后置条件 无
     */
    default Optional<Paper.Popularity> getByPaper_Id(Long id) {
        return getByPaper_IdAndYearIsNull(id);
    }

    Optional<Paper.Popularity> getByPaper_IdAndYearIsNull(Long id);

    /**
     * 查找某作者的代表作（即按论文热度排序）
     *
     * @param id 作者id
     * @return 按热度降序的作者论文
     * @前置条件 id不为null
     * @后置条件 无
     */
    default List<Paper.Popularity> findTopPapersByAuthorId(Long id) {
        return findTopPapersByAuthorIdAndYearIsNullPaged(id, Pageable.unpaged());
    }

    /**
     * 查找某作者代表作（即按论文热度排序）的某分页
     *
     * @param id   作者id
     * @param page 分页
     * @return 热度降序的分页的论文热度
     * @since 迭代三
     */
    @Query(nativeQuery = true,
            value = "select pp.id, pp.paper_id, pp.popularity, pp.year from se3.paper_aa aa " +
                    "right join paper_popularity pp on aa.paper_id = pp.paper_id " +
                    "where aa.author_id = ?1 and pp.year is null " +
                    "order by pp.popularity desc")
    List<Paper.Popularity> findTopPapersByAuthorIdAndYearIsNullPaged(Long id, Pageable page);

    /**
     * 查找机构的代表作
     *
     * @param id 机构id
     * @return 按热度降序的机构论文
     * @前置条件 id不为null
     * @后置条件 无
     */
    default List<Paper.Popularity> findTopPapersByAffiId(Long id) {
        return findTopPapersByAffiIdAndYearIsNullPaged(id, Pageable.unpaged());
    }

    /**
     * @since 迭代三
     */
    @Query(nativeQuery = true,
            value = "select pp.id, pp.paper_id, pp.popularity, pp.year from se3.paper_aa aa " +
                    "right join paper_popularity pp on aa.paper_id = pp.paper_id " +
                    "where aa.affiliation_id = ?1 and pp.year is null " +
                    "order by pp.popularity desc")
    List<Paper.Popularity> findTopPapersByAffiIdAndYearIsNullPaged(Long id, Pageable page);

    /**
     * 查找某个会议的最热门文章
     *
     * @param id 会议id
     * @return 按热度降序的会议论文
     * @前置条件 id不为null
     * @后置条件 无
     */
    default List<Paper.Popularity> findTopPapersByConferenceId(Long id) {
        return findTopPapersByConferenceIdAndYearIsNullPaged(id, Pageable.unpaged());
    }

    /**
     * @since 迭代三
     */
    @Query("select pp from paper_popularity pp " +
            "where pp.paper.conference.id = ?1 and pp.year is null order by pp.popularity desc")
    List<Paper.Popularity> findTopPapersByConferenceIdAndYearIsNullPaged(Long id, Pageable page);

    /**
     * 获得作者某一年所发表论文的热度之和
     *
     * @param authorId 作者id
     * @param year 年份
     * @return 热度之和
     */
    @Query("select coalesce(sum(pp.popularity),0.0) from paper_popularity pp " +
            "inner join pp.paper p inner join p.aa aa " +
            "where aa.author.id = ?1 and pp.year = ?2")
    Double getPopSumByAuthorIdAndYear(Long authorId, Integer year);

    /**
     * 获得机构的论文热度之和
     *
     * @param affiId 机构id
     * @param year 年份
     * @return 机构论文热度之和
     */
    @Query("select coalesce(sum(pp.popularity),0.0) from paper_popularity pp " +
            "inner join pp.paper p inner join p.aa aa " +
            "where aa.affiliation.id = ?1 and pp.year = ?2")
    Double getPopSumByAffiIdAndYear(Long affiId, Integer year);

    /**
     * 获得作者在某个研究方向上的论文热度之和（权重）
     *
     * @param authorId  作者id
     * @param keywordId 研究方向id
     * @return 作者在研究方向上的权重
     * @前置条件 参数都不为null
     * @后置条件 无
     */
//    @Query("select coalesce(sum(pp.popularity),0.0) from paper_popularity pp inner join pp.paper p " +
//            "where exists (select 1 from p.aa aa where aa.author.id = ?1) and " +
//            "exists (select 1 from p.author_keywords ak where ak.id = ?2) and pp.year is null")
    @Query(nativeQuery = true,
            value = "SELECT coalesce(sum(pp.popularity),0) FROM se3.paper_popularity pp " +
                    "left join se3.paper_aa aa on pp.paper_id = aa.paper_id " +
                    "left join se3.papers_author_keywords pak on pp.paper_id = pak.paper_id " +
                    "where aa.author_id = ?1 and pak.author_keywords_id = ?2 and pp.year is null")
    Double getWeightByAuthorOnKeyword(Long authorId, Long keywordId);

    /**
     * 获得机构在某个研究方向上的论文热度之和
     *
     * @param affiId    机构id
     * @param keywordId 研究方向id
     * @return 机构在研究方向上的权重
     * @前置条件 参数都不为null
     * @后置条件 无
     */
//    @Query("select coalesce(sum(pp.popularity),0.0) from paper_popularity pp inner join pp.paper p " +
//            "where exists (select 1 from p.aa aa where aa.affiliation.id = ?1) and " +
//            "exists (select 1 from p.author_keywords ak where ak.id = ?2) and pp.year is null")
    @Query(nativeQuery = true,
            value = "SELECT coalesce(sum(pp.popularity),0) FROM se3.paper_popularity pp " +
                    "left join se3.paper_aa aa on pp.paper_id = aa.paper_id " +
                    "left join se3.papers_author_keywords pak on pp.paper_id = pak.paper_id " +
                    "where aa.affiliation_id = ?1 and pak.author_keywords_id = ?2 and pp.year is null")
    Double getWeightByAffiOnKeyword(Long affiId, Long keywordId);

    /**
     * 获得会议在某研究方向上的论文热度之和
     *
     * @param cfrId     会议id
     * @param keywordId 研究方向id
     * @return 会议在研究方向上的权重
     * @前置条件 无
     * @后置条件 无
     */
//    @Query("select coalesce(sum(pp.popularity),0.0) from paper_popularity pp inner join pp.paper p " +
//            "where p.conference.id = ?1 and " +
//            "exists (select 1 from p.author_keywords ak where ak.id = ?2) and pp.year is null")
    @Query(nativeQuery = true,
            value = "SELECT coalesce(sum(pp.popularity),0) FROM se3.paper_popularity pp " +
                    "left join se3.papers p on pp.paper_id = p.id " +
                    "left join se3.papers_author_keywords pak on pp.paper_id = pak.paper_id " +
                    "where p.conference_id = ?1 and pak.author_keywords_id = ?2 and pp.year is null")
    Double getWeightByConferenceOnKeyword(Long cfrId, Long keywordId);
}
