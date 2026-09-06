package com.raisesns.backend.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LikeMapper {

    @Insert("""
            INSERT INTO likes (post_id, user_id, created_at)
            VALUES (#{postId}, #{userId}, #{createdAt})
            ON CONFLICT (post_id, user_id) DO NOTHING
            """)
    void insertIfAbsent(@Param("postId") Long postId, @Param("userId") Long userId,
                         @Param("createdAt") LocalDateTime createdAt);

    @Delete("DELETE FROM likes WHERE post_id = #{postId} AND user_id = #{userId}")
    void deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM likes WHERE post_id = #{postId}")
    int countByPostId(@Param("postId") Long postId);

    @Select("SELECT EXISTS(SELECT 1 FROM likes WHERE post_id = #{postId} AND user_id = #{userId})")
    boolean existsByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT post_id AS post_id, COUNT(*) AS count
            FROM likes
            WHERE post_id IN
            <foreach item="id" collection="postIds" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY post_id
            </script>
            """)
    List<PostCountRow> countsByPostIds(@Param("postIds") List<Long> postIds);

    @Select("""
            <script>
            SELECT post_id
            FROM likes
            WHERE user_id = #{userId}
            AND post_id IN
            <foreach item="id" collection="postIds" open="(" separator="," close=")">#{id}</foreach>
            </script>
            """)
    List<Long> findLikedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
