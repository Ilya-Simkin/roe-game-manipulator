package com.pany.bad.domain.repository;

import com.pany.bad.domain.model.Player;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface CurrentScanCollection extends MongoRepository<Player, String>  {

    Optional<Player> findByStateAndXxCoordinateAndYyCoordinate(Long State, Long xCord, Long yCord);

    boolean existsByStateAndXxCoordinateAndYyCoordinate(Long State, Long xCord, Long yCord);

    List<Player> findAll();

    void deleteAll();

    void deleteByIdIn(List<String> ids);

}
