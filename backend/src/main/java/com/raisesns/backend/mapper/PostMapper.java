package com.raisesns.backend.mapper;

import com.raisesns.backend.entity.Post;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface PostMapper {

    @Insert("""
            INSERT INTO posts (user_id, body, created_at, updated_at)
            VALUES (#{userId}, #{body}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Post post);

    @Select("SELECT * FROM posts WHERE id = #{id}")
    Optional<Post> findById(Long id);

    @Update("UPDATE posts SET body = #{body}, updated_at = #{updatedAt} WHERE id = #{id}")
    void update(Post post);

    @Delete("DELETE FROM posts WHERE id = #{id}")
    void deleteById(Long id);

    @Select("""
            <script>
            SELECT p.id AS id, p.user_id AS user_id, p.body AS body, p.image_url AS image_url,
                   p.created_at AS created_at, p.updated_at AS updated_at,
                   u.id AS author_id, u.username AS author_username,
                   u.display_name AS author_display_name, u.avatar_url AS author_avatar_url
            FROM posts p
            JOIN users u ON u.id = p.user_id
            <if test="cursor != null">WHERE p.id &lt; #{cursor}</if>
            ORDER BY p.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<PostFeedRow> findFeed(@Param("cursor") Long cursor, @Param("limit") int limit);

    @Select("""
            SELECT p.id AS id, p.user_id AS user_id, p.body AS body, p.image_url AS image_url,
                   p.created_at AS created_at, p.updated_at AS updated_at,
                   u.id AS author_id, u.username AS author_username,
                   u.display_name AS author_display_name, u.avatar_url AS author_avatar_url
            FROM posts p
            JOIN users u ON u.id = p.user_id
            WHERE p.id > #{sinceId}
            ORDER BY p.id DESC
            LIMIT #{limit}
            """)
    List<PostFeedRow> findNewerThan(@Param("sinceId") Long sinceId, @Param("limit") int limit);
}
