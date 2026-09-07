package com.raisesns.backend.mapper;

import com.raisesns.backend.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface UserMapper {

    @Insert("""
            INSERT INTO users (username, email, password_hash, display_name, created_at, updated_at)
            VALUES (#{username}, #{email}, #{passwordHash}, #{displayName}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username}")
    boolean existsByUsername(String username);

    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email}")
    boolean existsByEmail(String email);

    @Select("SELECT * FROM users WHERE email = #{email}")
    Optional<User> findByEmail(String email);

    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);

    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(String username);

    @Update("""
            UPDATE users SET display_name = #{displayName}, bio = #{bio}, avatar_url = #{avatarUrl}, updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    void updateProfile(@Param("id") Long id, @Param("displayName") String displayName,
                        @Param("bio") String bio, @Param("avatarUrl") String avatarUrl,
                        @Param("updatedAt") LocalDateTime updatedAt);
}
