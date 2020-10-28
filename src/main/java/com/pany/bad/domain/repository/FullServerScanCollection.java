package com.pany.bad.domain.repository;

import com.pany.bad.domain.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FullServerScanCollection extends MongoRepository<Player, String> {


    Optional<Player> findByFullPlayerName(String name);

    Optional<Player> findByPlayerNameAndState(String playerName, Long state);

    List<Player> findByAllianceName(String allianceName);

}
