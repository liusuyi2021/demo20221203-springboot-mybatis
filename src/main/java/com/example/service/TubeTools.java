package com.example.service;

import com.example.model.ArdTubesDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * @ClassName TubeTools
 * @Description: 管线计算工具
 * @Author 刘苏义
 * @Date 2023/6/6 19:29
 * @Version 1.0
 */
@Service
@Slf4j
public class TubeTools {
    public static void main(String[] args) {
        // 假设给定的三个坐标点 A、B、C
        double x1 = 124.939903268;
        double y1 = 46.684520056;
        double x2 = 124.94049634327537;
        double y2 = 46.68442539350505;
        double x3 = 124.940552075;
        double y3 = 46.684416498;

        double distance = getDistance(x1, y1, x3, y3);
        log.info("总距离:" + distance);
        double distance1 = getDistance(x1, y1, x2, y2);
        log.info("距离起点距离:" + distance1);
        double distance2 = getDistance(x2, y2, x3, y3);
        log.info("距离终点距离:" + distance2);
        // 计算斜率
        double slope1 = (y2 - y1) / (x2 - x1);
        double slope2 = (y3 - y2) / (x3 - x2);

        // 设置斜率差值的阈值
        double threshold = 0.000001;

        // 检查斜率是否相等
        if (Math.abs(slope1 - slope2) < threshold) {
            System.out.println("这三个点共线");
        } else {
            System.out.println("这三个点不共线");
        }
    }

    /**
     * @描述 计算坐标
     * @参数 [ardTubesDetails, alarmPointDistance]
     * @返回值 void
     * @创建人 刘苏义
     * @创建时间 2023/6/8 14:38
     * @修改人和其它信息
     */
    public static void CalculateCoordinates(List<ArdTubesDetails> ardTubesDetails, Integer alarmPointDistance) {

        Comparator<ArdTubesDetails> comparator = Comparator.comparingInt(person -> Integer.parseInt(person.getInflectionPointNumber())); // 使用Collections.sort方法进行排序
        Collections.sort(ardTubesDetails, comparator);
        double x = ardTubesDetails.get(0).getLongitude();
        double y = ardTubesDetails.get(0).getLatitude();
        TreeMap<Integer, Double> distanceMap = new TreeMap<>();
        TreeMap<Integer, Object> tubeMap = new TreeMap<>();
        double distance = 0.0;
        for (ArdTubesDetails atd : ardTubesDetails) {
            distance += getDistance(x, y, atd.getLongitude(), atd.getLatitude());
            distanceMap.put(Integer.parseInt(atd.getInflectionPointNumber()), distance);
            tubeMap.put(Integer.parseInt(atd.getInflectionPointNumber()), atd);
            x = atd.getLongitude();
            y = atd.getLatitude();
        }
        Integer num = 0;
        double tempDistance = 0.0;
        while (tempDistance < alarmPointDistance) {
            num++;
            tempDistance = distanceMap.get(num);
        }
        log.info("报警点在拐点" + (num - 1) + "-" + num + "之间,总距离" + (tempDistance - distanceMap.get(num - 1)));
        ArdTubesDetails point1 = (ArdTubesDetails) tubeMap.get(num - 1);
        double x0 = point1.getLongitude();
        double y0 = point1.getLatitude();
        double z0 = point1.getAltitude();
        ArdTubesDetails point2 = (ArdTubesDetails) tubeMap.get(num);
        double x1 = point2.getLongitude();
        double y1 = point2.getLatitude();
        double z1 = point2.getAltitude();
        /*计算报警点坐标*/
        double d = alarmPointDistance - distanceMap.get(num - 1);
        GeoPoint aPoint = new GeoPoint(x0, y0, z0);
        GeoPoint bPoint = new GeoPoint(x1, y1, z1);
        GeoPoint geoPoint = caculateRawGeoPoint(aPoint, bPoint, d);
        double height = interpolateHeight(aPoint, bPoint, geoPoint);
        geoPoint.setAltitude(height);
        log.info("计算结果:" + geoPoint);
    }

    // 线性插值计算任意点的高度
    private static double interpolateHeight(GeoPoint startPoint, GeoPoint endPoint, GeoPoint alarmPoint) {
        double startX = startPoint.getLongitude();
        double startY = startPoint.getLatitude();
        double startZ = startPoint.getAltitude();
        double endX = endPoint.getLongitude();
        double endY = endPoint.getLatitude();
        double endZ = endPoint.getAltitude();
        // 目标点的坐标
        double targetX = alarmPoint.getLongitude();
        double targetY = alarmPoint.getLatitude();
        double distance = Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        double targetDistance = Math.sqrt(Math.pow(targetX - startX, 2) + Math.pow(targetY - startY, 2));
        double t = targetDistance / distance;
        double targetHeight = startZ + t * (endZ - startZ);
        return targetHeight;
    }

    /**
     * 已知WGS84坐标系 A 点，B点, X 在AB 弧线上, 且是最短的这条, AX距离已知，求X点坐标.
     *
     * @param aPoint
     * @param bPoint
     * @param distance_ax_in_meter
     * @return
     */
    public static GeoPoint caculateRawGeoPoint(GeoPoint aPoint, GeoPoint bPoint, double distance_ax_in_meter) {
        MyLatLng a = new MyLatLng(aPoint.getLongitude(), aPoint.getLatitude());
        MyLatLng b = new MyLatLng(bPoint.getLongitude(), bPoint.getLatitude());
        double angle = getAngle(a, b);
        //double angle = GisUtil.getNorthAngle(a.getM_Longitude(),a.getM_Latitude(), b.getM_Longitude(),b.getM_Latitude());
        MyLatLng x = getMyLatLng(a, distance_ax_in_meter / 1000.0, angle);
        GeoPoint xPoint = new GeoPoint(x.m_Longitude, x.m_Latitude, 0.0);
        return xPoint;
    }

    /**
     * 求B点经纬度
     *
     * @param A            已知点的经纬度，
     * @param distanceInKM AB两地的距离  单位km
     * @param angle        AB连线与正北方向的夹角（0~360）
     * @return B点的经纬度
     */
    public static MyLatLng getMyLatLng(MyLatLng A, double distanceInKM, double angle) {

        double dx = distanceInKM * 1000 * Math.sin(Math.toRadians(angle));
        double dy = distanceInKM * 1000 * Math.cos(Math.toRadians(angle));

        double bjd = (dx / A.Ed + A.m_RadLo) * 180. / Math.PI;
        double bwd = (dy / A.Ec + A.m_RadLa) * 180. / Math.PI;
        return new MyLatLng(bjd, bwd);
    }

    /**
     * 获取AB连线与正北方向的角度
     *
     * @param A A点的经纬度
     * @param B B点的经纬度
     * @return AB连线与正北方向的角度（0~360）
     */
    public static double getAngle(MyLatLng A, MyLatLng B) {
        double dx = (B.m_RadLo - A.m_RadLo) * A.Ed;
        double dy = (B.m_RadLa - A.m_RadLa) * A.Ec;
        double angle = 0.0;
        angle = Math.atan(Math.abs(dx / dy)) * 180. / Math.PI;
        double dLo = B.m_Longitude - A.m_Longitude;
        double dLa = B.m_Latitude - A.m_Latitude;
        if (dLo > 0 && dLa <= 0) {
            angle = (90. - angle) + 90;
        } else if (dLo <= 0 && dLa < 0) {
            angle = angle + 180.;
        } else if (dLo < 0 && dLa >= 0) {
            angle = (90. - angle) + 270;
        }
        return angle;
    }

    private static final double EARTH_RADIUS = 6378.137; // 6378.137为地球半径(单位:千米)

    /**
     * 根据经纬度，计算两点间的距离
     *
     * @param longitude1 第一个点的经度
     * @param latitude1  第一个点的纬度
     * @param longitude2 第二个点的经度
     * @param latitude2  第二个点的纬度
     * @return 返回距离 单位千米
     */
    public static double getDistance(double longitude1, double latitude1, double longitude2, double latitude2) {
        // 纬度
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        // 经度
        double lng1 = Math.toRadians(longitude1);
        double lng2 = Math.toRadians(longitude2);
        // 纬度之差
        double a = lat1 - lat2;
        // 经度之差
        double b = lng1 - lng2;
        // 计算两点距离的公式
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(b / 2), 2)));
        // 弧长乘地球半径, 返回单位: 千米
        s = s * EARTH_RADIUS;
        //System.out.println("距离"+s);
        return s * 1000;
    }

}
