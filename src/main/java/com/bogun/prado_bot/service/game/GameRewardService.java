package com.bogun.prado_bot.service.game;

import com.bogun.prado_bot.domain.game.GameWallet;
import com.bogun.prado_bot.domain.game.GameWallet.GameWalletId;
import com.bogun.prado_bot.repo.GameWalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class GameRewardService {

    private final GameWalletRepository walletRepository;

    public GameRewardService(GameWalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional
    public void grantCoins(long guildId, long userId, int coins) {
        GameWalletId id = new GameWalletId(guildId, userId);
        GameWallet wallet = walletRepository.findById(id).orElseGet(() -> {
            GameWallet created = new GameWallet();
            created.setId(id);
            created.setBalance(0);
            created.setCreatedAt(Instant.now());
            created.setUpdatedAt(Instant.now());
            return created;
        });
        wallet.setBalance(wallet.getBalance() + coins);
        wallet.setUpdatedAt(Instant.now());
        walletRepository.save(wallet);
    }
}
