package com.pany.bad.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OneImageDetectRequest {

   private String imagePath ;
   private String templatePath;
   private String templateMaskPath;
   private String resultPath;
   private Double trash1;
   private Double trash2;
   private Integer templateWidth;
   private Integer templateHeight;
}
