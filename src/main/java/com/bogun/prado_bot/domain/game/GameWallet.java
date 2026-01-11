package com.bogun.prado_bot.domain.game;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "game_wallet")
public class GameWallet {

    @EmbeddedId
    private GameWalletId id;

    @Column(nullable = false)
    private long balance;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public GameWalletId getId() {
        return id;
    }

    public void setId(GameWalletId id) {
        this.id = id;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Embeddable
    public static class GameWalletId implements Serializable {
        @Column(name = "guild_id", nullable = false)
        private Long guildId;

        @Column(name = "user_id", nullable = false)
        private Long userId;

        public GameWalletId() {}

        public GameWalletId(Long guildId, Long userId) {
            this.guildId = guildId;
            this.userId = userId;
        }

        public Long getGuildId() {
            return guildId;
        }

        public void setGuildId(Long guildId) {
            this.guildId = guildId;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            GameWalletId that = (GameWalletId) o;
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
