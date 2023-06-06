package com.example.service;

import org.gavaghan.geodesy.*;

import java.math.BigDecimal;
import java.util.Arrays;

public class GisUtil {

    public static GeodeticCalculator geodeticCalculator = new GeodeticCalculator();

    /**
     * 根据经纬度，计算两点间的距离
     *
     * @param longitudeFrom 第一个点的经度
     * @param latitudeFrom  第一个点的纬度
     * @param longitudeTo   第二个点的经度
     * @param latitudeTo    第二个点的纬度
     * @return 返回距离 单位米
     */
    public static double getDistance(double longitudeFrom, double latitudeFrom, double longitudeTo, double latitudeTo) {
        GlobalCoordinates source = new GlobalCoordinates(latitudeFrom, longitudeFrom);
        GlobalCoordinates target = new GlobalCoordinates(latitudeTo, longitudeTo);
        return geodeticCalculator.calculateGeodeticCurve(Ellipsoid.WGS84, source, target).getEllipsoidalDistance();
    }

    /**
     * 计算从from到to方向的直线与正北方向夹角
     *
     * @param longitudeFrom 第一个点的经度
     * @param latitudeFrom  第一个点的纬度
     * @param longitudeTo   第二个点的经度
     * @param latitudeTo    第二个点的纬度
     * @return 返回角度
     */
    public static double getNorthAngle(double longitudeFrom, double latitudeFrom, double longitudeTo, double latitudeTo) {
        GlobalPosition source = new GlobalPosition(latitudeFrom, longitudeFrom, 0);
        GlobalPosition target = new GlobalPosition(latitudeTo, longitudeTo, 0);
        return geodeticCalculator.calculateGeodeticMeasurement(Ellipsoid.WGS84, source, target).getAzimuth();
    }

    /**
     * @param camera    经度,纬度,高度 如:{125.097531, 46.60029, 120};
     * @param lookAt    经度,纬度,高度 如:{125.124731, 46.584808, 0};
     * @param viewAngle 相机可视角度 如:20
     * @param viewWidth 视域宽度 如:150
     * @return ptz 数组 如:[129.5355798969157, -2.5419097807416655, 23.3676043024458]
     */
    public static double[] getCameraPTZ(double[] camera, double[] lookAt, double viewAngle, double viewWidth) {
        double p = 0, t = 0, z = 0;
        double distance = GisUtil.getDistance(camera[0], camera[1], lookAt[0], lookAt[1]);
        double northAngle = GisUtil.getNorthAngle(camera[0], camera[1], lookAt[0], lookAt[1]);
        double height = camera[2];
        p = northAngle;
        t = Angle.toDegrees(Math.atan(height / distance)) * -1+360;
        z = distance * Math.tan(viewAngle / 2) * 2 / viewWidth;
      /*  p = new BigDecimal(p).setScale(1,BigDecimal.ROUND_HALF_UP).doubleValue();
        t = new BigDecimal(t).setScale(1,BigDecimal.ROUND_HALF_UP).doubleValue();
        z = new BigDecimal(z).setScale(1,BigDecimal.ROUND_HALF_UP).doubleValue();*/
        return new double[]{p, t, z};
    }

    public static void main(String[] args) {
        // 125.097531,46.60029, 125.124731,46.584808
        //相机位置
        double[] camera = {125.146964331147,46.5580925811216,102};//经度,纬度,高度
        //看向的位置
        double[] lookAt = {125.155449,46.555108,0};//经度,纬度,高度

        double viewAngle = 20;//相机可视角度
        double viewWidth = 150;//相机视域宽度

        double[] ptz = GisUtil.getCameraPTZ(camera, lookAt, viewAngle, viewWidth);
        System.out.println("ptz:" + Arrays.toString(ptz));

    }
}
