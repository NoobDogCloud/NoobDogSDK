package common.java.CheckCode;

import common.java.Encrypt.Base64;
import common.java.Http.Server.HttpContext;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;


public class ImageCheckCode {
    private static final String[] fontNames = {"宋体", "华文楷体", "黑体", "微软雅黑", "楷体_GB2312"};
    private static final Random r = new Random();

    public static byte[] getCodeimage(String code) {
        Random random = new Random();
        int size = 5;
        // 验证码图片的生成
        // 定义图片的宽度和高度
        int width = (int) Math.ceil(size * 20);
        int height = 50;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        // 获取图片的上下文
        Graphics gr = image.getGraphics();
        // 设定图片背景颜色
        gr.setColor(Color.WHITE);
        gr.fillRect(0, 0, width, height);
        // 设定图片边框
        gr.setColor(Color.GRAY);
        gr.drawRect(0, 0, width - 1, height - 1);
        // 画十条干扰线
        for (int i = 0; i < 5; i++) {
            int x1 = random.nextInt(width);
            int y1 = random.nextInt(height);
            int x2 = random.nextInt(width);
            int y2 = random.nextInt(height);
            gr.setColor(randomColor());
            gr.drawLine(x1, y1, x2, y2);
        }
        // 设置字体，画验证码
        gr.setColor(randomColor());
        gr.setFont(randomFont());
        gr.drawString(code, 10, 22);
        // 图像生效
        gr.dispose();

        byte[] rsByte;
        ByteArrayOutputStream imageOut = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "JPEG", imageOut);
            rsByte = imageOut.toByteArray();
            HttpContext.current().setMime("image/jpeg");
        } catch (IOException e) {
            nLogger.logInfo(e);
            rsByte = null;
        }
        return rsByte;
    }

    // 生成随机的颜色
    private static Color randomColor() {
        int red = r.nextInt(150);
        int green = r.nextInt(150);
        int blue = r.nextInt(150);
        return new Color(red, green, blue);
    }

    // 生成随机的字体
    private static Font randomFont() {
        int index = r.nextInt(fontNames.length);
        String fontName = fontNames[index];// 生成随机的字体名称
        int style = r.nextInt(4);
        int size = r.nextInt(3) + 24; // 生成随机字号, 24 ~ 28
        return new Font(fontName, style, size);
    }

    /**
     * 创建水印
     *
     * @param data
     * @return
     */
    public static String CreateTextWaterMark(List<String> data) {
        return CreateTextWaterMark(data, randomFont());
    }

    public static String CreateTextWaterMark(List<String> data, Font font) {
        int width = 200;
        int height = 200;
        //Font font = randomFont();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        String currentTime = TimeHelper.build().nowDatetime();
        try {
            BufferedImage bi = new BufferedImage(width, height, 6);
            Graphics2D g2 = bi.createGraphics();
            g2.rotate(Math.toRadians(-45.0D), width / 2, height / 2);
            g2.setFont(font);
            g2.setColor(new Color(230, 230, 230));

            int l = data.size();
            int i;
            for (i = 0; i < l; i++) {
                g2.drawString(data.get(i), 30, 50 + i * 30);
            }
            g2.drawString(currentTime, 30, 70 + (i + 1) * 30);
            g2.dispose();

            ImageIO.write(bi, "PNG", buffer);
        } catch (Exception e) {
            nLogger.logInfo(e);
        }
        return "data:image/jpeg;base64," + Base64.decode(buffer.toByteArray());
    }
}