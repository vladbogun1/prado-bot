package com.bogun.prado_bot.repo;

import com.bogun.prado_bot.domain.game.GameWallet;
import com.bogun.prado_bot.domain.game.GameWallet.GameWalletId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameWalletRepository extends JpaRepository<GameWallet, GameWalletId> {}
