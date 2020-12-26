public class FaceVideoThread implements Runnable {

	private String oneImgUrl = null;
	private String otherImgUrl = null;

	public FaceVideoThread(String oneImgUrl, String otherImgUrl) {
		this.oneImgUrl = oneImgUrl;
		this.otherImgUrl = otherImgUrl;
	}

	@Override
	public void run() {
		try {
			double compareHist = FaceUtils.compare_image(oneImgUrl, otherImgUrl);
			System.err.println("匹配度：" + compareHist);
			if (compareHist > 0.72) {
				System.err.println("人脸匹配");
			} else {
				System.err.println("人脸不匹配");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}