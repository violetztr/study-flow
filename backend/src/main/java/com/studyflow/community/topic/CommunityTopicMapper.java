package com.studyflow.community.topic;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommunityTopicMapper extends BaseMapper<CommunityTopic> {
    @Update("UPDATE community_topics SET post_count = post_count + 1 WHERE id = #{topicId}")
    void incrementPostCount(@Param("topicId") Long topicId);

    @Update("UPDATE community_topics SET post_count = CASE WHEN post_count > 0 THEN post_count - 1 ELSE 0 END WHERE id = #{topicId}")
    void decrementPostCount(@Param("topicId") Long topicId);
}
