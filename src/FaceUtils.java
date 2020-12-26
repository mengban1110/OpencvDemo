import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

/**
 * 1 摄像头获取照片进行对比 2 两张图片进行对比
 * 
 * @author 梦伴
 *
 */
public class FaceUtils {

	/**
	 * 图片根路径
	 */
	private static final String endImgUrl = "E:\\";

	/**
	 * opencv的人脸识别xml文件路径
	 */
	private static final String faceDetectorXML2URL = "E:\\opencv\\sources\\data\\haarcascades\\haarcascade_frontalface_alt.xml";

	/**
	 * opencv的人眼识别xml文件路径
	 */
	private static final String eyeDetectorXML2URL = "E:\\opencv\\sources\\data\\haarcascades\\haarcascade_eye.xml";

	/**
	 * 直方图大小，越大精度越高，运行越慢
	 */
	private static int Matching_Accuracy = 100000;

	/**
	 * 初始化人脸探测器
	 */
	private static CascadeClassifier faceDetector;

	/**
	 * 初始化人眼探测器
	 */
	private static CascadeClassifier eyeDetector;

	private static int i = 0;

	// 加载Dll和Xml
	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		faceDetector = new CascadeClassifier(faceDetectorXML2URL);
		eyeDetector = new CascadeClassifier(eyeDetectorXML2URL);
	}

	
	
	
	
	
	/**
	 * 1: 两张图片 进行 人脸比对
	 * 
	 * @param img_1
	 * @param img_2
	 * @return
	 */
	public static double compare_image(String img_1, String img_2) {
		Mat mat_1 = conv_Mat(img_1);
		Mat mat_2 = conv_Mat(img_2);
		Mat hist_1 = new Mat();
		Mat hist_2 = new Mat();

		// 颜色范围
		MatOfFloat ranges = new MatOfFloat(0f, 256f);
		// 直方图大小， 越大匹配越精确 (越慢)
		MatOfInt histSize = new MatOfInt(Matching_Accuracy);

		Imgproc.calcHist(Arrays.asList(mat_1), new MatOfInt(0), new Mat(), hist_1, histSize, ranges);
		Imgproc.calcHist(Arrays.asList(mat_2), new MatOfInt(0), new Mat(), hist_2, histSize, ranges);

		// CORREL 相关系数
		double res = Imgproc.compareHist(hist_1, hist_2, Imgproc.CV_COMP_CORREL);
		return res;
	}

	
	
	
	
	
	/**
	 * 2: 从摄像头获取人脸 并与本地图片进行对比 (入口)
	 * 
	 * @param targetImgUrl 比对身份证图片
	 * @return: void
	 */
	public static void getVideoFromCamera(String targetImgUrl) {
		// 1 如果要从摄像头获取视频 则要在 VideoCapture 的构造方法写 0
		VideoCapture capture = new VideoCapture(0);
		Mat video = new Mat();
		int index = 0;
		if (capture.isOpened()) {
			while (i < 10) {
				// 匹配成功3次退出
				capture.read(video);
				HighGui.imshow("实时人脸识别", getFace(video, targetImgUrl));
				// 窗口延迟等待100ms，返回退出按键
				index = HighGui.waitKey(100);
				// 当退出按键为Esc时，退出窗口
				if (index == 27) {
					break;
				}
			}
		} else {
			System.err.println("摄像头未开启");
		}
		// 该窗口销毁不生效，该方法存在问题
		HighGui.destroyAllWindows();
		capture.release();
		return;
	}

	/**
	 * 摄像头中获取人脸 并进行本地图片对比
	 * 
	 * @param image        待处理Mat图片(视频中的某一帧)
	 * @param targetImgUrl 匹配身份证照片地址
	 * @return 处理后的图片
	 */
	public static Mat getFace(Mat image, String targetImgUrl) {
		MatOfRect face = new MatOfRect();
		faceDetector.detectMultiScale(image, face);
		Rect[] rects = face.toArray();
		System.err.println("匹配到 " + rects.length + " 个人脸");
		if (rects != null && rects.length >= 1) {
			i++;
			if (i == 3) {
				// 获取匹配成功第3次的照片
				Imgcodecs.imwrite(endImgUrl + "face.png", image);
				FaceVideoThread faceVideoThread = new FaceVideoThread(targetImgUrl, endImgUrl + "face.png");
				new Thread(faceVideoThread, "人脸比对线程").start();
			}
		}
		return image;
	}

	
	
	
	
	
	/**
	 * 人脸截图
	 * 
	 * @param img
	 * @return
	 */
	public static String face2Img(String img) {
		String faceImg = null;
		Mat image0 = Imgcodecs.imread(img);
		Mat image1 = new Mat();
		// 灰度化
		Imgproc.cvtColor(image0, image1, Imgproc.COLOR_BGR2GRAY);
		// 探测人脸
		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image1, faceDetections);
		// rect中人脸图片的范围
		for (Rect rect : faceDetections.toArray()) {
			faceImg = img + "_.jpg";
			// 进行图片裁剪
			imageCut(img, faceImg, rect.x, rect.y, rect.width, rect.height);
		}
		if (null == faceImg) {
			System.err.println("face2Img未识别出该图像中的人脸，img=" + img);
		}
		return faceImg;
	}

	
	
	
	

	
	
	
	
	
	/**
	 * 裁剪人脸
	 * 
	 * @param imagePath
	 * @param outFile
	 * @param posX
	 * @param posY
	 * @param width
	 * @param height
	 */
	public static void imageCut(String imagePath, String outFile, int posX, int posY, int width, int height) {
		// 原始图像
		Mat image = Imgcodecs.imread(imagePath);
		// 截取的区域：参数,坐标X,坐标Y,截图宽度,截图长度
		Rect rect = new Rect(posX, posY, width, height);
		// 两句效果一样
		// Mat sub = new Mat(image,rect);
		Mat sub = image.submat(rect);
		Mat mat = new Mat();
		Size size = new Size(width, height);
		// 将人脸进行截图并保存
		Imgproc.resize(sub, mat, size);
		Imgcodecs.imwrite(outFile, mat);
		System.err.println(String.format("图片裁切成功，裁切后图片文件为：" + outFile));
	}

	/**
	 * 灰度化人脸 (生成图片对象需要先将图片灰度化)
	 * 
	 * @param img
	 * @return
	 */
	public static Mat conv_Mat(String img) {
		if (StringUtils.isBlank(img)) {
			return null;
		}
		Mat image0 = Imgcodecs.imread(img);
		Mat image1 = new Mat();
		Mat image2 = new Mat();
		// 灰度化
		Imgproc.cvtColor(image0, image1, Imgproc.COLOR_BGR2GRAY);
		// 直方均匀
		Imgproc.equalizeHist(image1, image2);

		// 探测人脸
		MatOfRect faceDetections = new MatOfRect();
		faceDetector.detectMultiScale(image1, faceDetections);

		// 探测人眼
		MatOfRect eyeDetections = new MatOfRect();
		eyeDetector.detectMultiScale(image1, eyeDetections);

		// rect中人脸图片的范围
		Mat face = null;
		for (Rect rect : faceDetections.toArray()) {

//			 给图片上画框框 参数1是图片 参数2是矩形 参数3是颜色 参数四是画出来的线条大小
//			Imgproc.rectangle(image0, rect, new Scalar(0, 0, 255) , 2);
//			 输出图片
//			 Imgcodecs.imwrite(img+"_.jpg",image0);

			face = new Mat(image1, rect);
		}
		if (null == face) {
			System.err.println("conv_Mat未识别出该图像中的人脸，img=" + img);
		}
		return face;
	}

	
	
	
	

	
	
	
	
	
	public static void main(String[] args) {
		System.err.println("开始人脸匹配");
		long begin = System.currentTimeMillis();
		try {

//			//1: 从摄像头实时人脸识别，识别成功保存图片到本地
			getVideoFromCamera(endImgUrl + "Demo01.jpg");
			// 仅用于强制抛异常，从而关闭GUI界面
			Thread.sleep(1000);
			int err = 1 / 0;

			
			
			//2: 比对本地2张图的人脸相似度 （越接近1越相似）
//			double compareHist = FaceUtils.compare_image(endImgUrl + "Demo01.jpg", endImgUrl + "face.png");
//			System.err.println("匹配度： " + compareHist);
//			if (compareHist > 0.72) {
//				System.err.println("人脸匹配 ");
//			} else {
//				System.err.println("人脸不匹配 ");
//			}

			
			
			//3:人脸裁剪
//			face2Img(endImgUrl + "test.jpg");

		} catch (Exception e) {
			System.err.println("开始强制关闭");
			System.err.println("人脸匹配结束，总耗时 : " + (System.currentTimeMillis() - begin) + " ms");
			System.exit(0);
		}
	}

}