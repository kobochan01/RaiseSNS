package com.raisesns.backend.mapper;

import com.raisesns.backend.entity.Comment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface CommentMapper {

    @Insert("""
            INSERT INTO comments (post_id, user_id, parent_comment_id, body, created_at)
            VALUES (#{postId}, #{userId}, #{parentCommentId}, #{body}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Comment comment);

    @Select("SELECT * FROM comments WHERE id = #{id}")
    Optional<Comment> findById(Long id);

    @Delete("DELETE FROM comments WHERE id = #{id}")
    void deleteById(Long id);

    @Select("""
            SELECT c.id AS id, c.post_id AS post_id, c.user_id AS user_id,
                   c.parent_comment_id AS parent_comment_id, c.body AS body, c.created_at AS created_at,
                   u.id AS author_id, u.username AS author_username,
                   u.display_name AS author_display_name, u.avatar_url AS author_avatar_url
            FROM comments c
            JOIN users u ON u.id = c.user_id
            WHERE c.post_id = #{postId}
            ORDER BY c.id ASC
            """)
    List<CommentRow> findByPostId(@Param("postId") Long postId);

    @Select("SELECT COUNT(*) FROM comments WHERE post_id = #{postId}")
    int countByPostId(@Param("postId") Long postId);

    @Select("""
            <script>
            SELECT post_id AS post_id, COUNT(*) AS count
            FROM comments
            WHERE post_id IN
            <foreach item="id" collection="postIds" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY post_id
            </script>
            """)
    List<PostCountRow> countsByPostIds(@Param("postIds") List<Long> postIds);
}
