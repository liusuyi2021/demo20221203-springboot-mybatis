package com.example.service;

import com.example.mapper.ArdTubesDetailsMapper;
import com.example.model.ArdTubesDetails;
import com.example.model.ArdTubesDetailsExample;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
        double x1 = 124.925490653;
        double y1 = 46.687071291;
        double x2 = 124.9263717908152;
        double y2 = 46.686821229021255;
        double x3 = 124.926401943;
        double y3 = 46.686812672;

//        double distance = getDistance(x1, y1, x3, y3);
//        log.info("总距离:" + distance);
//        double distance1 = getDistance(x1, y1, x2, y2);
//        log.info("距离起点距离:" + distance1);
//        double distance2 = getDistance(x2, y2, x3, y3);
//        log.info("距离终点距离:" + distance2);
        // 计算斜率
        double slope1 = (y2 - y1) / (x2 - x1);
        double slope2 = (y3 - y2) / (x3 - x2);

        // 设置斜率差值的阈值
        double threshold = 0.000001;

        // 检查斜率是否相等
        if (Math.abs(slope1 - slope2) < threshold) {
            System.out.println("这三个点共线");
            System.out.println(Math.abs(slope1 - slope2));
        } else {
            System.out.println(Math.abs(slope1 - slope2));
            System.out.println("这三个点不共线");
        }
    }

    @Resource
    ArdTubesDetailsMapper ardTubesDetailsMapper;

    @PostConstruct
    void test() {
        ArdTubesDetailsExample ardTubesDetailsExample = new ArdTubesDetailsExample();
        ardTubesDetailsExample.createCriteria().andReelNumberEqualTo("10");
        List<ArdTubesDetails> ardTubesDetails = ardTubesDetailsMapper.selectByExample(ardTubesDetailsExample);
        GeoPoint geoPoint = CalculateCoordinates(ardTubesDetails, 100);
        log.info("结果：" + geoPoint);
    }

    public static void main1(String[] args) {
        // 已知起点和终点的坐标和高层
        double x1 = 124.925490653;
        double y1 = 46.687071291;
        double z1 = 148.2;
        double x2 = 124.926401943;
        double y2 = 46.686812672;
        double z2 = 143.5;

        // 创建线性插值器
        LinearInterpolator interpolator = new LinearInterpolator();

        // 根据已知点创建插值函数
        PolynomialSplineFunction function = interpolator.interpolate(new double[]{x1, x2}, new double[]{z1, z2});

        // 指定任意点的横坐标
        double x = 124.92637179084832;

        // 计算任意点的高层
        double z = function.value(x);

        System.out.println("任意点的高层：" + z);
    }


    /**
     * @描述 计算坐标
     * @参数 [ardTubesDetails, alarmPointDistance]
     * @返回值 void
     * @创建人 刘苏义
     * @创建时间 2023/6/8 14:38
     * @修改人和其它信息
     */
    public static GeoPoint CalculateCoordinates(List<ArdTubesDetails> ardTubesDetails, Integer alarmPointDistance) {

        Comparator<ArdTubesDetails> comparator = Comparator.comparingInt(person -> Integer.parseInt(person.getInflectionPointNumber())); // 使用Collections.sort方法进行排序
        Collections.sort(ardTubesDetails, comparator);
        GeoPoint point0 = new GeoPoint(ardTubesDetails.get(0).getLongitude(), ardTubesDetails.get(0).getLatitude(), ardTubesDetails.get(0).getAltitude());
        TreeMap<Integer, Double> distanceMap = new TreeMap<>();
        TreeMap<Integer, Object> tubeMap = new TreeMap<>();
        double distance = 0.0;
        for (ArdTubesDetails atd : ardTubesDetails) {
            GeoPoint point = new GeoPoint(atd.getLongitude(), atd.getLatitude(), atd.getAltitude());
            distance += getDistance(point, point0);
            distanceMap.put(Integer.parseInt(atd.getInflectionPointNumber()), distance);
            tubeMap.put(Integer.parseInt(atd.getInflectionPointNumber()), atd);
            point0=point;
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
        double z0 = point1.getAltitude() - point1.getDepth();
        ArdTubesDetails point2 = (ArdTubesDetails) tubeMap.get(num);
        double x1 = point2.getLongitude();
        double y1 = point2.getLatitude();
        double z1 = point2.getAltitude() - point2.getDepth();
        /*计算报警点坐标*/
        double d = alarmPointDistance - distanceMap.get(num - 1);
        GeoPoint aPoint = new GeoPoint(x0, y0, z0);
        GeoPoint bPoint = new GeoPoint(x1, y1, z1);
        GeoPoint geoPoint = caculateRawGeoPoint(aPoint, bPoint, d);
        double height = interpolateHeight(aPoint, bPoint, geoPoint);
        geoPoint.setAltitude(height);
        log.info("计算结果:" + geoPoint);
        return geoPoint;
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
     * @param pa 第一个点的经纬度
     * @param pb 第二个点的经纬度
     * @return 返回距离 单位千米
     */
    public static double getDistance(GeoPoint pa, GeoPoint pb) {
        // 纬度
        double y1 = Math.toRadians(pa.getLatitude());
        double y2 = Math.toRadians(pb.getLatitude());
        // 经度
        double x1 = Math.toRadians(pa.getLongitude());
        double x2 = Math.toRadians(pb.getLongitude());
        // 高层
        double z1 = Math.toRadians(pa.getAltitude());
        double z2 = Math.toRadians(pb.getAltitude());
        // 纬度之差
        double a = y1 - y2;
        // 经度之差
        double b = x1 - x2;
        // 计算两点距离的公式
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) +
                Math.cos(y1) * Math.cos(y2) * Math.pow(Math.sin(b / 2), 2)));
        // 弧长乘地球半径, 返回单位: 千米
        s = s * EARTH_RADIUS;

        double res = Math.sqrt(Math.pow(z1 - z2, 2) + Math.pow(s * 1000, 2));
        //System.out.println("距离"+s);
        return res;
    }
}
