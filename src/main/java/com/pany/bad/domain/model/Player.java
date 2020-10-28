package com.pany.bad.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "current_scan_collection")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Player {

    @Id
    private String id;

    @Field(name = "full_player_name")
    @Indexed(unique = true)
    private String fullPlayerName;

    @Field(name = "player_name")
    @Indexed(unique = true)
    private String playerName;

    @Field(name = "alliance_name")
    private String allianceName;

    @Field(name = "player_level")
    private Long playerLevel;

    @Field(name = "date_of_detection")
    private LocalDateTime dateOfDetection;

    @Field(name = "date_of_recognition")
    private LocalDateTime dateOfRecognition;

    @Field(name = "state")
    private Long state;

    @Field(name = "x_coordinate")
    private Long xxCoordinate;

    @Field(name = "y_coordinate")
    private Long yyCoordinate;

}
