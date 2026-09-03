package com.raisesns.backend.mapper;

import com.raisesns.backend.entity.RefreshToken;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Optional;

@Mapper
public interface RefreshTokenMapper {

    @Insert("""
            INSERT INTO refresh_tokens (user_id, token_hash, expires_at, created_at)
            VALUES (#{userId}, #{tokenHash}, #{expiresAt}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RefreshToken refreshToken);

    @Select("SELECT * FROM refresh_tokens WHERE token_hash = #{tokenHash} AND revoked_at IS NULL AND expires_at > NOW()")
    Optional<RefreshToken> findValidByTokenHash(String tokenHash);

    @Update("UPDATE refresh_tokens SET revoked_at = NOW() WHERE token_hash = #{tokenHash}")
    void revokeByTokenHash(String tokenHash);
}
