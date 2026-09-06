package com.raisesns.backend.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FollowMapper {

    @Insert("""
            INSERT INTO follows (follower_id, followee_id, created_at)
            VALUES (#{followerId}, #{followeeId}, #{createdAt})
            ON CONFLICT (follower_id, followee_id) DO NOTHING
            """)
    void insertIfAbsent(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId,
                         @Param("createdAt") LocalDateTime createdAt);

    @Delete("DELETE FROM follows WHERE follower_id = #{followerId} AND followee_id = #{followeeId}")
    void deleteByFollowerIdAndFolloweeId(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Select("SELECT EXISTS(SELECT 1 FROM follows WHERE follower_id = #{followerId} AND followee_id = #{followeeId})")
    boolean existsByFollowerIdAndFolloweeId(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    @Select("SELECT COUNT(*) FROM follows WHERE followee_id = #{followeeId}")
    int countByFolloweeId(@Param("followeeId") Long followeeId);

    @Select("SELECT COUNT(*) FROM follows WHERE follower_id = #{followerId}")
    int countByFollowerId(@Param("followerId") Long followerId);

    @Select("""
            SELECT u.id AS id, u.username AS username, u.display_name AS display_name, u.avatar_url AS avatar_url,
                   EXISTS(SELECT 1 FROM follows f2 WHERE f2.follower_id = #{viewerId} AND f2.followee_id = u.id)
                       AS is_followed_by_me
            FROM follows f
            JOIN users u ON u.id = f.followee_id
            WHERE f.follower_id = #{followerId}
            ORDER BY f.created_at DESC
            LIMIT #{limit}
            """)
    List<FollowUserRow> findFollowing(@Param("followerId") Long followerId, @Param("viewerId") Long viewerId,
                                       @Param("limit") int limit);

    @Select("""
            SELECT u.id AS id, u.username AS username, u.display_name AS display_name, u.avatar_url AS avatar_url,
                   EXISTS(SELECT 1 FROM follows f2 WHERE f2.follower_id = #{viewerId} AND f2.followee_id = u.id)
                       AS is_followed_by_me
            FROM follows f
            JOIN users u ON u.id = f.follower_id
            WHERE f.followee_id = #{followeeId}
            ORDER BY f.created_at DESC
            LIMIT #{limit}
            """)
    List<FollowUserRow> findFollowers(@Param("followeeId") Long followeeId, @Param("viewerId") Long viewerId,
                                       @Param("limit") int limit);
}
