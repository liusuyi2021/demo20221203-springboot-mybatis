package com.example.service;

import com.example.mapper.ArdTubesDetailsMapper;
import com.example.model.ArdTubesDetails;
import com.example.model.ArdTubesDetailsExample;
import lombok.extern.slf4j.Slf4j;
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
        Integer alarmPointDistance = 100;
        ArdTubesDetailsExample ardTubesDetailsExample = new ArdTubesDetailsExample();
        ardTubesDetailsExample.createCriteria().andReelNumberEqualTo("10");
        List<ArdTubesDetails> ardTubesDetails = ardTubesDetailsMapper.selectByExample(ardTubesDetailsExample);
        Comparator<ArdTubesDetails> comparator = Comparator.comparingInt(person -> Integer.parseInt(person.getInflectionPointNumber())); // 使用Collections.sort方法进行排序
        Collections.sort(ardTubesDetails, comparator);
        double x = ardTubesDetails.get(0).getLongitude();
        double y = ardTubesDetails.get(0).getLatitude();
        TreeMap<Integer, Object> distanceMap = new TreeMap<>();
        TreeMap<Integer, Object> tubeMap = new TreeMap<>();
        for (ArdTubesDetails atd : ardTubesDetails) {
            double distance = GisUtil.getDistance(x, y, atd.getLongitude(), atd.getLatitude());
            distanceMap.put(Integer.parseInt(atd.getInflectionPointNumber()), distance);
            tubeMap.put(Integer.parseInt(atd.getInflectionPointNumber()), atd);
        }
        Integer num = 0;
        double distance = 0.0;
        for (int i = 0; i < distanceMap.size(); i++) {
            double currentDistance = (double) distanceMap.get(i + 1);
            if (currentDistance > alarmPointDistance) {
                num = i;
                distance = alarmPointDistance - (double) distanceMap.get(i);
                break;
            }
        }
        log.info("报警点在拐点" + num + "-" + (num + 1) + "之间,距离" + distance);
        ///124.92802845939386	46.686676183960614	142.367551661885
        ArdTubesDetails point1 = (ArdTubesDetails) tubeMap.get(num);
        double x0 = point1.getLongitude();
        double y0 = point1.getLatitude();
        ArdTubesDetails point2 = (ArdTubesDetails) tubeMap.get(num + 1);
        double x1 = point2.getLongitude();
        double y1 = point2.getLatitude();

        /*计算报警点坐标*/
        GeoPoint aPoint = new GeoPoint(x0, y0);
        GeoPoint bPoint = new GeoPoint(x1, y1);
        GeoPoint geoPoint = caculateRawGeoPoint(aPoint, bPoint, distance);
        log.info(geoPoint.toString());
    }
    public static void main(String[] args) {
        // 假设给定的三个坐标点 A、B、C
        double x1 = 124.926401943;
        double y1 = 46.686812672;
//        double x2 = 124.92802845939386;
//        double y2 = 46.686676183960614;
        double x2 = 124.92648657205987;
        double y2 = 46.68678818260882;
        double x3 = 124.926981879;
        double y3 = 46.686644854;

        // 计算斜率
        double slope1 = (y2 - y1) / (x2 - x1);
        double slope2 = (y3 - y2) / (x3 - x2);

        // 设置斜率差值的阈值
        double threshold = 0.0001;

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
        double angle = getAngle(a, b); //getAngle(a,x)==getAngle(a,b)
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

}
