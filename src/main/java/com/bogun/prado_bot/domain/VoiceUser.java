package com.bogun.prado_bot.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "voice_user")
public class VoiceUser {

    @EmbeddedId
    private VoiceUserId id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "member_name", nullable = false, length = 100)
    private String memberName;

    @Column(name = "points", nullable = false)
    private long points;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Embeddable
    public static class VoiceUserId implements Serializable {
        @Column(name = "guild_id", nullable = false)
        private Long guildId;

        @Column(name = "user_id", nullable = false)
        private Long userId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            VoiceUserId that = (VoiceUserId) o;
            if (guildId != null ? !guildId.equals(that.guildId) : that.guildId != null) {
                return false;
            }
            return userId != null ? userId.equals(that.userId) : that.userId == null;
        }

        @Override
        public int hashCode() {
            int result = guildId != null ? guildId.hashCode() : 0;
            result = 31 * result + (userId != null ? userId.hashCode() : 0);
            return result;
        }
    }
}
