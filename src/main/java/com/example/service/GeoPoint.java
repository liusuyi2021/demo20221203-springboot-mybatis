package com.example.service;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @ClassName GeoPoint
 * @Description:
 * @Author 刘苏义
 * @Date 2023/6/6 20:22
 * @Version 1.0
 */
@Data
@AllArgsConstructor
public class GeoPoint {
    Double longitude;
    Double latitude;
    Double altitude;
}
