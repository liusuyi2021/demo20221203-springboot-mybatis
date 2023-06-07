package com.example.service;

import com.example.mapper.ArdTubesDetailsMapper;
import com.example.model.ArdTubesDetails;
import com.example.model.ArdTubesDetailsExample;
import lombok.extern.slf4j.Slf4j;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GlobalCoordinates;
import org.gavaghan.geodesy.GlobalPosition;
import org.springframework.stereotype.Service;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

/**
 * @ClassName TestService
 * @Description:
 * @Author 刘苏义
 * @Date 2023/6/6 19:29
 * @Version 1.0
 */
@Service
@Slf4j
public class TestService {
    @Resource
    ArdTubesDetailsMapper ardTubesDetailsMapper;

    @PostConstruct
    void getList() {
        Integer alarmPointDistance = 10;
        ArdTubesDetailsExample ardTubesDetailsExample = new ArdTubesDetailsExample();
        ardTubesDetailsExample.createCriteria().andReelNumberEqualTo("10");
        List<ArdTubesDetails> ardTubesDetails = ardTubesDetailsMapper.selectByExample(ardTubesDetailsExample);
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
        double tempDistance=0.0;
        while(tempDistance<alarmPointDistance)
        {
            num++;
            tempDistance = distanceMap.get(num);
        }

        log.info("报警点在拐点" + (num-1) + "-" + num + "之间,距离差" + (tempDistance-alarmPointDistance));
        ///124.92802845939386	46.686676183960614	142.367551661885
        ArdTubesDetails point1 = (ArdTubesDetails) tubeMap.get(num-1);
        double x0 = point1.getLongitude();
        double y0 = point1.getLatitude();
        ArdTubesDetails point2 = (ArdTubesDetails) tubeMap.get(num);
        double x1 = point2.getLongitude();
        double y1 = point2.getLatitude();

        /*计算报警点坐标*/
        double d = alarmPointDistance - distanceMap.get(num - 1);
        GeoPoint aPoint = new GeoPoint(x0, y0);
        GeoPoint bPoint = new GeoPoint(x1, y1);
        GeoPoint geoPoint = caculateRawGeoPoint(aPoint, bPoint, d);
        log.info(geoPoint.toString());
    }


    public static void main1(String[] args) {
        // 已知起点的坐标
        double x1 = 124.925490653;
        double y1 = 46.687071291;
        double z1 = 148.2;

        // 已知终点的坐标
        double x2 = 124.926401943;
        double y2 = 46.686812672;
        double z2 = 143.5;

        // 已知任意点到起点的距离
        double d = 100.0;

        double vx = x2 - x1;
        double vy = y2 - y1;
        double vz = z2 - z1;
        double vLength = Math.sqrt(vx*vx + vy*vy + vz*vz);
        double tx = vx * d / vLength;
        double ty = vy * d / vLength;
        double tz = vz * d / vLength;
        double x = x1 + tx;
        double y = y1 + ty;
        double z = z1 + tz;
        // 输出结果
        System.out.println("任意点的坐标：(" + x + ", " + y + ", " + z + ")");
    }

    public static void main(String[] args) {
        // 假设给定的三个坐标点 A、B、C
        double x1 = 124.925198285;
        double y1 = 46.68693001;
        double x2 = 124.92530552899079;
        double y2 = 46.68698183351785;
        double x3 = 124.925490653;
        double y3 = 46.687071291;

        double distance1 = getDistance(x1, y1, x2, y2);
        double distance2 = getDistance(x2, y2, x3, y3);
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
        GeoPoint xPoint = new GeoPoint(x.m_Longitude, x.m_Latitude);
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

    // Java 计算两个GPS坐标点之间的距离
    // lat1、lng1 表示A点经纬度，lat2、lng2 表示B点经纬度，计算出来的结果单位为千米
    public static double getDistance1(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;      // a 是两坐标点的纬度之差
        double b = rad(lng1) - rad(lng2);  // b 是两坐标点的经度之差

        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2) +
                Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
        s = s * EARTH_RADIUS;
        System.out.println("s = " + s + "千米"); // 单位:千米

        s = Math.round(s * 1000); // 转为米，用 Math.round() 取整
//        s = s/1000; // 米转千米
        return s;
    }

    private static double rad(double d) {  return d * Math.PI / 180.0; }


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
        s =  s * EARTH_RADIUS;
        //System.out.println("距离"+s);
        return s*1000;
    }

}
