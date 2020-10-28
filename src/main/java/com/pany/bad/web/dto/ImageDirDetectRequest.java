package com.pany.bad.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class ImageDirDetectRequest {

    private String ImageDir ;
    private String templatePath;
    private String resultPath;
}
